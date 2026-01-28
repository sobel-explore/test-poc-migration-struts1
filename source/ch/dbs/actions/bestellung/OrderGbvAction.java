//  Copyright (C) 2005 - 2010  Markus Fischer, Pascal Steiner
//
//  This program is free software; you can redistribute it and/or
//  modify it under the terms of the GNU General Public License
//  as published by the Free Software Foundation; version 2 of the License.
//
//  This program is distributed in the hope that it will be useful,
//  but WITHOUT ANY WARRANTY; without even the implied warranty of
//  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//  GNU General Public License for more details.
//
//  You should have received a copy of the GNU General Public License
//  along with this program; if not, write to the Free Software
//  Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
//
//  Contact: info@doctor-doc.com

package ch.dbs.actions.bestellung;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.actions.DispatchAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import util.Auth;
import util.Check;
import util.CodeUrl;
import util.Http;
import util.SpecialCharacters;
import ch.dbs.actions.illformat.IllHandler;
import ch.dbs.actions.openurl.ConvertOpenUrl;
import ch.dbs.entity.AbstractBenutzer;
import ch.dbs.entity.Bestellungen;
import ch.dbs.entity.Lieferanten;
import ch.dbs.entity.OrderState;
import ch.dbs.entity.Text;
import ch.dbs.form.ActiveMenusForm;
import ch.dbs.form.ErrorMessage;
import ch.dbs.form.GbvSruForm;
import ch.dbs.form.IllForm;
import ch.dbs.form.Message;
import ch.dbs.form.OrderForm;
import ch.dbs.form.UserInfo;
import enums.Connect;
import enums.Result;
import enums.TextType;

/**
 * Klasse um Metainformation wie PPN zu bestimmen und Bestellungen beim GBV
 * durchzuführen.
 * 
 * @author Markus Fischer
 */
public final class OrderGbvAction extends DispatchAction {

    private static final Logger LOG = LoggerFactory.getLogger(OrderGbvAction.class);
    private static final String BASEURL = "http://cbs4.gbv.de:8080/cgi-bin/vuefl/vuefl_recv_data.pl";

    /**
     * löst eine Bestellung beim GBV aus.
     */
    public ActionForward order(final ActionMapping mp, final ActionForm fm, final HttpServletRequest rq,
            final HttpServletResponse rp) {

        final Auth auth = new Auth();
        // make sure the user is logged in
        if (!auth.isLogin(rq)) {
            return mp.findForward(Result.ERROR_TIMEOUT.getValue());
        }

        String forward = Result.FAILURE.getValue();
        final OrderForm of = (OrderForm) fm;
        final OrderState orderstate = new OrderState();
        final OrderAction orderAction = new OrderAction();
        final IllHandler illHandler = new IllHandler();
        final Lieferanten supplier = new Lieferanten();
        final UserInfo ui = (UserInfo) rq.getSession().getAttribute("userinfo");
        final Text t = new Text();
        final Check ck = new Check();
        // URL Test-System des GBV
        //    String baseurl = "http://cbst.gbv.de:8080/cgi-bin/vuefl_0702/vuefl_recv_data.pl";

        try {

            // Fileformat für Statistik aus gemachten Angaben schliessen
            if ("Buch".equals(of.getMediatype())) {
                of.setFileformat("Papierkopie");
            } else {
                if (of.getDeloptions().equals("post")) {
                    of.setFileformat("Papierkopie");
                }
                if (of.getDeloptions().equals("fax")) {
                    of.setFileformat("Papierkopie");
                }
                if (of.getDeloptions().equals("fax to pdf")) {
                    of.setFileformat("PDF");
                }
            }

            if (!"GBV-Suche".equals(of.getSubmit())) {

                // Bestellkunde darf nicht "deaktiviert" sein
                if (auth.isUserAccount(of.getForuser(), t.getConnection())) {
                    // Kontrolle max. Bestellungen aller Kunden pro Kalenderjahr auf einem Konto
                    if (auth.isMaxordersj(rq, t.getConnection())) {
                        // Kontrolle max. Bestellungen eines Kunden pro Kalenderjahr
                        if (auth.isMaxordersutotal(of.getForuser(), rq, t.getConnection())) {

                            try {

                                // *** Bestelltyp Copy
                                if ("Artikel".equals(of.getMediatype()) || "Teilkopie Buch".equals(of.getMediatype())) {

                                    // noch keine PPN vorhanden, d.h. choosehits.do noch nicht durchlaufen
                                    if (of.getPpn() == null) {

                                        GbvSruForm gsf = new GbvSruForm();
                                        List<GbvSruForm> matches = new ArrayList<GbvSruForm>();

                                        // *** hier gibt es zwei Chancen ein GBV-Bestellobjekt zu erhalten:
                                        // ZDB-ID und ISSN vorhanden
                                        if (ck.isMinLength(of.getZdbid(), 2) && ck.isMinLength(of.getIssn(), 8)) {
                                            try { // allfällige SRU-Fehler-Codes abfangen
                                                  // 1. Chance anhand der vorhandenen ZDB-ID
                                                matches = getGbvMatches(getGbvSrucontentSearchAsPhrase("zdb",
                                                        of.getZdbid(), 1));
                                                of.setGbvfield("zdb");
                                                of.setGbvsearch(of.getZdbid());
                                            } catch (final Exception e) {
                                                LOG.info("GBV-Message: " + e.getMessage());
                                                final Message m = new Message();
                                                m.setMessage(e.getMessage());
                                                rq.setAttribute("gbvmessage", m);
                                            }
                                            gsf = getGbvOrderObject(matches, of);
                                            // ...ist das Bestellobjekt da und kein E-Journal?
                                            if (gsf.getPpn_003AT() == null || gbvIsEjournal(gsf)) {
                                                // 2. Chance anhand der vorhandenen ISSN
                                                of.setZdbid(orderAction.getZdbidFromIssn(of.getIssn(),
                                                        t.getConnection()));
                                                // hier kann es sein dass dbs keine zdbid liefert
                                                // => ggf. Suche anhand ISSN
                                                if (of.getZdbid() != null && !of.getZdbid().equals("")) {
                                                    try { // allfällige SRU-Fehler-Codes abfangen
                                                        matches = getGbvMatches(getGbvSrucontentSearchAsPhrase("zdb",
                                                                of.getZdbid(), 1));
                                                        of.setGbvfield("zdb");
                                                        of.setGbvsearch(of.getZdbid());
                                                        gsf = getGbvOrderObject(matches, of);
                                                    } catch (final Exception e) {
                                                        LOG.info("GBV-Message: " + e.getMessage());
                                                        final Message m = new Message();
                                                        m.setMessage(e.getMessage());
                                                        rq.setAttribute("gbvmessage", m);
                                                    }
                                                }
                                                // ...ist das Bestellobjekt da und kein E-Journal?
                                                if (gsf.getPpn_003AT() == null || gbvIsEjournal(gsf)) {
                                                    try { // allfällige SRU-Fehler-Codes abfangen
                                                          // 3. anhand der ISSN eine GBV-Suche machen
                                                        matches = getGbvMatches(getGbvSrucontentSearchAsPhrase("iss",
                                                                of.getIssn(), 1));
                                                        of.setGbvfield("iss");
                                                        of.setGbvsearch(of.getIssn());
                                                        gsf = getGbvOrderObject(matches, of);
                                                    } catch (final Exception e) {
                                                        LOG.info("GBV-Message: " + e.getMessage());
                                                        final Message m = new Message();
                                                        m.setMessage(e.getMessage());
                                                        rq.setAttribute("gbvmessage", m);
                                                    }
                                                }
                                            }
                                            // *** normaler Versuch GBV-Bestellobjekt zu erhalten
                                        } else {
                                            // handelt es sich um eine Zeitschrift?
                                            if (ck.isMinLength(of.getIssn(), 2)) {
                                                of.setZdbid(orderAction.getZdbidFromIssn(of.getIssn(),
                                                        t.getConnection()));
                                                // hier kann es sein dass dbs keine zdbid liefert
                                                // => ggf. Suche anhand ISSN
                                                if (of.getZdbid() != null && !of.getZdbid().equals("")) {
                                                    try { // allfällige SRU-Fehler-Codes abfangen
                                                        matches = getGbvMatches(getGbvSrucontentSearchAsPhrase("zdb",
                                                                of.getZdbid(), 1));
                                                        of.setGbvfield("zdb");
                                                        of.setGbvsearch(of.getZdbid());
                                                        gsf = getGbvOrderObject(matches, of);
                                                    } catch (final Exception e) {
                                                        LOG.info("GBV-Message: " + e.getMessage());
                                                        final Message m = new Message();
                                                        m.setMessage(e.getMessage());
                                                        rq.setAttribute("gbvmessage", m);
                                                    }
                                                }
                                                // ...ist das Bestellobjekt da und kein E-Journal?
                                                if (gsf.getPpn_003AT() == null || gbvIsEjournal(gsf)) {
                                                    try { // allfällige SRU-Fehler-Codes abfangen
                                                        matches = getGbvMatches(getGbvSrucontentSearchAsPhrase("iss",
                                                                of.getIssn(), 1));
                                                        of.setGbvfield("iss");
                                                        of.setGbvsearch(of.getIssn());
                                                        gsf = getGbvOrderObject(matches, of);
                                                    } catch (final Exception e) {
                                                        LOG.info("GBV-Message: " + e.getMessage());
                                                        final Message m = new Message();
                                                        m.setMessage(e.getMessage());
                                                        rq.setAttribute("gbvmessage", m);
                                                    }
                                                }
                                                // falls Bestimmung anhand ISSN fehlgeschlagen ist => Titelsuche
                                                if (matches.isEmpty() && ck.isMinLength(of.getZeitschriftentitel(), 2)) {
                                                    try { // allfällige SRU-Fehler-Codes abfangen
                                                          // erster Versuch eng, als Phrase
                                                        matches = getGbvMatches(getGbvSrucontentSearch("zti",
                                                                of.getZeitschriftentitel(), 1));
                                                        of.setGbvfield("zti");
                                                        of.setGbvsearch(of.getZeitschriftentitel());
                                                        // zweiter Versuch als Stichwort-Suche
                                                        if (matches.isEmpty()) {
                                                            matches = getGbvMatches(getGbvSrucontentSearch("sha",
                                                                    of.getZeitschriftentitel(), 1));
                                                            of.setGbvfield("sha");
                                                            of.setGbvsearch(of.getZeitschriftentitel());
                                                        }
                                                    } catch (final Exception e) {
                                                        LOG.info("GBV-Message: " + e.getMessage());
                                                        final Message m = new Message();
                                                        m.setMessage(e.getMessage());
                                                        rq.setAttribute("gbvmessage", m);
                                                    }
                                                    gsf = getGbvOrderObject(matches, of);
                                                }
                                            } else {
                                                if (of.getMediatype().equals("Teilkopie Buch")) {
                                                    // Versuch anhand der ISBN GBV-Bestellobjekt zu erhalten
                                                    if (ck.isMinLength(of.getIsbn(), 2)) {
                                                        try { // allfällige SRU-Fehler-Codes abfangen
                                                            matches = getGbvMatches(getGbvSrucontentSearchAsPhrase(
                                                                    "isb", of.getIsbn(), 1));
                                                            of.setGbvfield("isb");
                                                            of.setGbvsearch(of.getIsbn());
                                                            gsf = getGbvOrderObject(matches, of);
                                                        } catch (final Exception e) {
                                                            LOG.info("GBV-Message: " + e.getMessage());
                                                            final Message m = new Message();
                                                            m.setMessage(e.getMessage());
                                                            rq.setAttribute("gbvmessage", m);
                                                        }
                                                    } else {
                                                        // Versuch anhand des Titels ein GBV-Bestellobjekt zu erhalten
                                                        try { // allfällige SRU-Fehler-Codes abfangen
                                                              // erster Versuch eng, als Phrase
                                                            matches = getGbvMatches(getGbvSrucontentSearchAsPhrase(
                                                                    "tit", of.getBuchtitel(), 1));
                                                            of.setGbvfield("tit");
                                                            of.setGbvsearch(of.getBuchtitel());
                                                            // zweiter Versuch als Stichwort-Suche
                                                            if (matches.isEmpty()) {
                                                                matches = getGbvMatches(getGbvSrucontentSearch("tit",
                                                                        of.getBuchtitel(), 1));
                                                                of.setGbvfield("tit");
                                                                of.setGbvsearch(of.getBuchtitel());
                                                            }
                                                            // dritter Versuch über alle Wörter
                                                            if (matches.isEmpty()) {
                                                                matches = getGbvMatches(getGbvSrucontentSearch("all",
                                                                        of.getBuchtitel(), 1));
                                                                of.setGbvfield("all");
                                                                of.setGbvsearch(of.getBuchtitel());
                                                            }
                                                        } catch (final Exception e) {
                                                            LOG.info("GBV-Message: " + e.getMessage());
                                                            final Message m = new Message();
                                                            m.setMessage(e.getMessage());
                                                            rq.setAttribute("gbvmessage", m);
                                                        }
                                                        gsf = getGbvOrderObject(matches, of);
                                                    }
                                                } else { // Titelsuche für Zeitschriften ohne ISSN
                                                    if (ck.isMinLength(of.getZeitschriftentitel(), 2)) {
                                                        try { // allfällige SRU-Fehler-Codes abfangen
                                                              // erster Versuch eng, als Phrase
                                                            matches = getGbvMatches(getGbvSrucontentSearch("zti",
                                                                    of.getZeitschriftentitel(), 1));
                                                            of.setGbvfield("zti");
                                                            of.setGbvsearch(of.getZeitschriftentitel());
                                                            // zweiter Versuch als Stichwort-Suche
                                                            if (matches.isEmpty()) {
                                                                matches = getGbvMatches(getGbvSrucontentSearch("sha",
                                                                        of.getZeitschriftentitel(), 1));
                                                                of.setGbvfield("sha");
                                                                of.setGbvsearch(of.getZeitschriftentitel());
                                                            }
                                                        } catch (final Exception e) {
                                                            LOG.info("GBV-Message: " + e.getMessage());
                                                            final Message m = new Message();
                                                            m.setMessage(e.getMessage());
                                                            rq.setAttribute("gbvmessage", m);
                                                        }
                                                        gsf = getGbvOrderObject(matches, of);
                                                    }
                                                }
                                            }
                                        }

                                        // *** ein Bestellobjekt erhalten und nicht ein E-Journal => Bestellverarbeitung
                                        if (gsf.getPpn_003AT() != null && !gbvIsEjournal(gsf)
                                                && of.getMediatype().equals("Artikel")) {
                                            // bei Teilkopie Buch immer zuerst zur Trefferauswahl!

                                            if (of.isManuell()) { // manuelle Bestellung
                                                forward = "redirect";
                                                // of.setLink("http://gso.gbv.de/login/FORM/REQUEST?DBS_ID=2.1&DB=2.1&USER_KEY=" + ui.getKonto().getGbvbenutzername() + "&PASSWORD=" +
                                                // ui.getKonto().getGbvpasswort() + "&REDIRECT=http%3A%2F%2Fgso.gbv.de%2Frequest%2FFORCETT%3DHTML%2FDB%3D2.1%2FFORM%2F" +
                                                // "COPY%3FPPN%3D" + gsf.getPpn_003AT() + "%26LANGCODE%3DDU");
                                                // bei Artikeln kann per OpenURL die Datenübergeben übergeben werden (bessere Übernahme der Angaben)
                                                of.setLink("http://www.gbv.de/gso/opengso.php?sid=DRDOC:doctor-doc&db=GVK&genre=article&issn="
                                                        + of.getIssn()
                                                        + "&date="
                                                        + of.getJahr()
                                                        + "&volume="
                                                        + of.getJahrgang()
                                                        + "&issue="
                                                        + of.getHeft()
                                                        + "&pages="
                                                        + of.getSeiten()
                                                        + "&title="
                                                        + of.getZeitschriftentitel()
                                                        + "&atitle="
                                                        + of.getArtikeltitel()
                                                        + "&aulast="
                                                        + of.getAuthor());

                                            } else { // *** automatische Bestellung
                                                of.setPpn(gsf.getPpn_003AT());
                                                IllForm gbv = new IllForm();
                                                gbv = illHandler.prepareGbvIllRequest(of, ui.getKonto(), ui);
                                                final String gbvanswer = illHandler.sendIllRequest(gbv, BASEURL);
                                                final String returnValue = illHandler.readGbvIllAnswer(gbvanswer);
                                                forward = "ordersuccess";
                                                if (gbvIsOrdernumber(gbvanswer)) { // autom. Bestellung erfolgreich
                                                    AbstractBenutzer kunde = new AbstractBenutzer();
                                                    kunde = kunde.getUser(Long.valueOf(of.getForuser()),
                                                            t.getConnection());
                                                    of.setLieferant(supplier.getLieferantFromName(
                                                            "GBV - Gemeinsamer Bibliotheksverbund", t.getConnection()));
                                                    // doppelter Eintrag um Sortieren und Suche zu ermöglichen
                                                    of.setBestellquelle("GBV - Gemeinsamer Bibliotheksverbund");
                                                    of.setStatus("bestellt");
                                                    of.setKaufpreis(new BigDecimal("0.00"));
                                                    of.setWaehrung("EUR");
                                                    of.setTrackingnr(gbv.getTransaction_group_qualifier());
                                                    of.setGbvnr(returnValue);
                                                    final Bestellungen b = new Bestellungen(of, kunde, ui.getKonto());
                                                    if (of.getBid() == null) {
                                                        b.save(t.getConnection());
                                                    } else {
                                                        b.setId(of.getBid());
                                                        b.update(t.getConnection());
                                                    }
                                                    if (b.getId() != null) {
                                                        // Status bestellt setzen wenn Bestellung gültige ID hat
                                                        orderstate.setNewOrderState(b, ui.getKonto(),
                                                                new Text(t.getConnection(), TextType.STATE_ORDER,
                                                                        "bestellt"), null, ui.getBenutzer().getEmail(),
                                                                t.getConnection());
                                                    }

                                                } else {
                                                    final ErrorMessage em = new ErrorMessage();
                                                    em.setError("error.gbvorder");
                                                    em.setError_specific(returnValue);
                                                    em.setLink("searchfree.do?activemenu=suchenbestellen");
                                                    rq.setAttribute(Result.ERRORMESSAGE.getValue(), em);
                                                    // Für die manuelle Bestellung
                                                    of.setLink("http://gso.gbv.de/login/FORM/REQUEST?DBS_ID=2.1&DB=2.1&USER_KEY="
                                                            + ui.getKonto().getGbvbenutzername()
                                                            + "&PASSWORD="
                                                            + ui.getKonto().getGbvpasswort()
                                                            + "&REDIRECT=http%3A%2F%2Fgso.gbv.de%2Frequest%2FFORCETT%3DHTML%2FDB%3D2.1%2FFORM%2F"
                                                            + "COPY%3FPPN%3D" + of.getPpn() + "%26LANGCODE%3DDU");

                                                    LOG.debug("Failure GBV-Order: "
                                                            + ui.getKonto().getBibliotheksname() + "\012" + returnValue
                                                            + "\012" + gbvanswer);
                                                }
                                            }
                                            // *** keine (0), keine eindeutigen (>1) oder nur ein E-Journal (1) erhalten
                                        } else {
                                            if (!matches.isEmpty()) { // Teilkopie oder E-Journal kann 1 sein
                                                // Pfad auf Trefferauswahl
                                                forward = "trefferauswahl";
                                                of.setTreffer_total(matches.get(0).getTreffer_total());
                                                final int start = matches.get(0).getStart_record();
                                                final int max = matches.get(0).getMaximum_record();
                                                if (of.getTreffer_total() - (start + max - 1) > 0) {
                                                    of.setForwrd(start + max);
                                                }
                                                if (start - max > 0) {
                                                    of.setBack(start - max);
                                                }
                                                rq.setAttribute("matches", matches);
                                            } else {
                                                // Pfad auf Suche
                                                forward = "search";
                                            }
                                        }

                                        // *** PPN bereits vorhanden => Bestellverarbeitung
                                    } else {

                                        if (of.isManuell()) { // manuelle Bestellung
                                            forward = "redirect";
                                            if (!"Artikel".equals(of.getMediatype())) { // Bei Büchern und Teilkopien...
                                                of.setLink("http://gso.gbv.de/login/FORM/REQUEST?DBS_ID=2.1&DB=2.1&USER_KEY="
                                                        + ui.getKonto().getGbvbenutzername()
                                                        + "&PASSWORD="
                                                        + ui.getKonto().getGbvpasswort()
                                                        + "&REDIRECT=http%3A%2F%2Fgso.gbv.de%2Frequest%2FFORCETT%3DHTML%2FDB%3D2.1%2FFORM%2F"
                                                        + "COPY%3FPPN%3D" + of.getPpn() + "%26LANGCODE%3DDU");
                                            } else { // bei Artikeln
                                                // bei Artikeln kann per OpenURL die Datenübergeben übergeben werden (bessere Übernahme der Angaben)
                                                of.setLink("http://www.gbv.de/gso/opengso.php?sid=DRDOC:doctor-doc&db=GVK&genre=article&issn="
                                                        + of.getIssn()
                                                        + "&date="
                                                        + of.getJahr()
                                                        + "&volume="
                                                        + of.getJahrgang()
                                                        + "&issue="
                                                        + of.getHeft()
                                                        + "&pages="
                                                        + of.getSeiten()
                                                        + "&title="
                                                        + of.getZeitschriftentitel()
                                                        + "&atitle="
                                                        + of.getArtikeltitel()
                                                        + "&aulast="
                                                        + of.getAuthor());
                                            }
                                        } else { // *** automatische Bestellung
                                            IllForm gbv = new IllForm();
                                            gbv = illHandler.prepareGbvIllRequest(of, ui.getKonto(), ui);
                                            final String gbvanswer = illHandler.sendIllRequest(gbv, BASEURL);
                                            final String returnValue = illHandler.readGbvIllAnswer(gbvanswer);
                                            forward = "ordersuccess";
                                            if (gbvIsOrdernumber(gbvanswer)) { // autom. Bestellung erfolgreich
                                                AbstractBenutzer kunde = new AbstractBenutzer();
                                                kunde = kunde.getUser(Long.valueOf(of.getForuser()), t.getConnection());
                                                of.setLieferant(supplier.getLieferantFromName(
                                                        "GBV - Gemeinsamer Bibliotheksverbund", t.getConnection()));
                                                // doppelter Eintrag um Sortieren und Suche zu ermöglichen
                                                of.setBestellquelle("GBV - Gemeinsamer Bibliotheksverbund");
                                                of.setStatus("bestellt");
                                                of.setKaufpreis(new BigDecimal("0.00"));
                                                of.setWaehrung("EUR");
                                                of.setTrackingnr(gbv.getTransaction_group_qualifier());
                                                of.setGbvnr(returnValue);
                                                final Bestellungen b = new Bestellungen(of, kunde, ui.getKonto());
                                                if (of.getBid() == null) {
                                                    b.save(t.getConnection());
                                                } else {
                                                    b.setId(of.getBid());
                                                    b.update(t.getConnection());
                                                }
                                                if (b.getId() != null) {
                                                    // Status bestellt setzen wenn Bestellung gültige ID hat
                                                    orderstate.setNewOrderState(b, ui.getKonto(),
                                                            new Text(t.getConnection(), TextType.STATE_ORDER,
                                                                    "bestellt"), null, ui.getBenutzer().getEmail(), t
                                                                    .getConnection());
                                                }

                                            } else {
                                                final ErrorMessage em = new ErrorMessage();
                                                em.setError("error.gbvorder");
                                                em.setError_specific(returnValue);
                                                em.setLink("searchfree.do?activemenu=suchenbestellen");
                                                rq.setAttribute(Result.ERRORMESSAGE.getValue(), em);
                                                // Für die manuelle Bestellung
                                                of.setLink("http://gso.gbv.de/login/FORM/REQUEST?DBS_ID=2.1&DB=2.1&USER_KEY="
                                                        + ui.getKonto().getGbvbenutzername()
                                                        + "&PASSWORD="
                                                        + ui.getKonto().getGbvpasswort()
                                                        + "&REDIRECT=http%3A%2F%2Fgso.gbv.de%2Frequest%2FFORCETT%3DHTML%2FDB%3D2.1%2FFORM%2F"
                                                        + "COPY%3FPPN%3D" + of.getPpn() + "%26LANGCODE%3DDU");

                                                LOG.debug("Failure GBV-Order: " + ui.getKonto().getBibliotheksname()
                                                        + "\012" + returnValue + "\012" + gbvanswer);
                                            }
                                        }

                                    }

                                    // *** Bestelltyp Loan
                                } else {
                                    // noch keine PPN vorhanden, d.h. choosehits.do noch nicht durchlaufen
                                    if (of.getPpn() == null) {

                                        GbvSruForm gsf = new GbvSruForm();
                                        List<GbvSruForm> matches = new ArrayList<GbvSruForm>();

                                        // Versuch anhand der ISBN GBV-Bestellobjekt zu erhalten
                                        if (ck.isMinLength(of.getIsbn(), 2)) {
                                            try { // allfällige SRU-Fehler-Codes abfangen
                                                matches = getGbvMatches(getGbvSrucontentSearchAsPhrase("isb",
                                                        of.getIsbn(), 1));
                                                of.setGbvfield("isb");
                                                of.setGbvsearch(of.getIsbn());
                                            } catch (final Exception e) {
                                                LOG.info("GBV-Message: " + e.getMessage());
                                                final Message m = new Message();
                                                m.setMessage(e.getMessage());
                                                rq.setAttribute("gbvmessage", m);
                                            }
                                            gsf = getGbvOrderObject(matches, of);
                                        } else { // Versuch anhand des Titels ein GBV-Bestellobjekt zu erhalten
                                            try { // allfällige SRU-Fehler-Codes abfangen
                                                  // erster Versuch eng, als Phrase
                                                matches = getGbvMatches(getGbvSrucontentSearchAsPhrase("tit",
                                                        of.getBuchtitel(), 1));
                                                of.setGbvfield("tit");
                                                of.setGbvsearch(of.getBuchtitel());
                                                // zweiter Versuch als Stichwort-Suche
                                                if (matches.isEmpty()) {
                                                    matches = getGbvMatches(getGbvSrucontentSearch("tit",
                                                            of.getBuchtitel(), 1));
                                                    of.setGbvfield("tit");
                                                    of.setGbvsearch(of.getBuchtitel());
                                                }
                                                // dritter Versuch über alle Wörter
                                                if (matches.isEmpty()) {
                                                    matches = getGbvMatches(getGbvSrucontentSearch("all",
                                                            of.getBuchtitel(), 1));
                                                    of.setGbvfield("all");
                                                    of.setGbvsearch(of.getBuchtitel());
                                                }
                                            } catch (final Exception e) {
                                                LOG.info("GBV-Message: " + e.getMessage());
                                                final Message m = new Message();
                                                m.setMessage(e.getMessage());
                                                rq.setAttribute("gbvmessage", m);
                                            }
                                            gsf = getGbvOrderObject(matches, of);
                                        }

                                        // *** ein Loan-Bestellobjekt erhalten => Bestellverarbeitung
                                        if (gsf.getPpn_003AT() != null) {

                                            if (of.isManuell()) { // manuelle Bestellung
                                                forward = "redirect";
                                                of.setLink("http://gso.gbv.de/login/FORM/REQUEST?DBS_ID=2.1&DB=2.1&USER_KEY="
                                                        + ui.getKonto().getGbvbenutzername()
                                                        + "&PASSWORD="
                                                        + ui.getKonto().getGbvpasswort()
                                                        + "&REDIRECT=http%3A%2F%2Fgso.gbv.de%2Frequest%2FFORCETT%3DHTML%2FDB%3D2.1%2FFORM%2F"
                                                        + "LOAN%3FPPN%3D" + gsf.getPpn_003AT() + "%26LANGCODE%3DDU");
                                            } else { // *** Trefferauswahl vor automatischer Bestellung bei Büchern
                                                // Hier wird auf jeden Fall Choosehits durchlaufen, da die
                                                // Angaben sonst zu ungenau sind!!!
                                                // Pfad auf Trefferauswahl
                                                forward = "trefferauswahl";
                                                of.setTreffer_total(matches.get(0).getTreffer_total());
                                                final int start = matches.get(0).getStart_record();
                                                final int max = matches.get(0).getMaximum_record();
                                                if (of.getTreffer_total() - (start + max - 1) > 0) {
                                                    of.setForwrd(start + max);
                                                }
                                                if (start - max > 0) {
                                                    of.setBack(start - max);
                                                }
                                                rq.setAttribute("matches", matches);
                                            }
                                            // *** keine (0), keine eindeutigen (>1)
                                        } else {
                                            if (matches.size() > 1) {
                                                // Pfad auf Trefferauswahl
                                                forward = "trefferauswahl";
                                                of.setTreffer_total(matches.get(0).getTreffer_total());
                                                final int start = matches.get(0).getStart_record();
                                                final int max = matches.get(0).getMaximum_record();
                                                if (of.getTreffer_total() - (start + max - 1) > 0) {
                                                    of.setForwrd(start + max);
                                                }
                                                if (start - max > 0) {
                                                    of.setBack(start - max);
                                                }
                                                rq.setAttribute("matches", matches);
                                            } else {
                                                // Pfad auf Suche
                                                forward = "search";
                                            }
                                        }
                                        // *** PPN vorhanden => Bestellverarbeitung Loan
                                    } else {

                                        if (of.isManuell()) { // manuelle Bestellung
                                            forward = "redirect";
                                            of.setLink("http://gso.gbv.de/login/FORM/REQUEST?DBS_ID=2.1&DB=2.1&USER_KEY="
                                                    + ui.getKonto().getGbvbenutzername()
                                                    + "&PASSWORD="
                                                    + ui.getKonto().getGbvpasswort()
                                                    + "&REDIRECT=http%3A%2F%2Fgso.gbv.de%2Frequest%2FFORCETT%3DHTML%2FDB%3D2.1%2FFORM%2F"
                                                    + "LOAN%3FPPN%3D" + of.getPpn() + "%26LANGCODE%3DDU");
                                        } else { // *** automatische Bestellung
                                            IllForm gbv = new IllForm();
                                            gbv = illHandler.prepareGbvIllRequest(of, ui.getKonto(), ui);
                                            final String gbvanswer = illHandler.sendIllRequest(gbv, BASEURL);
                                            final String returnValue = illHandler.readGbvIllAnswer(gbvanswer);
                                            forward = "ordersuccess";
                                            if (gbvIsOrdernumber(gbvanswer)) { // autom. Bestellung erfolgreich
                                                AbstractBenutzer kunde = new AbstractBenutzer();
                                                kunde = kunde.getUser(Long.valueOf(of.getForuser()), t.getConnection());
                                                of.setLieferant(supplier.getLieferantFromName(
                                                        "GBV - Gemeinsamer Bibliotheksverbund", t.getConnection()));
                                                // doppelter Eintrag um Sortieren und Suche zu ermöglichen
                                                of.setBestellquelle("GBV - Gemeinsamer Bibliotheksverbund");
                                                of.setStatus("bestellt");
                                                of.setKaufpreis(new BigDecimal("0.00"));
                                                of.setWaehrung("EUR");
                                                of.setTrackingnr(gbv.getTransaction_group_qualifier());
                                                of.setGbvnr(returnValue);
                                                final Bestellungen b = new Bestellungen(of, kunde, ui.getKonto());
                                                if (of.getBid() == null) {
                                                    b.save(t.getConnection());
                                                } else {
                                                    b.setId(of.getBid());
                                                    b.update(t.getConnection());
                                                }
                                                if (b.getId() != null) {
                                                    // Status bestellt setzen wenn Bestellung gültige ID hat
                                                    orderstate.setNewOrderState(b, ui.getKonto(),
                                                            new Text(t.getConnection(), TextType.STATE_ORDER,
                                                                    "bestellt"), null, ui.getBenutzer().getEmail(), t
                                                                    .getConnection());
                                                }

                                            } else {
                                                final ErrorMessage em = new ErrorMessage();
                                                em.setError("error.gbvorder");
                                                em.setError_specific(returnValue);
                                                em.setLink("searchfree.do?activemenu=suchenbestellen");
                                                rq.setAttribute(Result.ERRORMESSAGE.getValue(), em);
                                                // Für die manuelle Bestellung
                                                of.setLink("http://gso.gbv.de/login/FORM/REQUEST?DBS_ID=2.1&DB=2.1&USER_KEY="
                                                        + ui.getKonto().getGbvbenutzername()
                                                        + "&PASSWORD="
                                                        + ui.getKonto().getGbvpasswort()
                                                        + "&REDIRECT=http%3A%2F%2Fgso.gbv.de%2Frequest%2FFORCETT%3DHTML%2FDB%3D2.1%2FFORM%2F"
                                                        + "COPY%3FPPN%3D" + of.getPpn() + "%26LANGCODE%3DDU");

                                                LOG.debug("Failure GBV-Order: " + ui.getKonto().getBibliotheksname()
                                                        + "\012" + returnValue + "\012" + gbvanswer);
                                            }
                                        }

                                    }

                                }

                            } catch (final Exception e) {
                                final ErrorMessage em = new ErrorMessage();
                                em.setError("error.system");
                                em.setLink("searchfree.do?activemenu=suchenbestellen");
                                rq.setAttribute(Result.ERRORMESSAGE.getValue(), em);
                                LOG.error("order: " + e.toString());
                            }

                        } else {
                            of.setIssn(null); // naja, unterdrücken von "manuell bestellen"...
                            rq.setAttribute("orderform", of);
                            final ErrorMessage em = new ErrorMessage("error.maxorder", "searchfree.do");
                            rq.setAttribute(Result.ERRORMESSAGE.getValue(), em);
                        }
                    } else {
                        of.setIssn(null); // naja, unterdrücken von "manuell bestellen"...
                        rq.setAttribute("orderform", of);
                        final ErrorMessage em = new ErrorMessage("error.maxorderyear", "searchfree.do");
                        rq.setAttribute(Result.ERRORMESSAGE.getValue(), em);
                    }
                } else {
                    of.setIssn(null); // naja, unterdrücken von "manuell bestellen"...
                    rq.setAttribute("orderform", of);
                    final ErrorMessage em = new ErrorMessage("error.inactive", "searchfree.do");
                    rq.setAttribute(Result.ERRORMESSAGE.getValue(), em);
                }

            } else { // GBV-Suche
                forward = "search";
            }

            of.setSubmit("GBV");
            of.setPreisvorkomma("0");
            of.setPreisnachkomma("00");
            of.setWaehrung("EUR");

            rq.setAttribute("orderform", of);

            final ActiveMenusForm mf = new ActiveMenusForm();
            mf.setActivemenu("suchenbestellen");
            rq.setAttribute(Result.ACTIVEMENUS.getValue(), mf);

        } finally {
            t.close();
        }

        return mp.findForward(forward);
    }

    /**
     * führt eine GBV-Suche aus und holt die Treffer
     */
    public ActionForward search(final ActionMapping mp, final ActionForm fm, final HttpServletRequest rq,
            final HttpServletResponse rp) {

        final Auth auth = new Auth();
        // make sure the user is logged in
        if (!auth.isLogin(rq)) {
            return mp.findForward(Result.ERROR_TIMEOUT.getValue());
        }

        String forward = Result.FAILURE.getValue();
        final OrderForm of = (OrderForm) fm;

        try {

            final Check ck = new Check();
            int startRecord = 1;
            if (of.getBack() != 0) {
                startRecord = of.getBack();
            } // in der Anzeige wurde zurück geblättert
            if (of.getForwrd() != 0) {
                startRecord = of.getForwrd();
            } // in der Anzeige wurde vorwärts geblättert
            of.setBack(0); // zurücksetzen für korrkete Anzeige
            of.setForwrd(0); // // zurücksetzen für korrkete Anzeige

            if (ck.isMinLength(of.getGbvsearch(), 1) && ck.isMinLength(of.getGbvfield(), 3)) { // Eingabe erfolgt

                try { // allfällige SRU-Fehler-Codes abfangen
                      // erste Suche eng als Phrase
                    List<GbvSruForm> matches = getGbvMatches(getGbvSrucontentSearchAsPhrase(of.getGbvfield(),
                            of.getGbvsearch(), startRecord));
                    // zweite Suche als Stichwörter
                    if (matches.isEmpty()) {
                        matches = getGbvMatches(getGbvSrucontentSearch(of.getGbvfield(), of.getGbvsearch(), startRecord));
                    }

                    if (!matches.isEmpty()) {
                        // Pfad auf Trefferauswahl
                        forward = "trefferauswahl";
                        of.setTreffer_total(matches.get(0).getTreffer_total());
                        final int start = matches.get(0).getStart_record();
                        final int max = matches.get(0).getMaximum_record();
                        if (of.getTreffer_total() - (start + max - 1) > 0) {
                            of.setForwrd(start + max);
                        }
                        if (start - max > 0) {
                            of.setBack(start - max);
                        }
                        rq.setAttribute("matches", matches);
                    } else {
                        // Pfad auf Suche
                        forward = "search";
                        final Message m = new Message();
                        m.setMessage("Keine Treffer!");
                        rq.setAttribute("gbvmessage", m);
                    }

                } catch (final Exception e) {
                    LOG.info("GBV-Message: " + e.getMessage());
                    forward = "search";
                    final Message m = new Message();
                    m.setMessage(e.getMessage());
                    rq.setAttribute("gbvmessage", m);
                }

            } else { // keine Eingabe erfolgt => zurück zur Suche
                forward = "search";
                final Message m = new Message();
                m.setMessage("Keine Treffer!");
                rq.setAttribute("gbvmessage", m);
            }

        } catch (final Exception e) {
            forward = Result.FAILURE.getValue();
            final ErrorMessage em = new ErrorMessage();
            em.setError("error.system");
            em.setLink("searchfree.do?activemenu=suchenbestellen");
            rq.setAttribute(Result.ERRORMESSAGE.getValue(), em);
            LOG.error("search: " + e.toString());
        }

        of.setSubmit("GBV");
        rq.setAttribute("orderform", of);

        final ActiveMenusForm mf = new ActiveMenusForm();
        mf.setActivemenu("suchenbestellen");
        rq.setAttribute(Result.ACTIVEMENUS.getValue(), mf);

        return mp.findForward(forward);

    }

    /**
     * validiert die Mussfelder
     */
    public ActionForward validate(final ActionMapping mp, final ActionForm fm, final HttpServletRequest rq,
            final HttpServletResponse rp) {

        final Auth auth = new Auth();
        // make sure the user is logged in
        if (!auth.isLogin(rq)) {
            return mp.findForward(Result.ERROR_TIMEOUT.getValue());
        }

        String forward = Result.FAILURE.getValue();
        final OrderForm of = (OrderForm) fm;
        final Text cn = new Text();

        try {

            if (!"GBV-Suche".equals(of.getSubmit())) {

                if (!gbvOrderValues(of)) {

                    forward = "missingvalues";
                    final Message m = new Message("Fehlende Mussfelder oder zuwenig Angaben für eine Bestellung.");
                    rq.setAttribute("gbvmessage", m);

                    final UserInfo ui = (UserInfo) rq.getSession().getAttribute("userinfo");

                    if (auth.isBenutzer(rq)) { // Benutzer sehen nur die eigenen Adressen
                        final List<AbstractBenutzer> kontouser = new ArrayList<AbstractBenutzer>();
                        final AbstractBenutzer b = ui.getBenutzer();
                        kontouser.add(b);
                        of.setKontouser(kontouser);
                    } else {
                        of.setKontouser(ui.getBenutzer().getKontoUser(ui.getKonto(), cn.getConnection()));
                    }

                } else {
                    forward = Result.SUCCESS.getValue();
                }

            } else { // zur GBV-Suche
                forward = "search";
            }

            of.setSubmit("GBV");
            rq.setAttribute("orderform", of);

        } catch (final Exception e) {
            forward = Result.FAILURE.getValue();
            final ErrorMessage em = new ErrorMessage();
            em.setError("error.system");
            em.setLink("searchfree.do?activemenu=suchenbestellen");
            rq.setAttribute(Result.ERRORMESSAGE.getValue(), em);
            LOG.error("validate: " + e.toString());

        } finally {
            cn.close();
        }

        final ActiveMenusForm mf = new ActiveMenusForm();
        mf.setActivemenu("suchenbestellen");
        rq.setAttribute(Result.ACTIVEMENUS.getValue(), mf);

        return mp.findForward(forward);

    }

    /**
     * Ändert den angezeigten Medientyp für eine GBV-Bestellung.
     */
    public ActionForward changeMediatypeGbv(final ActionMapping mp, final ActionForm form, final HttpServletRequest rq,
            final HttpServletResponse rp) {

        final Auth auth = new Auth();
        // make sure the user is logged in
        if (!auth.isLogin(rq)) {
            return mp.findForward(Result.ERROR_TIMEOUT.getValue());
        }

        String forward = Result.SUCCESS.getValue();
        final OrderForm of = (OrderForm) form;
        final Text cn = new Text();

        try {
            final UserInfo ui = (UserInfo) rq.getSession().getAttribute("userinfo");

            if (auth.isBenutzer(rq)) { // Benutzer sehen nur die eigenen Adressen
                final List<AbstractBenutzer> kontouser = new ArrayList<AbstractBenutzer>();
                final AbstractBenutzer b = ui.getBenutzer();
                kontouser.add(b);
                of.setKontouser(kontouser);
            } else {
                of.setKontouser(ui.getBenutzer().getKontoUser(ui.getKonto(), cn.getConnection()));
            }

            if (of.getMediatype() == null) {
                of.setMediatype("Artikel");
            } // Defaultwert Artikel
            if (of.getMediatype().equals("Buch")) {
                of.setDeloptions("post"); // logische Konsequenz
                of.setFileformat("Papierkopie"); // logische Konsequenz
            }

            // ISIL und requester-id werden für autom. Bestellung benötigt
            if (ui.getKonto().getIsil() == null || ui.getKonto().getGbvrequesterid() == null) {
                of.setManuell(true);
            } else {
                of.setManuell(false);
            }
            of.setSubmit("GBV");
            of.setMaximum_cost("8");

            rq.setAttribute("orderform", of);

        } catch (final Exception e) {
            forward = Result.FAILURE.getValue();
            final ErrorMessage em = new ErrorMessage();
            em.setError("error.system");
            em.setLink("searchfree.do?activemenu=suchenbestellen");
            rq.setAttribute(Result.ERRORMESSAGE.getValue(), em);
            LOG.error("changeMediaType: " + e.toString());
        } finally {
            cn.close();
        }

        final ActiveMenusForm mf = new ActiveMenusForm();
        mf.setActivemenu("suchenbestellen");
        rq.setAttribute(Result.ACTIVEMENUS.getValue(), mf);

        return mp.findForward(forward);
    }

    /**
     * Holt ein bestellbares GBV-Objekt (Zeitschrift, Buch, Film etc.)
     * 
     * @param OrderForm of
     * @return GbvSruForm
     */
    private GbvSruForm getGbvOrderObject(List<GbvSruForm> matches, final OrderForm of) {

        GbvSruForm gsf = new GbvSruForm();

        try {

            if (of.getMediatype().equals("Artikel")) {
                if (matches.size() == 1) {
                    gsf = matches.get(0);
                    // prüft, ob der Treffer ein E-Journal ist und holt ggf. die betreffende Print-Ausgabe
                    if (gbvIsEjournalRedirectable(gsf)) {
                        matches = getGbvMatches(getGbvSrucontentSearchAsPhrase("zdb",
                                gsf.getVerknuepfung_zdbid_horizontal(), 1));
                        // falls kein Print-J vorhanden, wird E-J zurückgegeben. Es folgt aber eine erneute Prüfung...
                        if (matches.size() == 1) {
                            gsf = matches.get(0);
                        }
                    }
                }
                return gsf;
            }
            if (of.getMediatype().equals("Teilkopie Buch")) {
                if (matches.size() == 1) {
                    gsf = matches.get(0);
                }
                return gsf;
            }
            if (of.getMediatype().equals("Buch")) {
                if (matches.size() == 1) {
                    gsf = matches.get(0);
                }
                return gsf;
            }

        } catch (final Exception e) {
            LOG.error("getGbvOrderObject(OrderForm of): " + e.getMessage());
        }

        return gsf;
    }

    /**
     * Liest die Treffer aus einem SRU-content und gibt sie in einem Array als
     * Objekte zurück.
     * 
     * @param String content
     * @return List<GbvSruForm>
     */
    private List<GbvSruForm> getGbvMatches(String content) throws Exception {

        final List<GbvSruForm> matches = new ArrayList<GbvSruForm>();
        int numberOfRecords = 0;
        int firstRecordPosition = 0;

        try {

            if (content.contains("<zs:numberOfRecords>")) {
                // kracht bewusst, wenn <zs:numberOfRecords></zs:numberOfRecords> gar keine Trefferangaben
                // enthält. Siehe catch
                numberOfRecords = Integer.valueOf(content.substring(content.indexOf("<zs:numberOfRecords>") + 20,
                        content.indexOf("</zs:numberOfRecords>")));

                // read SRU response only if we have records to read
                if (numberOfRecords > 0) {

                    firstRecordPosition = Integer.valueOf(content.substring(
                            content.indexOf("<zs:recordPosition>") + 19, content.indexOf("</zs:recordPosition>")));

                    while (content.contains("<zs:record>")) {

                        final GbvSruForm gsf = readSruRecord(content.substring(content.indexOf("<zs:record>"),
                                content.indexOf("</zs:record>")));
                        gsf.setTreffer_total(numberOfRecords);
                        gsf.setStart_record(firstRecordPosition);
                        // rechne
                        final int i = matches.size();
                        gsf.setRecord_number(gsf.getStart_record() + i);
                        matches.add(gsf);
                        content = content.substring(content.indexOf("</zs:record>") + 12);
                    }
                }
            }

        } catch (final Exception e) {
            LOG.error("getGbvMatches: " + e.getMessage() + "\012" + content);

            // Hier wird ein allfälliger SRU-Fehler-Code gelesen um auf der JSP angezeigt zu werden
            // z.B. "GBV: System temporarily unavailable" (s. Liste:
            // http://www.loc.gov/standards/sru/resources/diagnostics-list.html)
            if (content != null && content.contains("<diag:message>")) {
                final String error = getSruErrorCode(content);
                throw new Exception("GBV:\040" + error);
            }
        }

        return matches;
    }

    /**
     * Liest einen einzelnen Record (nur wichtigere Felder) aus einem
     * SRU-content und gibt ihn als GbvSruRecordForm zurück.
     * 
     * @param String content
     * @return GbvSruForm
     */
    private GbvSruForm readSruRecord(String content) {

        final GbvSruForm record = new GbvSruForm();
        final ConvertOpenUrl openurlConv = new ConvertOpenUrl();

        try {

            content = content.replaceAll("\012", ""); // Umbrüche entfernen

            if (content.contains("<datafield tag=\"003@\">")) {
                record.setPpn_003AT(getSruDatafield("003@", content));
            }
            // if (content.contains("<datafield tag=\"002@\">")) { record.setTyp_002AT(getSruDatafield("002@",
            // content)); } // wird unten erledigt
            // if (content.contains("<datafield tag=\"011@\">")) {
            // record.setErscheinungsjahr_011AT(getSruDatafield("011@", content)); } // wird unten erledigt
            if (content.contains("<datafield tag=\"019@\">")) {
                record.setErscheinungsland_019AT(getSruDatafield("019@", content));
            }
            if (content.contains("<datafield tag=\"031@\">")) {
                record.setErscheinungsverlauf_031AT(getSruDatafield("031@", content));
            }
            // if (content.contains("<datafield tag=\"031A\">")) { record.setUmfang_031A(getSruDatafield("031A",
            // content)); } // wird unten erledigt
            if (content.contains("<datafield tag=\"031N\">")) {
                record.setErscheinungsverlauf_normiert_031N(getSruDatafield("031N", content));
            }
            if (content.contains("<datafield tag=\"032@\">")) {
                record.setAusgabe_032AT(getSruDatafield("032@", content));
            }
            if (content.contains("<datafield tag=\"021V\">")) {
                record.setBand_021V_multipel(getSruDatafield("021V", content));
            }
            if (content.contains("<datafield tag=\"034K\">")) {
                record.setBegleitmaterial_034K(getSruDatafield("034K", content));
            }
            if (content.contains("<datafield tag=\"006T\">")) {
                record.setCip_num_db_006T_multipel(getSruDatafield("006T", content));
            }
            if (content.contains("<datafield tag=\"006G\">")) {
                record.setDnb_num_006G(getSruDatafield("006G", content));
            }
            if (content.contains("<datafield tag=\"004V\">")) {
                record.setDoi_004V_multipel(getSruDatafield("004V", content));
            }
            if (content.contains("<datafield tag=\"004L\">")) {
                record.setEan_004L_multipel(getSruDatafield("004L", content));
            }
            if (content.contains("<datafield tag=\"046Q\">")) {
                record.setEnthaltene_werke_046Q(getSruDatafield("046Q", content));
            }
            if (content.contains("<datafield tag=\"034I\">")) {
                record.setFormat_034I(getSruDatafield("034I", content));
            }
            if (content.contains("<datafield tag=\"037A\">")) {
                record.setFussnoten_unaufgegliedert_037A_multipel(getSruDatafield("037A", content));
            }
            if (content.contains("<datafield tag=\"028F\">")) {
                record.setGefeierte_personen_028F(getSruDatafield("028F", content));
            }
            // if (content.contains("<datafield tag=\"036C\">")) {
            // record.setGesamtheit_abteilungen_vorlage_036C(getSruDatafield("036C", content)); } // wird unten erledigt
            if (content.contains("<datafield tag=\"036D\">")) {
                record.setGesamtheit_ansetzungsform_036D(getSruDatafield("036D", content));
            }
            if (content.contains("<datafield tag=\"036F\">")) {
                record.setGesamtheit_ansetzungsform_036F_multipel(getSruDatafield("036F", content));
            }
            if (content.contains("<datafield tag=\"036E\">")) {
                record.setGesamtheit_vorlage_036E_multipel(getSruDatafield("036E", content));
            }
            // if (content.contains("<datafield tag=\"021A\">")) { record.setHauptsachtitel_021A(getSruDatafield("021A",
            // content)); } // wird unten erledigt
            if (content.contains("<datafield tag=\"021B\">")) {
                record.setHauptsachtitel_alternativ_021B(getSruDatafield("021B", content));
            }
            if (content.contains("<datafield tag=\"007E\">")) {
                record.setHochschulschriften_num_007E(getSruDatafield("007E", content));
            }
            // if (content.contains("<datafield tag=\"037C\">")) {
            // record.setHochschulschriftenvermerk_037C_multipel(getSruDatafield("037C", content)); } // wird unten
            // erledigt
            if (content.contains("<datafield tag=\"034M\">")) {
                record.setIllustration_034M(getSruDatafield("034M", content));
            }
            if (content.contains("<datafield tag=\"028E\">")) {
                record.setInterpreten_028E_multipel(getSruDatafield("028E", content));
            }
            if (content.contains("<datafield tag=\"004A\">")) {
                record.setIsbn_004A_multipel(getSruDatafield("004A", content));
            }
            if (content.contains("<datafield tag=\"004F\">")) {
                record.setIsmn_004F(getSruDatafield("004F", content));
            }
            if (content.contains("<datafield tag=\"005A\">")) {
                record.setIssn_005A_multipel(openurlConv.normalizeIssn(getSruDatafield("005A", content)));
            }
            if (content.contains("<datafield tag=\"029E\">")) {
                record.setKoerperschaft_als_interpret_029E_multipel(getSruDatafield("029E", content));
            }
            // if (content.contains("<datafield tag=\"029A\">")) {
            // record.setKoerperschaft_erste_029A(getSruDatafield("029A", content)); } // wird unten erledigt
            if (content.contains("<datafield tag=\"029K\">")) {
                record.setKoerperschaft_sonstige_029K_multipel(getSruDatafield("029K", content));
            }
            if (content.contains("<datafield tag=\"029F\">")) {
                record.setKoerperschaft_zweite_029F(getSruDatafield("029F", content));
            }
            if (content.contains("<datafield tag=\"030F\">")) {
                record.setKongresse_030F(getSruDatafield("030F", content));
            }
            if (content.contains("<datafield tag=\"006A\">")) {
                record.setLoc_num_006A(getSruDatafield("006A", content));
            }
            if (content.contains("<datafield tag=\"016H\">")) {
                record.setMaterialbenennung_allg_016H(getSruDatafield("016H", content));
            }
            if (content.contains("<datafield tag=\"034D\">")) {
                record.setMaterialbenennung_spezifisch_034D(getSruDatafield("034D", content));
            }
            // if (content.contains("<datafield tag=\"033A\">")) {
            // record.setOrt_verlag_033A_multipel(getSruDatafield("033A", content)); } // wird unten erledigt
            if (content.contains("<datafield tag=\"032B\">")) {
                record.setReprintvermerk_032B(getSruDatafield("032B", content));
            }
            if (content.contains("<datafield tag=\"010@\">")) {
                record.setSprache_010AT(getSruDatafield("010@", content));
            }
            if (content.contains("<datafield tag=\"021C\">")) {
                record.setUnterreihe_021C_multipel(getSruDatafield("021C", content));
            }
            // if (content.contains("<datafield tag=\"028A\">")) {
            // record.setVerfasser_erster_028A(getSruDatafield("028A", content)); } // wird unten erledigt
            if (content.contains("<datafield tag=\"028B\">")) {
                record.setVerfasser_zweiter_und_weitere_028B(getSruDatafield("028B", content));
            }
            // if (content.contains("<datafield tag=\"039B\">")) {
            // record.setVerknuepfung_groessere_einheit_039B_multipel(getSruDatafield("039B", content)); } // wird unten
            // erledigt
            // if (content.contains("<datafield tag=\"039D\">")) {
            // record.setVerknuepfung_horizontal_039D_multipel(getSruDatafield("039D", content)); } // wird unten
            // erledigt
            if (content.contains("<datafield tag=\"039C\">")) {
                record.setVerknuepfung_kleinere_einheit_039C_multipel(getSruDatafield("039C", content));
            }
            if (content.contains("<datafield tag=\"006Z\">")) {
                record.setZdbid_006Z_multipel(getSruDatafield("006Z", content));
            }
            if (content.contains("<datafield tag=\"021N\">")) {
                record.setZusaetze_und_verfasser_021N(getSruDatafield("021N", content));
            }
            if (content.contains("<datafield tag=\"021M\">")) {
                record.setZusatzwerk_021M_multipel(getSruDatafield("021M", content));
            }
            if (content.contains("<datafield tag=\"021G\">")) {
                record.setRezensierteswerk_021G_multipel(getSruDatafield("021G", content));
            }

            // hier folgen wichtige Subfields

            // kann mehrere Verweise enthalten...
            // grundsätzlich wird der letzte Verweise genommen, in der GbvOrderAction,
            // aber die manuelle Wahl gegeben bei anzahl039D > 1
            if (content.contains("<datafield tag=\"039D\">")) {
                String work = content; // Kopie für String Verkürzung
                while (work.contains("<datafield tag=\"039D\">")) {
                    final String contentCopy = work.substring(work.indexOf("<datafield tag=\"039D\">") + 22,
                            work.indexOf("</datafield>", work.indexOf("<datafield tag=\"039D\">")));
                    final String titel = getSruSubfield("a", contentCopy);
                    final String vortext = getSruSubfield("c", contentCopy);
                    final String ident = getSruSubfield("C", contentCopy); // Identifier für Querverweis, normalerweise ZDB
                    // in der Doku eigentlich Code 7, empirisch aber Code 6
                    final String zdbid = getSruSubfield("6", contentCopy);
                    final String titel2 = getSruSubfield("8", contentCopy); // kommt vermutlich nicht bis selten vor
                    final String ppn = getSruSubfield("9", contentCopy);
                    final StringBuffer text = new StringBuffer("");
                    if (!"".equals(vortext)) {
                        text.append(vortext);
                        text.append('\040');
                    }
                    text.append(titel);
                    text.append(titel2);
                    record.setVerknuepfung_horizontal_039D_multipel(text.toString());
                    if (!"".equals(ppn)) {
                        record.setVerknuepfung_ppn_horizontal(ppn);
                    }
                    if (!"".equals(zdbid) && "ZDB".equals(ident)) {
                        record.setVerknuepfung_zdbid_horizontal(zdbid);
                    }
                    if (record.getVerknuepfung_horizontal_039D_multipel().equals("")) {
                        record.setVerknuepfung_horizontal_039D_multipel(null); // Anzeigelogik
                    }
                    work = work.substring(work.indexOf("<datafield tag=\"039D\">") + 17); // Verkürzung
                    record.setAnzahl_039D(record.getAnzahl_039D() + 1);
                }
            }
            if (content.contains("<datafield tag=\"039B\">")) {
                final String contentCopy = content.substring(content.indexOf("<datafield tag=\"039B\">") + 22,
                        content.indexOf("</datafield>", content.indexOf("<datafield tag=\"039B\">")));
                final String textSammelw = getSruSubfield("a", contentCopy); // fortlaufende Sammelwerke
                final String zdbid = getSruSubfield("7", contentCopy);
                final String vortext = getSruSubfield("c", contentCopy);
                final String textUselbst = getSruSubfield("8", contentCopy); // unselbständige Werke
                final String ppn = getSruSubfield("9", contentCopy);
                final StringBuffer text = new StringBuffer("");
                if (!"".equals(vortext)) {
                    text.append(vortext);
                    text.append('\040');
                }
                text.append(textSammelw);
                text.append(textUselbst);
                record.setVerknuepfung_groessere_einheit_039B_multipel(text.toString());
                if (!"".equals(ppn)) {
                    record.setVerknuepfung_ppn_groesser(ppn);
                }
                if (!"".equals(zdbid)) {
                    record.setVerknuepfung_zdbid_groesser(zdbid);
                }
                if (record.getVerknuepfung_groessere_einheit_039B_multipel().equals("")) {
                    record.setVerknuepfung_groessere_einheit_039B_multipel(null); // Anzeigelogik
                }
            }
            if (content.contains("<datafield tag=\"031A\">")) {
                final String contentCopy = content.substring(content.indexOf("<datafield tag=\"031A\">") + 22,
                        content.indexOf("</datafield>", content.indexOf("<datafield tag=\"031A\">")));
                final String band = getSruSubfield("d", contentCopy);
                final String jahr = getSruSubfield("j", contentCopy);
                final String heft = getSruSubfield("e", contentCopy);
                //        String sonderheft = getSruSubfield("f", content_copy);
                final String seiten = getSruSubfield("h", contentCopy);
                final StringBuffer text = new StringBuffer(jahr);
                if (!"".equals(band)) {
                    if (!"".equals(jahr)) {
                        text.append(';');
                        text.append(band);
                    } else {
                        text.append(band);
                    }
                }
                if (!"".equals(heft)) {
                    text.append('(');
                    text.append(heft);
                    text.append(')');
                }
                if (!"".equals(seiten)) {
                    text.append(':');
                    text.append(seiten);
                }
                record.setUmfang_031A(text.toString());
                if (record.getUmfang_031A().equals("")) {
                    record.setUmfang_031A(null);
                } // Anzeigelogik
            }
            if (content.contains("<datafield tag=\"036C\">")) {
                final String contentCopy = content.substring(content.indexOf("<datafield tag=\"036C\">") + 22,
                        content.indexOf("</datafield>", content.indexOf("<datafield tag=\"036C\">")));
                String sachtitel = getSruSubfield("a", contentCopy);
                final String zusatz = getSruSubfield("y", contentCopy);
                final String band = getSruSubfield("l", contentCopy);
                if (!"".equals(zusatz) && !"".equals(sachtitel)) {
                    sachtitel = sachtitel + "\040:\040" + zusatz;
                }
                if (!"".equals(band)) {
                    record.setBandzaehlung(band + ":\040");
                }
                record.setGesamtheit_abteilungen_vorlage_036C(sachtitel);
                if (record.getGesamtheit_abteilungen_vorlage_036C().equals("")) {
                    record.setGesamtheit_abteilungen_vorlage_036C(null); // Anzeigelogik
                }
            }
            if (content.contains("<datafield tag=\"021A\">")) {
                final String contentCopy = content.substring(content.indexOf("<datafield tag=\"021A\">") + 22,
                        content.indexOf("</datafield>", content.indexOf("<datafield tag=\"021A\">")));
                String titel = getSruSubfield("a", contentCopy);
                final String untertitel = getSruSubfield("d", contentCopy);
                final String anmerkungen = getSruSubfield("h", contentCopy);
                if (!"".equals(untertitel) && !"".equals(titel)) {
                    titel = titel + "\040:\040" + untertitel;
                }
                if (!"".equals(anmerkungen) && !"".equals(titel)) {
                    titel = titel + "\040" + anmerkungen;
                }
                record.setHauptsachtitel_021A(titel);
                if (record.getHauptsachtitel_021A().equals("")) {
                    record.setHauptsachtitel_021A(null);
                } // Anzeigelogik
            }
            if (content.contains("<datafield tag=\"021G\">")) {
                final String contentCopy = content.substring(content.indexOf("<datafield tag=\"021G\">") + 22,
                        content.indexOf("</datafield>", content.indexOf("<datafield tag=\"021G\">")));
                final String titel = getSruSubfield("a", contentCopy);
                final String verfasser = getSruSubfield("g", contentCopy); // ...des Originals
                final String ausgabebez = getSruSubfield("d", contentCopy);
                final String rezensor = getSruSubfield("h", contentCopy); // rezensierende Person
                final String rezension = verfasser + ",\040" + titel + "\040:\040" + ausgabebez + "\040/\040"
                        + rezensor;
                record.setRezensierteswerk_021G_multipel(rezension);
                if (record.getRezensierteswerk_021G_multipel().equals("")) {
                    record.setRezensierteswerk_021G_multipel(null); // Anzeigelogik
                }
            }
            if (content.contains("<datafield tag=\"033A\">")) {
                final String contentCopy = content.substring(content.indexOf("<datafield tag=\"033A\">") + 22,
                        content.indexOf("</datafield>", content.indexOf("<datafield tag=\"033A\">")));
                final String ort = getSruSubfield("p", contentCopy);
                String verlag = getSruSubfield("n", contentCopy);
                if (!"".equals(verlag) && !"".equals(ort)) {
                    verlag = "\040:\040" + verlag;
                }
                record.setOrt_verlag_033A_multipel(ort + verlag);
                if (record.getOrt_verlag_033A_multipel().equals("")) {
                    record.setOrt_verlag_033A_multipel(null); // Anzeigelogik
                }
            }
            if (content.contains("<datafield tag=\"011@\">")) {
                final String contentCopy = content.substring(content.indexOf("<datafield tag=\"011@\">") + 22,
                        content.indexOf("</datafield>", content.indexOf("<datafield tag=\"011@\">")));
                String jahr = getSruSubfield("a", contentCopy);
                final String serie = getSruSubfield("n", contentCopy);
                if (!"".equals(serie)) {
                    jahr = serie;
                } // falls Verlauf vorhanden wird Verlauf genommen, sonst Jahr
                record.setErscheinungsjahr_011AT(jahr);
                if (record.getErscheinungsjahr_011AT().equals("")) {
                    record.setErscheinungsjahr_011AT(null); // Anzeigelogik
                }
            }
            if (content.contains("<datafield tag=\"028A\">")) {
                final String contentCopy = content.substring(content.indexOf("<datafield tag=\"028A\">") + 22,
                        content.indexOf("</datafield>", content.indexOf("<datafield tag=\"028A\">")));
                String name = "";
                final String nachname = getSruSubfield("a", contentCopy);
                final String praefix = getSruSubfield("c", contentCopy);
                final String vorname = getSruSubfield("d", contentCopy);
                final String zusatzVorname = getSruSubfield("e", contentCopy);
                final String zusatzName = getSruSubfield("f", contentCopy);
                final String lebensdaten = getSruSubfield("h", contentCopy);
                final String ident = getSruSubfield("p", contentCopy); // weitere identifizierende Angaben
                if (!"".equals(praefix)) {
                    name = praefix + "\040";
                }
                if (!"".equals(zusatzVorname)) {
                    name = name + zusatzVorname + "\040";
                }
                if (!"".equals(vorname)) {
                    name = name + vorname + "\040";
                }
                if (!"".equals(zusatzName)) {
                    name = name + zusatzName + "\040";
                }
                if (!"".equals(nachname)) {
                    name = name + nachname;
                }
                if (!"".equals(ident)) {
                    name = name + "\040" + ident;
                }
                if (!"".equals(lebensdaten)) {
                    name = name + "\040" + lebensdaten;
                }
                record.setVerfasser_erster_028A(name);
                if (record.getVerfasser_erster_028A().equals("")) {
                    record.setVerfasser_erster_028A(null); // Anzeigelogik
                }
            }
            if (content.contains("<datafield tag=\"029A\">")) {
                final String contentCopy = content.substring(content.indexOf("<datafield tag=\"029A\">") + 22,
                        content.indexOf("</datafield>", content.indexOf("<datafield tag=\"029A\">")));
                final String koerperschaft = getSruSubfield("8", contentCopy);
                record.setKoerperschaft_erste_029A(koerperschaft);
                if (record.getKoerperschaft_erste_029A().equals("")) {
                    record.setKoerperschaft_erste_029A(null); // Anzeigelogik
                }
            }
            if (content.contains("<datafield tag=\"037C\">")) {
                final String contentCopy = content.substring(content.indexOf("<datafield tag=\"037C\">") + 22,
                        content.indexOf("</datafield>", content.indexOf("<datafield tag=\"037C\">")));
                final String ort = getSruSubfield("b", contentCopy);
                String vermerk = getSruSubfield("a", contentCopy);
                if (!"".equals(vermerk) && !"".equals(ort)) {
                    vermerk = ",\040" + vermerk;
                }
                record.setHochschulschriftenvermerk_037C_multipel(ort + vermerk);
                if (record.getHochschulschriftenvermerk_037C_multipel().equals("")) {
                    record.setHochschulschriftenvermerk_037C_multipel(null); // Anzeigelogik
                }
            }
            if (content.contains("<datafield tag=\"002@\">")) {
                final String contentCopy = content.substring(content.indexOf("<datafield tag=\"002@\">") + 22,
                        content.indexOf("</datafield>", content.indexOf("<datafield tag=\"002@\">")));
                String code = getSruSubfield("0", contentCopy);
                code = resolveGbvMediatype(code);
                record.setTyp_002AT(code);
                if (record.getTyp_002AT().equals("")) {
                    record.setTyp_002AT(null);
                } // Anzeigelogik
            }
            if (content.contains("<datafield tag=\"009P\"")) {
                String contentVerk = content;
                while (contentVerk.contains("<datafield tag=\"009P\"")) {
                    final String contentCopy = contentVerk.substring(
                            contentVerk.indexOf("<datafield tag=\"009P\"") + 22,
                            contentVerk.indexOf("</datafield>", contentVerk.indexOf("<datafield tag=\"009P\"")));
                    final String license = getSruSubfield("S", contentCopy);
                    // 1 == frei zugänglich ; 0 == kostenpflichtig
                    if ("1".equals(license) && contentCopy.contains("<subfield code=\"a\">")) {
                        final String url = getSruSubfield("a", contentCopy);
                        // momentan nicht aktiviert, da Auflösung unklar...
                        //        String urn = getSruSubfield("g", content_copy);
                        record.setLink_009P_multiple(url);
                    }
                    contentVerk = contentVerk.substring(contentVerk.indexOf("<datafield tag=\"009P\"") + 20);
                }
            }

        } catch (final Exception e) {
            LOG.error("readSruRecord(String content): " + e.getMessage() + "\012" + content);
        }

        return record;
    }

    /**
     * Liest ein einzelnes Datafield aus einem SRU-record und gibt ihn als data
     * zurück.
     * 
     * @param String tag String content
     * @return String data
     */
    private String getSruDatafield(final String tag, String content) {

        final StringBuffer buf = new StringBuffer(128);
        final Pattern p = Pattern.compile("<subfield code=\".\">");

        try {

            while (content.contains("<datafield tag=\"" + tag + "\">")) { // ein Tag kann mehrmals vorkommen

                final String contentCopy = content.substring(content.indexOf("<datafield tag=\"" + tag + "\">") + 22,
                        content.indexOf("</datafield>", content.indexOf("<datafield tag=\"" + tag + "\">")));

                final Matcher m = p.matcher(contentCopy);

                while (m.find()) {

                    if (buf.length() > 0) {
                        // Trennzeichen, falls schon Inhalt vorhanden
                        buf.append("\040|\040");
                    }
                    buf.append(contentCopy.substring(m.start() + 19, contentCopy.indexOf("</subfield>", m.start())));
                }

                // Stringverkürzung für nächsten Treffer
                content = content.substring(content.indexOf("<datafield tag=\"" + tag + "\">") + 21);

            }

        } catch (final Exception e) {
            LOG.error("getSruDatafield(String content): " + e.getMessage() + "\012" + content);
        }

        if (buf.length() == 0) {
            buf.append("");
        } // nicht null zurückgeben

        return deleteAT(buf.toString());
    }

    /**
     * Liest ein einzelnes Subfield aus einem SRU-record und gibt ihn als data
     * zurück.
     * 
     * @param String code String content
     * @return String data
     */
    private String getSruSubfield(final String code, final String content) {

        String data = "";

        try {

            if (content.contains("<subfield code=\"" + code + "\">")) {

                data = content.substring(content.indexOf("<subfield code=\"" + code + "\">") + 19,
                        content.indexOf("</subfield>", content.indexOf("<subfield code=\"" + code + "\">")));

            }

        } catch (final Exception e) {
            LOG.error("getSruSubfield(String code, String content): " + code + "\040" + e.getMessage() + "\012"
                    + content);
        }

        return deleteAT(data);
    }

    /**
     * Liest Error aus SRU-Content aus
     * 
     * @param String content
     * @return String error
     */
    private String getSruErrorCode(final String content) {

        String error = "";

        try {

            if (content.contains("<diag:message>")) {

                error = content.substring(content.indexOf("<diag:message>") + 14,
                        content.indexOf("</diag:message>", content.indexOf("<diag:message>")));

            }

        } catch (final Exception e) {
            LOG.error("getSruErrorCode(String content): " + e.getMessage() + "\012" + content);
        }

        return error;
    }

    /**
     * Holt den GBV-Webcontent anhand einer Phrasen-Suche ("Wort 1 Wort2...")
     * über die SRU-Schnittstelle.
     * 
     * @param String gbvfield
     * @param String gbvsearchterm
     * @param String start_record
     * @return String content
     */
    private String getGbvSrucontentSearchAsPhrase(final String gbvfield, final String gbvsearchterm,
            final int start_record) {
        final Http http = new Http();
        final CodeUrl codeUrl = new CodeUrl();
        final SpecialCharacters specialCharacters = new SpecialCharacters();

        // http://sru.gbv.de/gvk?version=1.1&operation=searchRetrieve&query=pica.
        // http://gso.gbv.de/sru/DB=2.1/?version=1.1&operation=searchRetrieve&query=pica.
        final String link = "http://sru.gbv.de/gvk?version=1.1&operation=searchRetrieve&query=pica."
                + gbvfield.toLowerCase() + "%3D%22" + codeUrl.encode(gbvsearchterm, "UTF-8")
                + "%22&recordSchema=picaxml&sortKeys=YOP%2Cpica%2C0%2C%2C&maximumRecords=10&startRecord="
                + start_record;

        return specialCharacters.replace(convertStringFromLatin1ToUTF8(http.getContent(link,
                Connect.TIMEOUT_4.getValue(), Connect.TRIES_2.getValue(), null)));

    }

    /**
     * Holt den GBV-Webcontent anhand einer Stichwort-Suche (Wort 2 Wort1...)
     * über die SRU-Schnittstelle.
     * 
     * @param String gbvfield
     * @param String gbvsearchterm
     * @param String start_record
     * @return String content
     */
    private String getGbvSrucontentSearch(final String gbvfield, final String gbvsearchterm, final int start_record) {
        final Http http = new Http();
        final CodeUrl codeUrl = new CodeUrl();
        final SpecialCharacters specialCharacters = new SpecialCharacters();

        final String link = "http://sru.gbv.de/gvk?version=1.1&operation=searchRetrieve&query=pica."
                + gbvfield.toLowerCase() + "%3D" + codeUrl.encode(gbvsearchterm, "UTF-8")
                + "&recordSchema=picaxml&sortKeys=YOP%2Cpica%2C0%2C%2C&maximumRecords=10&startRecord=" + start_record;

        return specialCharacters.replace(convertStringFromLatin1ToUTF8(http.getContent(link,
                Connect.TIMEOUT_4.getValue(), Connect.TRIES_2.getValue(), null)));

    }

    /**
     * löscht alle @-Zeichen eines Strings
     */
    private static String deleteAT(final String input) {
        String output = input;

        final Check ck = new Check();

        if (ck.isMinLength(input, 1)) {

            output = output.replaceAll("@", "");

        }

        return output;
    }

    /**
     * löst den codierten Medientyp aus dem SRU-Result des GBV auf
     */
    private static String resolveGbvMediatype(String input) {
        String output = input;

        final Check ck = new Check();

        if (ck.isMinLength(input, 3)) { // Typ kann bis 6-Stellen haben, 3 sind obligatorisch

            if (input.length() > 3) {
                input = input.substring(0, 3);
            }

            final String one = input.substring(0, 1);
            final String two = input.substring(1, 2);
            final String three = input.substring(2);

            if ("A".equals(one)) {
                output = decodePositionTwo(two);
                final String status = decodePositionThree(three);
                if (!"".equals(status)) {
                    output = output + "\040-\040" + status;
                }
            } else {
                output = decodePositionOne(one);
                final String status = decodePositionThree(three);
                if (!"".equals(status)) {
                    output = output + "\040-\040" + status;
                }
            }

        }

        return output;
    }

    /**
     * decodiert Position 1
     */
    private static String decodePositionOne(final String input) {
        String output = "";

        //    Position 1 (physikalische Form)
        //    Bei Buchmaterialien ist Position 1 von 0500 immer A. Insgesamt sind folgende Codierungen vorgesehen:
        //
        //    A Druckschrift
        //    B Audiovisuelles Material (Bildtonträger, auch Stummfilme; Tonbildreihen)
        //    C Blindenschriftträger
        //    E Mikroform
        //    G Tonträger
        //    H Handschriftliches Material
        //    K Kartenmaterial
        //    M Noten
        //    O Elektronische Ressource im Fernzugriff
        //    S Elektronische Ressource auf Datenträger
        //    V Objekt (z. B. Spiele, Skulpturen, Gemälde)
        //    Z Materialkombination

        if ("A".equals(input)) {
            output = "Druckschrift";
        }
        if ("B".equals(input)) {
            output = "Multimedia";
        }
        if ("C".equals(input)) {
            output = "Blindenschrift";
        }
        if ("E".equals(input)) {
            output = "Mikroform";
        }
        if ("G".equals(input)) {
            output = "Tonträger";
        }
        if ("H".equals(input)) {
            output = "Handschrift";
        }
        if ("K".equals(input)) {
            output = "Kartenmaterial";
        }
        if ("O".equals(input)) {
            output = "Elektronische Ressource Online";
        }
        if ("S".equals(input)) {
            output = "Elektronische Ressource auf Datenträger";
        }
        if ("V".equals(input)) {
            output = "Objekt (Spiele, Skulpturen, Gemälde etc,)";
        }
        if ("Z".equals(input)) {
            output = "Materialkombination";
        }

        return output;
    }

    /**
     * decodiert Position 2
     */
    private static String decodePositionTwo(final String input) {
        String output = "";

        //    Position 2 (bibliographische Erscheinungsform)
        //    Nicht alle Codierungen kommen auch bei allen Materialarten vor;
        //    auf eine detaillierte Zuordnung wird hier verzichtet. Folgende Codierungen sind vorgesehen:
        //
        //    a Monographie (unabhängig ob Stück einer Reihe)
        //    b Zeitschrift/Zeitung
        //    c Gesamtaufnahme eines mehrbändigen begrenzten Werkes
        //    d Schriftenreihe
        //    e Abteilung (unselbstständig) Entfällt zukünftig
        //    E Abteilung (selbstständig) Nur aus Fremddaten
        //    f Teil eines mehrbändigen begrenzten Werkes ohne bzw. mit nicht zitierfähigem Stücktitel
        //    F Teil eines mehrbändigen begrenzten Werkes mit zitierfähigem Stücktitel
        //    h Abteilung (Zeitschriften und Reihen) Nur aus Fremddaten
        //    j Enthaltenes/beigefügtes Werk
        //    o Unselbstständiges Werk (nur OLC) Nicht mehr verwendet
        //    s Unselbstständiges Werk (Aufsatz, Rezension)
        //    v Bandsatz bei Zeitschriften/zeitschriftenartigen Reihen
        //    z Keine Angabe

        if ("a".equals(input)) {
            output = "Buch";
        }
        if ("b".equals(input)) {
            output = "Zeitschrift (Print)";
        }
        if ("c".equals(input)) {
            output = "mehrbändiges Gesamtwerk";
        }
        if ("d".equals(input)) {
            output = "Schriftenreihe";
        }
        if ("e".equals(input)) {
            output = "Abteilung";
        } // ?
        if ("E".equals(input)) {
            output = "Abteilung";
        } // ?
        if ("f".equals(input)) {
            output = "Band eines Gesamtwerkes";
        }
        if ("F".equals(input)) {
            output = "Band eines Gesamtwerkes";
        }
        if ("h".equals(input)) {
            output = "Aufsatz";
        }
        if ("j".equals(input)) {
            output = "Enthaltenes/beigefügtes Werk";
        }
        if ("o".equals(input)) {
            output = "unselbstständiges Werk";
        } // ?
        if ("s".equals(input)) {
            output = "Aufsatz";
        }
        if ("v".equals(input)) {
            output = "Zeitschrift gebunden";
        }
        if ("z".equals(input)) {
            output = "keine Angabe";
        }

        return output;
    }

    /**
     * decodiert Position 3
     */
    private static String decodePositionThree(final String input) {
        String output = "";

        //    Position 3 (Status der Beschreibung)
        //    Nicht alle Codierungen kommen auch bei allen Materialarten vor;
        //    auf eine detaillierte Zuordnung wird hier verzichtet. Folgende Codierungen sind vorgesehen:
        //
        //    u Autopsie
        //    a Erwerbungsdatensatz
        //    v Bibliographisch vollständig (Satzsperre)
        //    c CIP-Aufnahme
        //    k Lösch-Status (zur Nutzung gesperrt)
        //    n Konvertierte Altdaten
        //    r Retrospektives Katalogisat
        //    x Fremddatum
        //    y Vorläufigkeitsstatus
        //    B Offline eingespieltes Novum, wahrscheinlich dublett
        //    N Zunächst verdeckt eingespieltes Novum
        //    X Inhalt oder Struktur ist zu überprüfen

        if ("k".equals(input)) {
            output = "keine Ausleihe/Kopiebestellung";
        }
        if ("y".equals(input)) {
            output = "Vorläufigkeitsstatus";
        }
        // scheint nicht immer gesperrt zu sein...
        //      if (input.equals("B")) { output = "keine Ausleihe/Kopiebestellung"; }

        return output;
    }

    private String convertStringFromLatin1ToUTF8(final String stringForconversion) {
        try {
            final String stringToConvert = stringForconversion;
            final byte[] stringToByte = stringToConvert.getBytes("ISO-8859-1");
            return new String(stringToByte, "UTF-8");
        } catch (final UnsupportedEncodingException e) {
            LOG.error("convertStringFromLatintoUTF8: " + stringForconversion + "\040" + e.getMessage());
            return stringForconversion;
        }
    }

    /**
     * Wandelt eine e-ZDB-ID in eine p-ZDB-ID um. Optimiert für die Abfrage von
     * ausserhalb OrderGbvAction.
     */
    protected String getPrintZdbidIgnoreMultipleHits(final String content) {
        String pZdbid = null;

        try {
            final List<GbvSruForm> matches = getGbvMatches(content);

            if (!matches.isEmpty()) { // nur falls nicht keine Treffer! Sonst kracht es...
                final GbvSruForm gsf = matches.get(0);
                // bringt aus ggf. mehreren Umleitungen die letztmögliche
                if (gbvIsEjournalRedirectableIgnoreMultipleHits(gsf)) {
                    pZdbid = gsf.getVerknuepfung_zdbid_horizontal();
                }
            }

        } catch (final Exception e) {
            LOG.error("getPrintZdbid(String zdbid): " + e.toString() + "\012" + content);
        }

        return pZdbid;
    }

    /**
     * Prüft, ob alle Mussangaben für eine GBV-Bestellung gemacht wurden,
     * abhängig vom gewählten Medientyp
     * 
     * @param OrderForm of
     * @return boolean check
     */
    private boolean gbvOrderValues(final OrderForm of) {

        boolean check = false;
        final Check ck = new Check();

        // falls manuelle Bestellung gelten weniger scharfe Mussfelder
        if (of.isManuell()
                && ((of.getMediatype().equals("Artikel") && ck.isMinLength(of.getIssn(), 8) || ck.isMinLength(
                        of.getZeitschriftentitel(), 1))
                        || (of.getMediatype().equals("Teilkopie Buch") && ck.isMinLength(of.getIsbn(), 8) || ck
                                .isMinLength(of.getBuchtitel(), 1)) || (of.getMediatype().equals("Buch")
                        && ck.isMinLength(of.getIsbn(), 8) || ck.isMinLength(of.getBuchtitel(), 1)))) {
            check = true;
            return check;
        }

        // Check bei Medientyp Artikel
        if (!of.isManuell() && of.getMediatype().equals("Artikel") && !of.getForuser().equals("0")
                && ck.containsOnlyNumbers(of.getJahr()) && ck.isExactLength(of.getJahr(), 4)
                && (ck.isMinLength(of.getJahrgang(), 1) || ck.isMinLength(of.getHeft(), 1))
                && ck.isMinLength(of.getSeiten(), 1)
                // Entweder Angabe einer ISSN oder eines Zeitschriftentitels
                && (ck.isMinLength(of.getIssn(), 8) || ck.isMinLength(of.getZeitschriftentitel(), 1))) {
            check = true;
            return check;
        }
        // Check bei Medientyp Teilkopie Buch
        if (!of.isManuell() && of.getMediatype().equals("Teilkopie Buch") && !of.getForuser().equals("0")
                // Entweder Angabe einer ISBN oder eines Buchtitels
                && (ck.isMinLength(of.getIsbn(), 4) || ck.isMinLength(of.getBuchtitel(), 1))
                && ck.isMinLength(of.getSeiten(), 1)) {
            check = true;
            return check;
        }
        // Check bei Medientyp Buch
        if (!of.isManuell() && of.getMediatype().equals("Buch") && !of.getForuser().equals("0")
        // Entweder Angabe einer ISBN oder eines Buchtitels
                && (ck.isMinLength(of.getIsbn(), 4) || ck.isMinLength(of.getBuchtitel(), 1))) {
            check = true;
            return check;
        }

        return check;
    }

    /**
     * Prüft, ob es sich um ein Print- oder E-Journal handelt und ob,
     * ZDB-ID-Angaben über eine Paralleldruckausgabe vorliegen.
     * 
     * @param GbvSruForm srf
     * @return boolean check
     */
    private boolean gbvIsEjournalRedirectable(final GbvSruForm gsf) {

        boolean check = false;

        if (gsf.getVerknuepfung_horizontal_039D_multipel() != null
                && gsf.getAnzahl_039D() < 2
                && (gsf.getVerknuepfung_horizontal_039D_multipel().contains("Druckausg.") || (gsf
                        .getMaterialbenennung_spezifisch_034D() != null && gsf.getMaterialbenennung_spezifisch_034D()
                        .contains("Online-Ressource"))) && gsf.getVerknuepfung_zdbid_horizontal() != null
                && !gsf.getVerknuepfung_zdbid_horizontal().equals("")) {

            check = true;

        }

        return check;
    }

    /**
     * Optimiert für Checkavailability Prüft, ob es sich um ein Print- oder
     * E-Journal handelt und ob, ZDB-ID-Angaben über eine Paralleldruckausgabe
     * vorliegen.
     * 
     * @param GbvSruForm srf
     * @return boolean check
     */
    private static boolean gbvIsEjournalRedirectableIgnoreMultipleHits(final GbvSruForm gsf) {

        boolean check = false;

        if (gsf.getVerknuepfung_horizontal_039D_multipel() != null
                && (gsf.getVerknuepfung_horizontal_039D_multipel().contains("Druckausg.") || (gsf
                        .getMaterialbenennung_spezifisch_034D() != null && gsf.getMaterialbenennung_spezifisch_034D()
                        .contains("Online-Ressource"))) && gsf.getVerknuepfung_zdbid_horizontal() != null
                && !gsf.getVerknuepfung_zdbid_horizontal().equals("")) {

            check = true;

        }

        return check;
    }

    /**
     * Prüft, ob es sich um ein Print- oder E-Journal handelt
     * 
     * @param GbvSruForm srf
     * @return boolean check
     */
    private boolean gbvIsEjournal(final GbvSruForm gsf) {

        boolean check = false;

        if ((gsf.getVerknuepfung_horizontal_039D_multipel() != null && gsf.getVerknuepfung_horizontal_039D_multipel()
                .contains("Druckausg."))
                || (gsf.getMaterialbenennung_spezifisch_034D() != null && gsf.getMaterialbenennung_spezifisch_034D()
                        .contains("Online-Ressource"))) {

            check = true;

        }

        return check;
    }

    /**
     * Prüft, ob es sich bei der GBV-Antwort um den Status OK handelt und ob
     * eine Bestellnummer vorliegt
     * 
     * @param String gbvanswer
     * @return boolean check
     */
    private boolean gbvIsOrdernumber(String gbvanswer) {

        boolean check = false;

        if (gbvanswer != null && gbvanswer.contains("<status>OK</status>") && gbvanswer.contains("<return_value>")) {

            try {

                gbvanswer = gbvanswer.substring(gbvanswer.indexOf("<return_value>") + 14,
                        gbvanswer.indexOf("</return_value>", gbvanswer.indexOf("<return_value>")));

                final Pattern p = Pattern.compile("A[0-9]*");
                final Matcher m = p.matcher(gbvanswer);
                if (m.find()) {
                    check = true;
                }
            } catch (final Exception e) {
                LOG.error("Check - gbvIsOrdernumber): " + e.toString() + "\012" + gbvanswer);
            }
        }

        return check;
    }

}
