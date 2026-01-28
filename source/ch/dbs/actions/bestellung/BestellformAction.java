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

import java.sql.Connection;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.mail.internet.InternetAddress;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.actions.DispatchAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import util.Auth;
import util.Base64String;
import util.Check;
import util.CodeUrl;
import util.MHelper;
import util.ReadSystemConfigurations;
import util.ThreadSafeSimpleDateFormat;
import ch.dbs.actions.openurl.ConvertOpenUrl;
import ch.dbs.entity.AbstractBenutzer;
import ch.dbs.entity.BestellFormParam;
import ch.dbs.entity.Bestellungen;
import ch.dbs.entity.Countries;
import ch.dbs.entity.DaiaParam;
import ch.dbs.entity.Konto;
import ch.dbs.entity.OrderState;
import ch.dbs.entity.Text;
import ch.dbs.entity.VKontoBenutzer;
import ch.dbs.form.ActiveMenusForm;
import ch.dbs.form.ErrorMessage;
import ch.dbs.form.Message;
import ch.dbs.form.OrderForm;
import ch.dbs.form.UserInfo;
import enums.BestellformNumber;
import enums.Result;
import enums.TextType;
import enums.XPrio;

/**
 * BestellformAction prüft ip-basierte Zugriffe und erlaubt Kundenbestellungen
 * innerhalb einer Institution z.Hd. der betreffenden Bibliothek
 * 
 * @author Markus Fischer
 */
public final class BestellformAction extends DispatchAction {

    private static final Logger LOG = LoggerFactory.getLogger(BestellformAction.class);

    /**
     * Prüft IP und ordnet den Request der betreffenden Bibliothek zu, ergänzt
     * Angaben anhand PMID und DOI
     */
    public ActionForward validate(final ActionMapping mp, final ActionForm fm, final HttpServletRequest rq,
            final HttpServletResponse rp) {

        Text t = new Text();
        final Text cn = new Text();
        final Auth auth = new Auth();
        String forward = Result.FAILURE.getValue();
        OrderForm of = (OrderForm) fm;
        BestellFormParam bp = new BestellFormParam();
        final Countries country = new Countries();
        final DOI doi = new DOI();
        final Pubmed pubmed = new Pubmed();

        try {

            if (rq.getAttribute("ofjo") != null) {
                of = (OrderForm) rq.getAttribute("ofjo"); // if coming from checkAvailability and getOpenUrlRequest
            }

            // There are three ways of taking access, without being logged in. Priority is as follows:
            // 1. Kontokennung (overwrites IP based access)
            // 2. IP based (overwrites Broker-Kennung)
            // 3. Broker-Kennung (e.g. Careum Explorer)

            if (of.getKkid() == null) {
                t = auth.grantAccess(rq);
            }

            // Not logged in: IP based, Kontokennung or Brokerkennung
            if (((t != null && t.getInhalt() != null) || (of.getKkid() != null || of.getBkid() != null))
                    && !auth.isLogin(rq)) {
                forward = Result.SUCCESS.getValue();

                final String kkid = of.getKkid(); // separate variables to avoid that kkid gets overwritten in resolvePmid
                final String bkid = of.getBkid();

                if (of.getMediatype() == null || // default orderform 'Article'
                        (!"Artikel".equals(of.getMediatype()) && !"Teilkopie Buch".equals(of.getMediatype()) && !"Buch"
                                .equals(of.getMediatype()))) {
                    of.setMediatype("Artikel");
                }

                // resolve PMID or DOI
                if (of.isResolve() && of.getPmid() != null && !of.getPmid().equals("") && of.areArticleValuesMissing()) {
                    of = pubmed.resolvePmid(pubmed.extractPmid(of.getPmid()));
                } else {
                    if (of.isResolve() && of.getDoi() != null && !of.getDoi().equals("")
                            && of.areArticleValuesMissing()) {
                        of = doi.resolveDoi(doi.extractDoi(of.getDoi()));
                        if (of.getDoi() == null || of.getDoi().equals("")) {
                            of = (OrderForm) fm;
                        } // sometimes we can't resolve a DOI...
                    }
                }

                // has to be placed after resolvePmid, to avoid overwriting of library name (Bibliotheksname)...
                if (t != null && t.getInhalt() != null) {
                    rq.setAttribute("ip", t);
                    of.setBibliothek(t.getKonto().getBibliotheksname());
                    if (t.getTexttype().getValue() == TextType.ACCOUNT_ID_OVERRIDDEN_BY_IP.getValue()) {
                        of.setBkid(t.getInhalt());
                    }
                    if (t.getTexttype().getValue() == TextType.ACCOUNT_ID_OVERRIDES_IP.getValue()) {
                        of.setKkid(t.getInhalt());
                    }
                } else {
                    if (kkid != null) { // Kontokennung
                        t = new Text(cn.getConnection(), TextType.ACCOUNT_ID_OVERRIDES_IP, kkid); // Text with Kontokennung
                        if (t != null && t.getInhalt() != null) { // makes sure the kkid entered is valid!
                            rq.setAttribute("ip", t);
                            of.setBibliothek(t.getKonto().getBibliotheksname());
                            of.setKkid(kkid);
                        } else { // invalid kkid
                            forward = Result.FAILURE.getValue();
                            final ErrorMessage em = new ErrorMessage("error.kkid", "login.do");
                            rq.setAttribute(Result.ERRORMESSAGE.getValue(), em);
                            final ActiveMenusForm mf = new ActiveMenusForm();
                            mf.setActivemenu("bestellform");
                            rq.setAttribute(Result.ACTIVEMENUS.getValue(), mf);
                        }
                    }
                    if (bkid != null) { // Brokerkennung
                        t = new Text(cn.getConnection(), TextType.ACCOUNT_ID_OVERRIDDEN_BY_IP, bkid); // Text with Brokerkennung
                        if (t != null && t.getInhalt() != null) { // makes sure the bkid entered is valid!
                            if (t.getKonto().getId() != null) { // Brokerkennung belongs to ONE account
                                rq.setAttribute("ip", t);
                                of.setBibliothek(t.getKonto().getBibliotheksname());
                                of.setBkid(bkid);
                            }
                        } else { // invalid bkid
                            forward = Result.FAILURE.getValue();
                            final ErrorMessage em = new ErrorMessage("error.bkid", "login.do");
                            rq.setAttribute(Result.ERRORMESSAGE.getValue(), em);
                            final ActiveMenusForm mf = new ActiveMenusForm();
                            mf.setActivemenu("bestellform");
                            rq.setAttribute(Result.ACTIVEMENUS.getValue(), mf);
                        }
                    }
                }

                // get additional orderform parameters (BestellFormParam bp)
                // Changes in this section have to be repeated in save()!
                if (t != null && t.getInhalt() != null) {
                    bp = new BestellFormParam(t, cn.getConnection());
                    // Länderauswahl setzen
                    final List<Countries> allPossCountries = country.getAllCountries(cn.getConnection());
                    of.setCountries(allPossCountries);
                    if (of.getRadiobutton().equals("")) {
                        of.setRadiobutton(bp.getOption_value1());
                    } // default Option1

                    // get user categories for drop down menu
                    if (bp.isCategory()) {
                        final List<Text> categories = cn.getAllKontoText(TextType.USER_CATEGORY, t.getKonto().getId(),
                                cn.getConnection());
                        // only set into request, if we have at least one category
                        rq.setAttribute("categories", categories);
                    }

                    // set link in request if there is institution logo for this account
                    if (t.getKonto().getInstlogolink() != null) {
                        rq.setAttribute("logolink", t.getKonto().getInstlogolink());
                    }
                }

                if (of.getDeloptions() == null || of.getDeloptions().equals("")) { // default value deloptions
                    if (!bp.isLieferart()) {
                        of.setDeloptions("email");
                    } else {
                        of.setDeloptions(bp.getLieferart_value1());
                    }
                }

                // read Cookie
                final Cookie[] cookies = rq.getCookies();
                if (cookies != null) {
                    final Base64String base64String = new Base64String();
                    final int max = cookies.length;
                    for (int i = 0; i < max; i++) {
                        if (cookies[i].getName() != null && cookies[i].getName().equals("doctordoc-bestellform")) {
                            String cookietext = base64String.decodeString(cookies[i].getValue());
                            if (cookietext != null && cookietext.contains("---")) {
                                try {
                                    of.setKundenvorname(cookietext.substring(0, cookietext.indexOf("---")));
                                    cookietext = cookietext.substring(cookietext.indexOf("---") + 3);
                                    of.setKundenname(cookietext.substring(0, cookietext.indexOf("---")));
                                    cookietext = cookietext.substring(cookietext.indexOf("---") + 3);
                                    of.setKundenmail(cookietext.substring(0, cookietext.indexOf("---")));
                                    cookietext = cookietext.substring(cookietext.indexOf("---") + 3);
                                    of.setKundenkategorieID(cookietext);
                                } catch (final Exception e) { //
                                    LOG.error("Error while reading cookie!: " + e.toString());
                                }
                            }
                        }
                    }
                }

                final ActiveMenusForm mf = new ActiveMenusForm();
                mf.setActivemenu("bestellform");
                rq.setAttribute(Result.ACTIVEMENUS.getValue(), mf);
                rq.setAttribute("bestellformparam", bp);
                rq.setAttribute("orderform", of);
            } else {

                // Case User is logged in
                if (auth.isLogin(rq)) {

                    forward = Result.SUCCESS.getValue();
                    final UserInfo ui = (UserInfo) rq.getSession().getAttribute("userinfo");

                    if (of.getMediatype() == null || // default orderform 'Artikel'
                            (!of.getMediatype().equals("Artikel") && !of.getMediatype().equals("Teilkopie Buch") && !of
                                    .getMediatype().equals("Buch"))) {
                        of.setMediatype("Artikel");
                    }

                    // resolve PMID or DOI
                    if (!of.isResolver() && of.getPmid() != null && !of.getPmid().equals("")
                            && of.areArticleValuesMissing()) {
                        of = pubmed.resolvePmid(pubmed.extractPmid(of.getPmid()));
                    } else {
                        if (!of.isResolver() && of.getDoi() != null && !of.getDoi().equals("")
                                && of.areArticleValuesMissing()) {
                            of = doi.resolveDoi(doi.extractDoi(of.getDoi()));
                            if (of.getDoi() == null || of.getDoi().equals("")) {
                                of = (OrderForm) fm;
                            } // sometimes we can't resolve a DOI...
                        }
                    }

                    // special case BestellFormParam when logged in
                    bp = new BestellFormParam(ui.getKonto(), cn.getConnection());

                    // set country select
                    if (bp != null && bp.getId() != null) {
                        final List<Countries> allPossCountries = country.getAllCountries(cn.getConnection());
                        of.setCountries(allPossCountries);
                        if (of.getRadiobutton().equals("")) {
                            of.setRadiobutton(bp.getOption_value1());
                        } // default Option1

                        // get user categories for drop down menu
                        if (bp.isCategory()) {
                            final List<Text> categories = cn.getAllKontoText(TextType.USER_CATEGORY, ui.getKonto()
                                    .getId(), cn.getConnection());
                            // only set into request, if we have at least one category
                            rq.setAttribute("categories", categories);
                        }

                        // values for customizable orderform
                        of.setKundeninstitution(ui.getBenutzer().getInstitut());
                        of.setKundenabteilung(ui.getBenutzer().getAbteilung());
                        if (ui.getBenutzer().getCategory() != null && ui.getBenutzer().getCategory().getId() != null) {
                            of.setKundenkategorieID(ui.getBenutzer().getCategory().getId().toString());
                        }
                        of.setKundenadresse(ui.getBenutzer().getAdresse() + "\012" + ui.getBenutzer().getAdresszusatz()
                                + "\012" + ui.getBenutzer().getPlz() + "\040" + ui.getBenutzer().getOrt());
                        of.setKundenstrasse(ui.getBenutzer().getAdresse() + "\040" + ui.getBenutzer().getAdresszusatz());
                        of.setKundenplz(ui.getBenutzer().getPlz());
                        of.setKundenort(ui.getBenutzer().getOrt());
                        of.setKundenland(ui.getBenutzer().getLand());
                        if (ui.getBenutzer().getTelefonnrg() != null && !ui.getBenutzer().getTelefonnrg().equals("")) {
                            of.setKundentelefon(ui.getBenutzer().getTelefonnrg());
                        } else {
                            if (ui.getBenutzer().getTelefonnrp() != null
                                    && !ui.getBenutzer().getTelefonnrp().equals("")) {
                                of.setKundentelefon(ui.getBenutzer().getTelefonnrp());
                            }
                        }
                    }

                    if (of.getDeloptions() == null || of.getDeloptions().equals("")) { // default values all other
                        // situations of deloptions
                        if (!bp.isLieferart()) {
                            of.setDeloptions("email");
                        } else {
                            of.setDeloptions(bp.getLieferart_value1());
                        }
                    }

                    // of.setBibliothek(t.getKonto().getBibliotheksname());
                    of.setKundenvorname(ui.getBenutzer().getVorname());
                    of.setKundenname(ui.getBenutzer().getName());
                    of.setKundenmail(ui.getBenutzer().getEmail());

                    rq.setAttribute("bestellformparam", bp);
                    rq.setAttribute("orderform", of);

                    final ActiveMenusForm mf = new ActiveMenusForm();
                    mf.setActivemenu("suchenbestellen");
                    rq.setAttribute(Result.ACTIVEMENUS.getValue(), mf);

                } else {
                    final ErrorMessage em = new ErrorMessage("error.ip", "login.do");
                    rq.setAttribute(Result.ERRORMESSAGE.getValue(), em);
                    final ActiveMenusForm mf = new ActiveMenusForm();
                    mf.setActivemenu("bestellform");
                    rq.setAttribute(Result.ACTIVEMENUS.getValue(), mf);
                }

            }

            // if this Bestellform is deactivated show an error message
            if (bp.isDeactivated()) {
                final ErrorMessage em = new ErrorMessage("error.deactivated", "login.do");
                rq.setAttribute(Result.ERRORMESSAGE.getValue(), em);
                final ActiveMenusForm mf = new ActiveMenusForm();
                mf.setActivemenu("bestellform");
                rq.setAttribute(Result.ACTIVEMENUS.getValue(), mf);
                //                cn.close(); // using finally for direct return => close
                return mp.findForward(Result.FAILURE.getValue());
            }

            // redirect to external order form
            if (bp.getUse_did() != null && bp.getUse_did() != 0) {
                final DaiaParam dp = new DaiaParam(bp.getUse_did(), cn.getConnection());
                // set linkout dependent on protocol
                dp.setLinkout(dp, of);
                rq.setAttribute("daiaparam", dp);
                // redirect to linkout directly
                if (dp.isRedirect()) {
                    forward = "redirect";
                }
            }

        } finally {
            cn.close();
        }

        return mp.findForward(forward);
    }

    /**
     * Prüft Angaben und schickt Email mit Bestellangaben an Bibliothek und an
     * User
     */
    public ActionForward sendOrder(final ActionMapping mp, final ActionForm fm, final HttpServletRequest rq,
            final HttpServletResponse rp) {

        final Auth auth = new Auth();

        Text t = new Text();
        final Text cn = new Text();
        String forward = Result.FAILURE.getValue();
        final OrderForm of = (OrderForm) fm;
        BestellFormParam bp = new BestellFormParam();
        final Countries country = new Countries();
        final ConvertOpenUrl openurlConv = new ConvertOpenUrl();
        final DOI doi = new DOI();
        final Pubmed pubmed = new Pubmed();

        final UserInfo ui = (UserInfo) rq.getSession().getAttribute("userinfo");
        Konto konto = new Konto();
        String library = "";
        boolean saveOrder = false;

        try {

            // There are four ways of taking access. Priority is as follows:
            // 1. Being logged in
            // Not being logged in:
            // 2. Kontokennung (overwrites IP based access)
            // 3. IP based (overwrites Brokerkennung)
            // 4. Brokerkennung

            if (of.getKkid() == null && !auth.isLogin(rq)) {
                t = auth.grantAccess(rq);
            }

            // Logged in. IP based, Kontokennung or Brokerkennung
            if (((t != null && t.getInhalt() != null) || (of.getKkid() != null || of.getBkid() != null))
                    || auth.isLogin(rq)) {

                if (t == null || t.getInhalt() == null) {
                    if (of.getKkid() != null) { // Kontokennung
                        t = new Text(cn.getConnection(), TextType.ACCOUNT_ID_OVERRIDES_IP, of.getKkid()); // Text with Kontokennung
                    }
                    if (of.getBkid() != null) { // Brokerkennung
                        t = new Text(cn.getConnection(), TextType.ACCOUNT_ID_OVERRIDDEN_BY_IP, of.getBkid()); // Text with Brokerkennung
                    }
                }

                // case Konto/Brokerkennung or IP based
                if (t != null && t.getInhalt() != null) {
                    // if activated on system level, access will be restricted to paid only
                    if (auth.isPaidOnly(t.getKonto())) {
                        return mp.findForward(Result.ERROR_PAID_ONLY.getValue());
                    }
                    library = t.getKonto().getBibliotheksname();
                    konto = t.getKonto();
                    // set link in request if there is institution logo for this account
                    if (t.getKonto().getInstlogolink() != null) {
                        rq.setAttribute("logolink", t.getKonto().getInstlogolink());
                    }
                } else { // case logged in
                    // if activated on system level, access will be restricted to paid only
                    if (auth.isPaidOnly(rq)) {
                        return mp.findForward(Result.ERROR_PAID_ONLY.getValue());
                    }
                }

                // get an eventual BestellFormParam
                if (!auth.isLogin(rq) && t != null && t.getInhalt() != null) {
                    bp = new BestellFormParam(t, cn.getConnection());
                    // Länderauswahl setzen
                    final List<Countries> allPossCountries = country.getAllCountries(cn.getConnection());
                    of.setCountries(allPossCountries);
                } else {
                    if (auth.isLogin(rq)) {
                        konto = ui.getKonto();
                        bp = new BestellFormParam(konto, cn.getConnection());
                        // Länderauswahl setzen
                        final List<Countries> allPossCountries = country.getAllCountries(cn.getConnection());
                        of.setCountries(allPossCountries);
                    }
                }

                if (bp != null && bp.getId() != null) {
                    saveOrder = bp.isSaveorder();
                } // additionally save order in the database?

                final StringBuffer m = new StringBuffer();

                try {
                    // remove empty spaces from email
                    if (of.getKundenmail() != null) {
                        of.setKundenmail(of.getKundenmail().trim());
                    }
                    final Message message = checkValues4BestellFormParam(of, bp);
                    if (message.getMessage() == null) {

                        of.setKundenmail(extractEmail(of.getKundenmail())); // remove invalid characters
                        final Base64String base64String = new Base64String();
                        // Cookie Base64 encoded for better privacy
                        final Cookie cookie = new Cookie("doctordoc-bestellform", base64String.encodeString(of
                                .getKundenvorname()
                                + "---"
                                + of.getKundenname()
                                + "---"
                                + of.getKundenmail()
                                + "---"
                                + of.getKundenkategorieID()));
                        cookie.setMaxAge(-1); // only valid for session
                        cookie.setVersion(1);
                        try {
                            // if there were invalid not-ASCI-characters, the order will still be processed and no cookie set.
                            rp.addCookie(cookie);
                        } catch (final Exception e) {
                            LOG.error("Setting Cookie: " + e.toString());
                        }

                        AbstractBenutzer u = new AbstractBenutzer();

                        if (auth.isLogin(rq)) { // User is already known
                            u = ui.getBenutzer();
                            // if registered email is not the same as specified in the orderform
                            // append to remarks
                            if (!u.getEmail().equals(of.getKundenmail())) {
                                of.setNotizen((of.getKundenmail() + "\012" + of.getNotizen()).trim());
                            }
                        } else { // try to look up user from given Emailaddress
                            library = konto.getBibliotheksname();
                            u = getUserFromBestellformEmail(konto, of.getKundenmail(), cn.getConnection());
                        }

                        if (u.getId() != null) { // we do have already a valid user
                            of.setForuser(u.getId().toString()); // Preselection of user for saving an order through the
                            // getMethod in the email (depreceated)
                        } else if (saveOrder) {
                            // save as new user
                            final Date d = new Date();
                            final ThreadSafeSimpleDateFormat fmt = new ThreadSafeSimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                            final String datum = fmt.format(d, konto.getTimezone());
                            u = new AbstractBenutzer(of);
                            u.setDatum(datum);
                            if (u.getLand() == null || u.getLand().equals("0")) {
                                u.setLand(konto.getLand());
                            } // use same value as library, if not specified
                            u.setId(u.saveNewUser(u, konto.getTimezone(), cn.getConnection()));
                            final VKontoBenutzer vKontoBenutzer = new VKontoBenutzer();
                            vKontoBenutzer.setKontoUser(u, konto, cn.getConnection());
                            of.setForuser(u.getId().toString());
                        }

                        if (saveOrder) {
                            // save oder
                            final Bestellungen b = new Bestellungen(of, u, konto);
                            // set standard values. Fileformat isn't implemented in any possible orderform
                            if (!b.getMediatype().equals("Buch")) {
                                b.setFileformat("PDF");
                            } else {
                                b.setFileformat("Papierkopie");
                            }
                            b.setStatustext("zu bestellen");
                            b.save(cn.getConnection());

                            final Text state = new Text(cn.getConnection(), TextType.STATE_ORDER, "zu bestellen");
                            final OrderState orderstate = new OrderState();
                            orderstate.setNewOrderState(b, konto, state, null, u.getEmail(), cn.getConnection());
                        }

                        forward = Result.SUCCESS.getValue();

                        // set current date
                        final Date d = new Date();
                        final ThreadSafeSimpleDateFormat sdf = new ThreadSafeSimpleDateFormat("dd.MM.yyyy HH:mm:ss");
                        final String date = sdf.format(d, konto.getTimezone());

                        m.append("First name: ");
                        m.append(of.getKundenvorname());
                        m.append("\nLast name: ");
                        m.append(of.getKundenname());
                        m.append("\nEmail: ");
                        m.append(of.getKundenmail());
                        m.append('\n');

                        // configurable part (orderforms)
                        if (of.getFreitxt1_inhalt() != null && !of.getFreitxt1_inhalt().equals("")) {
                            m.append(of.getFreitxt1_label());
                            m.append(": ");
                            m.append(of.getFreitxt1_inhalt());
                            m.append('\n');
                        }

                        if (of.getKundeninstitution() != null && !of.getKundeninstitution().equals("")) {
                            m.append("Institution: ");
                            m.append(of.getKundeninstitution());
                            m.append('\n');
                        } else { // use information from database
                            if (u.getInstitut() != null && !u.getInstitut().equals("")) {
                                m.append("Institution: ");
                                m.append(u.getInstitut());
                                m.append('\n');
                            }
                        }

                        if (of.getKundenabteilung() != null && !of.getKundenabteilung().equals("")) {
                            m.append("Department: ");
                            m.append(of.getKundenabteilung());
                            m.append('\n');
                        } else { // use information from database
                            if (u.getAbteilung() != null && !u.getAbteilung().equals("")) {
                                m.append("Department: ");
                                m.append(u.getAbteilung());
                                m.append('\n');
                            }
                        }

                        if (of.getKundenkategorieID() != null && !"0".equals(of.getKundenkategorieID())) {
                            m.append("Category: ");
                            m.append(new Text(cn.getConnection(), Long.valueOf(of.getKundenkategorieID()),
                                    TextType.USER_CATEGORY).getInhalt());
                            m.append('\n');
                        } else { // use information from database
                            if (u.getCategory() != null && u.getCategory().getInhalt() != null) {
                                m.append("Category: ");
                                m.append(u.getCategory().getInhalt());
                                m.append('\n');
                            }
                        }

                        if (of.getFreitxt2_inhalt() != null && !of.getFreitxt2_inhalt().equals("")) {
                            m.append(of.getFreitxt2_label());
                            m.append(": ");
                            m.append(of.getFreitxt2_inhalt());
                            m.append('\n');
                        }
                        if (of.getKundenadresse() != null && !of.getKundenadresse().equals("")) {
                            m.append("Address: ");
                            m.append(of.getKundenadresse());
                            m.append('\n');
                        }
                        if (of.getKundenstrasse() != null && !of.getKundenstrasse().equals("")) {
                            m.append("Street: ");
                            m.append(of.getKundenstrasse());
                            m.append('\n');
                        }
                        if (of.getKundenplz() != null && !of.getKundenplz().equals("")) {
                            m.append("ZIP: ");
                            m.append(of.getKundenplz());
                            m.append('\n');
                        }
                        if (of.getKundenort() != null && !of.getKundenort().equals("")) {
                            m.append("Place: ");
                            m.append(of.getKundenort());
                            m.append('\n');
                        }
                        if (of.getKundenland() != null && !of.getKundenland().equals("0")) {
                            m.append("Country: ");
                            m.append(of.getKundenland());
                            m.append('\n');
                        }

                        if (of.getKundentelefon() != null && !of.getKundentelefon().equals("")) { // ggf. Angaben aus
                            // Formular
                            m.append("Phone: ");
                            m.append(of.getKundentelefon());
                            m.append('\n');
                        } else { // use information from database
                            if (u.getTelefonnrg() != null && !u.getTelefonnrg().equals("")) {
                                m.append("Phone B: ");
                                m.append(u.getTelefonnrg());
                                m.append('\n');
                            }
                            if (u.getTelefonnrp() != null && !u.getTelefonnrp().equals("")) {
                                m.append("Phone P: ");
                                m.append(u.getTelefonnrp());
                                m.append('\n');
                            }
                        }

                        if (of.getKundenbenutzernr() != null && !of.getKundenbenutzernr().equals("")) {
                            m.append("Library card #: ");
                            m.append(of.getKundenbenutzernr());
                            m.append('\n');
                        }
                        if (of.getFreitxt3_inhalt() != null && !of.getFreitxt3_inhalt().equals("")) {
                            m.append(of.getFreitxt3_label());
                            m.append(": ");
                            m.append(of.getFreitxt3_inhalt());
                            m.append('\n');
                        }
                        if (of.getRadiobutton() != null && !of.getRadiobutton().equals("")) {
                            m.append(of.getRadiobutton_name());
                            m.append(": ");
                            m.append(of.getRadiobutton());
                            m.append('\n');
                        }

                        m.append('\n');

                        if (library != null && !"".equals(library)) {
                            m.append(library);
                            m.append("\n\n");
                        }

                        if (of.getDeloptions() != null && !of.getDeloptions().equals("")) {
                            m.append("Desired deliveryway: ");
                            m.append(of.getDeloptions().toUpperCase());
                            m.append('\n');
                        }
                        if (of.getPrio() != null && of.getPrio().equals("urgent")) {
                            m.append("Priority: URGENT\n");
                        }

                        m.append("-----\n");

                        if (of.getMediatype().equals("Artikel")) {
                            if (of.getRfr_id() != null && !of.getRfr_id().equals("")) {
                                m.append("DATABASE: ");
                                m.append(of.getRfr_id());
                                m.append('\n');
                            }
                            m.append("PUBLICATION: Journal Article ");
                            m.append(of.getGenre());
                            m.append("\nAUTHOR: ");
                            m.append(of.getAuthor());
                            m.append("\nTITLE OF ARTICLE: ");
                            m.append(of.getArtikeltitel());
                            m.append("\nJOURNAL: ");
                            m.append(of.getZeitschriftentitel());
                            m.append("\nISSN: ");
                            m.append(of.getIssn());
                            m.append("\nYEAR: ");
                            m.append(of.getJahr());
                            m.append("\nVOLUME: ");
                            m.append(of.getJahrgang());
                            m.append("\nISSUE: ");
                            m.append(of.getHeft());
                            m.append("\nPAGES: ");
                            m.append(of.getSeiten());
                            m.append('\n');

                            // If there is only an ISSN and no journaltitle present...
                            if (of.getIssn() != null && !of.getIssn().equals("")
                                    && (of.getZeitschriftentitel() == null || of.getZeitschriftentitel().equals(""))) {

                                // Add a link to the EZB
                                String bibid = "AAAAA";
                                if (konto.getEzbid() != null && !konto.getEzbid().equals("")) {
                                    bibid = konto.getEzbid();
                                }
                                final String link = "http://ezb.uni-regensburg.de/ezeit/searchres.phtml?bibid="
                                        + bibid
                                        + "&colors=7&lang=de&jq_type1=KT&jq_term1=&jq_bool2=AND&jq_not2=+&jq_type2="
                                        + "KS&jq_term2=&jq_bool3=AND&jq_not3=+&jq_type3=PU&jq_term3=&jq_bool4=AND&jq_not4=+"
                                        + "&jq_type4=IS&offset=-1&hits_per_page=50&search_journal=Suche+starten&"
                                        + "Notations%5B%5D=all&selected_colors%5B%5D=1&selected_colors%5B%5D=4&jq_term4=";

                                m.append("EZB link: ");
                                m.append(link);
                                m.append(of.getIssn());
                                m.append('\n');
                            }

                            if (of.getDoi() != null && !of.getDoi().equals("")) {
                                m.append("DOI: " + of.getDoi() + '\n');
                                if (!doi.extractDoi(of.getDoi()).contains("http://")) {
                                    m.append("DOI-URI: http://dx.doi.org/");
                                    m.append(doi.extractDoi(of.getDoi()));
                                    m.append('\n');
                                } else {
                                    m.append("DOI-URI: ");
                                    m.append(doi.extractDoi(of.getDoi()));
                                    m.append('\n');
                                }
                            }
                            if (of.getPmid() != null && !of.getPmid().equals("")) {
                                m.append("PMID: ");
                                m.append(of.getPmid());
                                m.append("\nPMID-URI: http://www.ncbi.nlm.nih.gov/pubmed/");
                                m.append(pubmed.extractPmid(of.getPmid()));
                                m.append('\n');
                            }
                            m.append('\n');
                        }

                        if ("Teilkopie Buch".equals(of.getMediatype()) || "Buch".equals(of.getMediatype())) {
                            if (of.getRfr_id() != null && !of.getRfr_id().equals("")) {
                                m.append("DATABASE: ");
                                m.append(of.getRfr_id());
                                m.append('\n');
                            }
                            if (of.getMediatype().equals("Teilkopie Buch")) {
                                m.append("PUBLICATION: Book Part ");
                                m.append(of.getGenre());
                                m.append('\n');
                            } else {
                                m.append("PUBLICATION: Book ");
                                m.append(of.getGenre());
                                m.append('\n');
                            }
                            m.append("AUTHOR: ");
                            m.append(of.getAuthor());
                            m.append('\n');

                            if ("Teilkopie Buch".equals(of.getMediatype())) {
                                m.append("CHAPTER: ");
                                m.append(of.getKapitel());
                                m.append('\n');
                            }
                            m.append("TITLE OF BOOK: ");
                            m.append(of.getBuchtitel());
                            m.append("\nPUBLISHER: ");
                            m.append(of.getVerlag());
                            m.append("\nISBN: ");
                            m.append(of.getIsbn());
                            m.append('\n');
                            if (!of.getIssn().equals("")) {
                                m.append("ISSN: ");
                                m.append(of.getIssn());
                                m.append('\n');
                            } // Buchserie mit ISSN
                            m.append("YEAR: ");
                            m.append(of.getJahr());
                            m.append('\n');
                            if (!of.getJahrgang().equals("")) {
                                m.append("VOLUME: ");
                                m.append(of.getJahrgang());
                                m.append('\n');
                            } // Buchserie mit Zählung
                            if (of.getMediatype().equals("Teilkopie Buch")) {
                                m.append("PAGES: ");
                                m.append(of.getSeiten());
                                m.append('\n');
                            }

                            if (of.getDoi() != null && !of.getDoi().equals("")) {
                                m.append("DOI: " + of.getDoi() + '\n');
                                if (!doi.extractDoi(of.getDoi()).contains("http://")) {
                                    m.append("DOI-URI: http://dx.doi.org/");
                                    m.append(doi.extractDoi(of.getDoi()));
                                    m.append('\n');
                                } else {
                                    m.append("DOI-URI: ");
                                    m.append(doi.extractDoi(of.getDoi()));
                                    m.append('\n');
                                }
                            }
                            m.append('\n');

                        }

                        if (of.getNotizen() != null && !of.getNotizen().equals("")) {
                            m.append("Remarks of patron: ");
                            m.append(of.getNotizen());
                            m.append('\n');
                        }

                        m.append("-----\nOrder date: ");
                        m.append(date);
                        m.append("\nBrought to you by ");
                        m.append(ReadSystemConfigurations.getApplicationName());
                        m.append(": ");
                        m.append(ReadSystemConfigurations.getServerWelcomepage());
                        m.append('\n');

                        // Prepare a direct login link for librarians, to save order details
                        final String loginlink = ReadSystemConfigurations.getServerInstallation() + "/pl.do?"
                                + openurlConv.makeGetMethodString(of) + "&foruser=" + of.getForuser();

                        String adduserlink = "";
                        if (u.getId() == null) { // User unknown => Prepare a direct login link for librarians, to save new
                            // user
                            adduserlink = ReadSystemConfigurations.getServerInstallation() + "/add.do?"
                                    + createUrlParamsForAddUser(of);
                        }

                        String xprio = XPrio.NORMAL.getValue();
                        if (of.getPrio() != null && of.getPrio().equals("urgent")) {
                            xprio = XPrio.HIGHEST.getValue();
                        }

                        final InternetAddress[] toPatron = new InternetAddress[1];
                        toPatron[0] = new InternetAddress(of.getKundenmail()); // email of patron

                        final InternetAddress[] toLibrary = new InternetAddress[1];
                        toLibrary[0] = new InternetAddress(konto.getDbsmail()); // email of library

                        if (of.getMediatype().equals("Artikel")) {

                            final String subject = "Article: " + of.getZeitschriftentitel() + "\040" + of.getJahr()
                                    + ";" + of.getJahrgang() + "(" + of.getHeft() + "):" + of.getSeiten();

                            // send email to patron, ReplyTo library
                            final MHelper mhPatron = new MHelper(toPatron, subject, m.toString());
                            mhPatron.setReplyTo(konto.getDbsmail());
                            mhPatron.send();

                            // send email to library, ReplyTo patron
                            if (u.getId() != null) { // User already exists
                                final MHelper mhLibrary = new MHelper(toLibrary, subject, m.toString()
                                        + "\012Save order details in " + ReadSystemConfigurations.getApplicationName()
                                        + ":\012" + loginlink);
                                mhLibrary.setReplyTo(of.getKundenmail());
                                mhLibrary.setXPrio(xprio);
                                mhLibrary.send(); // send email to library

                            } else { // User unknown
                                final MHelper mhLibrary = new MHelper(toLibrary, subject, m.toString()
                                        + "\012Unknown Email! Save patron in "
                                        + ReadSystemConfigurations.getApplicationName() + ":\012" + adduserlink
                                        + "\012" + "\012Save order details in "
                                        + ReadSystemConfigurations.getApplicationName() + ":\012" + loginlink);
                                mhLibrary.setReplyTo(of.getKundenmail());
                                mhLibrary.setXPrio(xprio);
                                mhLibrary.send();
                            }
                        }

                        if (of.getMediatype().equals("Teilkopie Buch")) {

                            final String subject = "Book part: " + of.getBuchtitel() + "\040" + of.getJahr() + ":"
                                    + of.getSeiten();

                            // send email to patron, ReplyTo library
                            final MHelper mhPatron = new MHelper(toPatron, subject, m.toString());
                            mhPatron.setReplyTo(konto.getDbsmail());
                            mhPatron.send();

                            // send email to library, ReplyTo patron
                            if (u.getId() != null) { // User already exists
                                final MHelper mhLibrary = new MHelper(toLibrary, subject, m.toString());
                                mhLibrary.setReplyTo(of.getKundenmail());
                                mhLibrary.setXPrio(xprio);
                                mhLibrary.send();
                            } else { // User unknown
                                final MHelper mhLibrary = new MHelper(toLibrary, subject, m.toString()
                                        + "\012Unknown Email! Save patron in "
                                        + ReadSystemConfigurations.getApplicationName() + ":\012" + adduserlink);
                                mhLibrary.setReplyTo(of.getKundenmail());
                                mhLibrary.setXPrio(xprio);
                                mhLibrary.send();
                            }
                        }

                        if (of.getMediatype().equals("Buch")) {

                            final String subject = "Book: " + of.getBuchtitel() + "\040" + of.getJahr();

                            // send email to patron, ReplyTo library
                            final MHelper mhPatron = new MHelper(toPatron, subject, m.toString());
                            mhPatron.setReplyTo(konto.getDbsmail());
                            mhPatron.send();

                            // send email to library, ReplyTo patron
                            if (u.getId() != null) { // User already exists
                                final MHelper mhLibrary = new MHelper(toLibrary, subject, m.toString());
                                mhLibrary.setReplyTo(of.getKundenmail());
                                mhLibrary.setXPrio(xprio);
                                mhLibrary.send();
                            } else { // User unknown
                                final MHelper mhLibrary = new MHelper(toLibrary, subject, m.toString()
                                        + "\012Unknown Email! Save patron in "
                                        + ReadSystemConfigurations.getApplicationName() + ":\012" + adduserlink);
                                mhLibrary.setReplyTo(of.getKundenmail());
                                mhLibrary.setXPrio(xprio);
                                mhLibrary.send();
                            }
                        }

                        // temporary log to see who is using the system as mailing system
                        LOG.warn("Order sent by email for library: " + library + " ; " + konto.getDbsmail());

                    } else {
                        forward = "missingvalues";
                        rq.setAttribute("messagemissing", message);
                    }

                    rq.setAttribute("orderform", of);
                    if (!"".equals(library)) {
                        rq.setAttribute("library", library);
                    }

                } catch (final Exception e) {
                    forward = Result.FAILURE.getValue();
                    final ErrorMessage em = new ErrorMessage("error.send", "login.do");
                    rq.setAttribute(Result.ERRORMESSAGE.getValue(), em);
                    // Severe error
                    final MHelper mh = new MHelper(e, "Order form - Error sending an order", m.toString());
                    mh.sendError();
                }

                if (auth.isLogin(rq)) {
                    final ActiveMenusForm mf = new ActiveMenusForm();
                    mf.setActivemenu("suchenbestellen");
                    rq.setAttribute(Result.ACTIVEMENUS.getValue(), mf);
                } else {
                    final ActiveMenusForm mf = new ActiveMenusForm();
                    mf.setActivemenu("bestellform");
                    rq.setAttribute(Result.ACTIVEMENUS.getValue(), mf);
                }

            } else {
                final ErrorMessage em = new ErrorMessage("error.ip", "login.do");
                rq.setAttribute(Result.ERRORMESSAGE.getValue(), em);
                final ActiveMenusForm mf = new ActiveMenusForm();
                mf.setActivemenu("bestellform");
                rq.setAttribute(Result.ACTIVEMENUS.getValue(), mf);
            }

        } finally {
            cn.close();
        }

        return mp.findForward(forward);
    }

    /**
     * Bereitet die Bestellformular-Konfiguration vor
     */
    public ActionForward prepareConfigure(final ActionMapping mp, final ActionForm fm, final HttpServletRequest rq,
            final HttpServletResponse rp) {

        final Auth auth = new Auth();
        // make sure the user is logged in
        if (!auth.isLogin(rq)) {
            return mp.findForward(Result.ERROR_TIMEOUT.getValue());
        }
        // check access rights
        if (!auth.isBibliothekar(rq) && !auth.isAdmin(rq)) {
            return mp.findForward(Result.ERROR_MISSING_RIGHTS.getValue());
        }

        String forward = Result.FAILURE.getValue();
        final UserInfo ui = (UserInfo) rq.getSession().getAttribute("userinfo");
        final Text cn = new Text();
        final Text ip = new Text();
        BestellFormParam ipbasiert = new BestellFormParam();

        try {
            forward = Result.SUCCESS.getValue();

            final ActiveMenusForm mf = new ActiveMenusForm();
            mf.setActivemenu("konto");
            rq.setAttribute(Result.ACTIVEMENUS.getValue(), mf);

            final boolean hasIP = cn.hasIP(cn.getConnection(), ui.getKonto());

            if (hasIP) {
                ip.setId(Long.valueOf(0));
                ip.setTexttype(TextType.IP4);
                ip.setKonto(ui.getKonto());
                ipbasiert = new BestellFormParam(ip, cn.getConnection());
            }

            final BestellFormParam eingeloggt = new BestellFormParam(ui.getKonto(), cn.getConnection());

            final List<Text> kkid = cn.getAllKontoText(TextType.ACCOUNT_ID_OVERRIDES_IP, ui.getKonto().getId(),
                    cn.getConnection());
            final List<Text> bkid = cn.getAllKontoText(TextType.ACCOUNT_ID_OVERRIDDEN_BY_IP, ui.getKonto().getId(),
                    cn.getConnection());

            if (eingeloggt != null && eingeloggt.getId() != null) {
                rq.setAttribute("eingeloggt", eingeloggt.getId()); // allenfalls vorhandene BestellFormParam-ID in
                // Request
            } else {
                rq.setAttribute("eingeloggt", "0"); // 0 als ID
            }

            if (hasIP) { // IP hinterlegt
                if (ipbasiert != null && ipbasiert.getId() != null) {
                    rq.setAttribute("ipbasiert", ipbasiert.getId()); // allenfalls vorhandene BestellFormParam-ID in
                    // Request
                } else {
                    rq.setAttribute("ipbasiert", "-1"); // -1 als ID
                }
            }

            if (!kkid.isEmpty()) {
                rq.setAttribute("kkid", kkid);
            }
            if (!bkid.isEmpty()) {
                rq.setAttribute("bkid", bkid);
            }

        } catch (final Exception e) {
            LOG.error("BestellformAction - prepareConfigure: " + e.toString());
        } finally {
            cn.close();
        }

        return mp.findForward(forward);
    }

    /**
     * ändert und erstellt angepasste Bestellformulare
     */
    public ActionForward modify(final ActionMapping mp, final ActionForm fm, final HttpServletRequest rq,
            final HttpServletResponse rp) {

        final Auth auth = new Auth();
        // make sure the user is logged in
        if (!auth.isLogin(rq)) {
            return mp.findForward(Result.ERROR_TIMEOUT.getValue());
        }
        // check access rights
        if (!auth.isBibliothekar(rq) && !auth.isAdmin(rq)) {
            return mp.findForward(Result.ERROR_MISSING_RIGHTS.getValue());
        }

        String forward = Result.FAILURE.getValue();
        final UserInfo ui = (UserInfo) rq.getSession().getAttribute("userinfo");
        final Text cn = new Text();

        final BestellFormParam bp = (BestellFormParam) fm;

        try {

            if (checkPermission(ui, bp, cn.getConnection())) { // Prüfung auf URL-hacking

                try {
                    forward = Result.SUCCESS.getValue();

                    final ActiveMenusForm mf = new ActiveMenusForm();
                    mf.setActivemenu("konto");
                    rq.setAttribute(Result.ACTIVEMENUS.getValue(), mf);

                    BestellFormParam custom = new BestellFormParam();
                    custom.setKid(ui.getKonto().getId());

                    if (bp.getId() > 0) { // bestehendes BestellFormParam (eingeloggt oder IP-basiert)
                        custom = new BestellFormParam(bp.getId(), cn.getConnection());
                    }
                    if (bp.getId() == BestellformNumber.LOGGED_IN.getValue()) {
                        custom.setTyid(TextType.ORDERFORM_LOGGED_IN.getValue());
                        custom.setKennung("Bestellformular eingeloggt");
                    }
                    if (bp.getId() == BestellformNumber.IP.getValue()) {
                        custom.setTyid(TextType.IP4.getValue()); // IP
                    }
                    if (bp.getId() == BestellformNumber.KKID.getValue()) { // Konto-Kennung
                        custom = new BestellFormParam(bp.getKennung(), ui.getKonto().getId(), cn.getConnection());
                        if (custom.getId() == null) {
                            custom.setTyid(TextType.ACCOUNT_ID_OVERRIDES_IP.getValue());
                            custom.setKid(ui.getKonto().getId());
                            custom.setKennung(bp.getKennung());
                            custom.setId(bp.getId());
                        }
                    }
                    if (bp.getId() == BestellformNumber.BKID.getValue()) { // Borker-Kennung
                        custom = new BestellFormParam(bp.getKennung(), ui.getKonto().getId(), cn.getConnection());
                        if (custom.getId() == null) {
                            custom.setTyid(TextType.ACCOUNT_ID_OVERRIDDEN_BY_IP.getValue());
                            custom.setKid(ui.getKonto().getId());
                            custom.setKennung(bp.getKennung());
                            custom.setId(bp.getId());
                        }
                    }

                    rq.setAttribute("bestellform", custom);

                    // do we have a DAIA order form? It may be used as an external order form in replacement
                    // of the internal oder forms of D-D ...
                    final DaiaParam dp = new DaiaParam(ui.getKonto(), cn.getConnection());
                    if (dp.getId() != null) {
                        rq.setAttribute("daiaparam", dp);
                    }

                } catch (final Exception e) {
                    LOG.error("modify: " + e.toString());
                } finally {
                    cn.close();
                }

            } else { // URL-hacking
                final ActiveMenusForm mf = new ActiveMenusForm();
                mf.setActivemenu("suchenbestellen");
                rq.setAttribute(Result.ACTIVEMENUS.getValue(), mf);
                final ErrorMessage em = new ErrorMessage("error.hack", "searchfree.do?activemenu=suchenbestellen");
                rq.setAttribute(Result.ERRORMESSAGE.getValue(), em);
                LOG.info("modify: prevented URL-hacking! " + ui.getBenutzer().getEmail());
            }

        } finally {
            cn.close();
        }

        return mp.findForward(forward);
    }

    /**
     * speichert neue und bestehende Bestellformulare
     */
    public ActionForward save(final ActionMapping mp, final ActionForm fm, final HttpServletRequest rq,
            final HttpServletResponse rp) {

        final Auth auth = new Auth();
        // make sure the user is logged in
        if (!auth.isLogin(rq)) {
            return mp.findForward(Result.ERROR_TIMEOUT.getValue());
        }
        // check access rights
        if (!auth.isBibliothekar(rq) && !auth.isAdmin(rq)) {
            return mp.findForward(Result.ERROR_MISSING_RIGHTS.getValue());
        }

        String forward = Result.FAILURE.getValue();
        final UserInfo ui = (UserInfo) rq.getSession().getAttribute("userinfo");
        final Text cn = new Text();

        BestellFormParam bp = (BestellFormParam) fm;
        final Countries country = new Countries();

        try {

            if (checkPermission(ui, bp, cn.getConnection())) { // Prüfung auf URL-hacking

                try {
                    forward = Result.SUCCESS.getValue();

                    final ActiveMenusForm mf = new ActiveMenusForm();
                    mf.setActivemenu("konto");
                    rq.setAttribute(Result.ACTIVEMENUS.getValue(), mf);

                    bp = checkBPLogic(bp); // logische Prüfungen und setzt abhängige Werte

                    if (bp.getMessage() == null) { // keine Fehlermeldungen

                        forward = "bestellform";
                        final OrderForm of = new OrderForm();

                        if (bp.getId() <= 0) { // negative ID => save
                            bp.setId(bp.save(bp, cn.getConnection()));
                        } else { // positive ID => update
                            bp.update(bp, cn.getConnection());
                        }
                        bp.setBack(true); // Flag für "Back" auf Bestellform
                        bp.setLink_back("bfconfigure.do?method=modify&id=" + bp.getId());

                        // analog wie in validate()
                        // Länderauswahl setzen
                        final List<Countries> allPossCountries = country.getAllCountries(cn.getConnection());
                        of.setCountries(allPossCountries);
                        if (of.getRadiobutton().equals("")) {
                            of.setRadiobutton(bp.getOption_value1());
                        } // default Option1

                        if (of.getDeloptions() == null || of.getDeloptions().equals("")) { // Defaultwert deloptions
                            if (!bp.isLieferart()) {
                                of.setDeloptions("email");
                            } else {
                                of.setDeloptions(bp.getLieferart_value1());
                            }
                        }

                        // get user categories for drop down menu
                        if (bp.isCategory()) {
                            final List<Text> categories = cn.getAllKontoText(TextType.USER_CATEGORY, ui.getKonto()
                                    .getId(), cn.getConnection());
                            // only set into request, if we have at least one category
                            rq.setAttribute("categories", categories);
                        }

                        rq.setAttribute("orderform", of);
                        rq.setAttribute("bestellformparam", bp);

                    } else { // Fehlermeldung vorhanden
                        forward = Result.SUCCESS.getValue(); // auf bestellformconfigure
                        rq.setAttribute("message", bp.getMessage());
                        rq.setAttribute("bestellform", bp);
                    }

                } catch (final Exception e) {
                    LOG.error("save: " + e.toString());
                } finally {
                    cn.close();
                }

            } else { // URL-hacking
                final ActiveMenusForm mf = new ActiveMenusForm();
                mf.setActivemenu("suchenbestellen");
                rq.setAttribute(Result.ACTIVEMENUS.getValue(), mf);
                final ErrorMessage em = new ErrorMessage("error.hack", "searchfree.do?activemenu=suchenbestellen");
                rq.setAttribute(Result.ERRORMESSAGE.getValue(), em);
                LOG.info("save: prevented URL-hacking! " + ui.getBenutzer().getEmail());
            }

        } finally {
            cn.close();
        }

        return mp.findForward(forward);
    }

    /**
     * Manage an external order form.
     */
    public ActionForward prepDaiaForm(final ActionMapping mp, final ActionForm fm, final HttpServletRequest rq,
            final HttpServletResponse rp) {

        final Auth auth = new Auth();
        // make sure the user is logged in
        if (!auth.isLogin(rq)) {
            return mp.findForward(Result.ERROR_TIMEOUT.getValue());
        }
        // check access rights
        if (!auth.isBibliothekar(rq) && !auth.isAdmin(rq)) {
            return mp.findForward(Result.ERROR_MISSING_RIGHTS.getValue());
        }

        final String forward = Result.SUCCESS.getValue();
        final UserInfo ui = (UserInfo) rq.getSession().getAttribute("userinfo");
        final Text cn = new Text();

        try {

            final DaiaParam dp = new DaiaParam(ui.getKonto(), cn.getConnection());
            rq.setAttribute("daiaparam", dp);

        } finally {
            cn.close();
        }

        return mp.findForward(forward);
    }

    /**
     * Save an external order form.
     */
    public ActionForward saveDaiaForm(final ActionMapping mp, final ActionForm fm, final HttpServletRequest rq,
            final HttpServletResponse rp) {

        final Auth auth = new Auth();
        // make sure the user is logged in
        if (!auth.isLogin(rq)) {
            return mp.findForward(Result.ERROR_TIMEOUT.getValue());
        }
        // check access rights
        if (!auth.isBibliothekar(rq) && !auth.isAdmin(rq)) {
            return mp.findForward(Result.ERROR_MISSING_RIGHTS.getValue());
        }

        String forward = Result.FAILURE.getValue();
        final DaiaParam dp = (DaiaParam) fm;
        final UserInfo ui = (UserInfo) rq.getSession().getAttribute("userinfo");
        final Text cn = new Text();

        try {

            final Message message = new Message();

            // make sure base url is valid
            final Check ck = new Check();
            if (ck.isUrl(dp.getBaseurl())) {

                forward = Result.SUCCESS.getValue();

                // delete DAIA-Param for account
                dp.delete(ui.getKonto(), cn.getConnection());

                // save new DAIA-Param
                final Long newDaiaParamID = dp.save(ui.getKonto(), cn.getConnection());

                // do we need to update USE_DID in bestellform_param?
                final BestellFormParam bf = new BestellFormParam();
                final List<BestellFormParam> bfps = bf.getAllBestellFormParam(ui.getKonto(), cn.getConnection());

                for (final BestellFormParam bfp : bfps) {
                    if (bfp.getUse_did() != null && !bfp.getUse_did().equals(Long.valueOf("0"))) {
                        // set new DAIA-Param ID
                        bfp.setUse_did(newDaiaParamID);
                        bf.update(bfp, cn.getConnection());
                    }
                }

                // set success message
                message.setMessage("message.modifyuser");
                message.setLink("externalform.do?method=prepDaiaForm");
                rq.setAttribute("message", message);

            } else {
                forward = "missingvalues";
                message.setMessage("error.url");
                rq.setAttribute("message", message);
                rq.setAttribute("daiaparam", dp);
            }

        } finally {
            cn.close();
        }

        return mp.findForward(forward);
    }

    /**
     * Delete an external order form.
     */
    public ActionForward deleteDaiaForm(final ActionMapping mp, final ActionForm fm, final HttpServletRequest rq,
            final HttpServletResponse rp) {

        final Auth auth = new Auth();
        // make sure the user is logged in
        if (!auth.isLogin(rq)) {
            return mp.findForward(Result.ERROR_TIMEOUT.getValue());
        }
        // check access rights
        if (!auth.isBibliothekar(rq) && !auth.isAdmin(rq)) {
            return mp.findForward(Result.ERROR_MISSING_RIGHTS.getValue());
        }

        final String forward = Result.SUCCESS.getValue();
        final DaiaParam dp = new DaiaParam();
        final UserInfo ui = (UserInfo) rq.getSession().getAttribute("userinfo");
        final Text cn = new Text();

        try {

            // delete DAIA-Params for account
            dp.delete(ui.getKonto(), cn.getConnection());

            // do we need to update USE_DID in bestellform_param?
            final BestellFormParam bf = new BestellFormParam();
            final List<BestellFormParam> bfps = bf.getAllBestellFormParam(ui.getKonto(), cn.getConnection());

            for (final BestellFormParam bfp : bfps) {
                if (bfp.getUse_did() != null && !bfp.getUse_did().equals(Long.valueOf("0"))) {
                    // set new DAIA-Param ID
                    bfp.setUse_did(null);
                    bf.update(bfp, cn.getConnection());
                }
            }

            // set success message
            final Message message = new Message();
            message.setMessage("message.modifyuser");
            message.setLink("externalform.do?method=prepDaiaForm");
            rq.setAttribute("message", message);

        } finally {
            cn.close();
        }

        return mp.findForward(forward);
    }

    /**
     * Checks if there are required values missing and returns an error message.
     */
    private Message checkValues4BestellFormParam(final OrderForm of, final BestellFormParam bp) {

        final Message m = new Message();
        final Check ck = new Check();

        try {

            if (!ck.isMinLength(of.getKundenvorname(), 1)) {
                m.setMessage("error.vorname");
            } // auf jeden Fall Mussfeld
            if (!ck.isMinLength(of.getKundenname(), 1)) {
                m.setMessage("error.name");
            } // auf jeden Fall Mussfeld
            if (!ck.isEmail(of.getKundenmail())) {
                m.setMessage("error.mail");
            } // auf jeden Fall Mussfeld

            if (bp != null && bp.getId() != null) {
                if (bp.isInst_required() && !ck.isMinLength(of.getKundeninstitution(), 1)) {
                    m.setMessage("error.institution");
                }
                if (bp.isAbt_required() && !ck.isMinLength(of.getKundenabteilung(), 1)) {
                    m.setMessage("error.abteilung");
                }
                if (bp.isCategory_required()
                        && ("0".equals(of.getKundenkategorieID()) || !ck.isMinLength(of.getKundenkategorieID(), 1))) {
                    m.setMessage("error.category");
                }
                if (bp.isFreitxt1_required() && !ck.isMinLength(of.getFreitxt1_inhalt(), 1)) {
                    m.setMessage("error.values");
                }
                if (bp.isFreitxt2_required() && !ck.isMinLength(of.getFreitxt2_inhalt(), 1)) {
                    m.setMessage("error.values");
                }
                if (bp.isFreitxt3_required() && !ck.isMinLength(of.getFreitxt3_inhalt(), 1)) {
                    m.setMessage("error.values");
                }
                if (bp.isAdr_required() && !ck.isMinLength(of.getKundenadresse(), 1)) {
                    m.setMessage("error.adresse");
                }
                if (bp.isStr_required() && !ck.isMinLength(of.getKundenstrasse(), 1)) {
                    m.setMessage("error.strasse");
                }
                if (bp.isPlz_required() && !ck.isMinLength(of.getKundenplz(), 1)) {
                    m.setMessage("error.plz");
                }
                if (bp.isOrt_required() && !ck.isMinLength(of.getKundenort(), 1)) {
                    m.setMessage("error.ort");
                }
                if (bp.isLand_required() && !ck.isMinLength(of.getKundenland(), 2)) {
                    m.setMessage("error.land");
                }
                if (bp.isTelefon_required() && !ck.isMinLength(of.getKundentelefon(), 1)) {
                    m.setMessage("error.telefon");
                }
                if (bp.isBenutzernr_required() && !ck.isMinLength(of.getKundenbenutzernr(), 1)) {
                    m.setMessage("error.benutzernummer");
                }
                if (bp.isGebuehren() && of.getGebuehren() == null) {
                    m.setMessage("error.fees");
                } // muss "on" sein
                if (bp.isAgb() && of.getAgb() == null) {
                    m.setMessage("error.agb");
                } // muss "on" sein
            }

        } catch (final Exception e) {
            LOG.error("checkValues4BestellFormParam: " + e.toString());
        }

        return m;
    }

    /**
     * Sucht anhand der im Bestellformular eingegebenen Email den zugehörigen
     * Benutzer des betreffenden Kontos zu holen
     */
    private AbstractBenutzer getUserFromBestellformEmail(final Konto konto, final String email, final Connection cn) {

        AbstractBenutzer u = new AbstractBenutzer();

        try {

            final List<AbstractBenutzer> list = u.getUserListFromEmailAndKonto(konto, email, cn);

            if (!list.isEmpty()) {
                u = list.get(0);
            } // es wird der erste Benutzer zurückgegeben

        } catch (final Exception e) {
            LOG.error("getUserFromBestellformEmail: " + email + "\040" + e.toString());
        }

        return u;
    }

    private String createUrlParamsForAddUser(final OrderForm of) {
        final StringBuffer urlParam = new StringBuffer();
        final CodeUrl urlCoder = new CodeUrl();

        if (of.getKundenmail() != null && !"".equals(of.getKundenmail())) {
            urlParam.append("email=");
            urlParam.append(of.getKundenmail());
        }
        if (of.getKundenname() != null && !"".equals(of.getKundenname())) {
            urlParam.append("&name=");
            urlParam.append(urlCoder.encode(of.getKundenname(), "UTF-8"));
        }
        if (of.getKundenvorname() != null && !"".equals(of.getKundenvorname())) {
            urlParam.append("&vorname=");
            urlParam.append(urlCoder.encode(of.getKundenvorname(), "UTF-8"));
        }
        if (of.getKundeninstitution() != null && !"".equals(of.getKundeninstitution())) {
            urlParam.append("&institut=");
            urlParam.append(urlCoder.encode(of.getKundeninstitution(), "UTF-8"));
        }
        if (of.getKundenabteilung() != null && !"".equals(of.getKundenabteilung())) {
            urlParam.append("&abteilung=");
            urlParam.append(urlCoder.encode(of.getKundenabteilung(), "UTF-8"));
        }
        if (of.getKundenkategorieID() != null && !"0".equals(of.getKundenkategorieID())) {
            urlParam.append("&category=");
            urlParam.append(urlCoder.encode(of.getKundenkategorieID(), "UTF-8"));
        }
        if (of.getKundenadresse() != null && !"".equals(of.getKundenadresse())) {
            urlParam.append("&adresse=");
            urlParam.append(urlCoder.encode(of.getKundenadresse(), "UTF-8"));
        }
        if (of.getKundenstrasse() != null && !"".equals(of.getKundenstrasse())) {
            urlParam.append("&adresse=");
            urlParam.append(urlCoder.encode(of.getKundenstrasse(), "UTF-8"));
        }
        if (of.getKundentelefon() != null && !"".equals(of.getKundentelefon())) {
            urlParam.append("&telefonnrg=");
            urlParam.append(of.getKundentelefon());
        }
        if (of.getKundenplz() != null && !"".equals(of.getKundenplz())) {
            urlParam.append("&plz=");
            urlParam.append(urlCoder.encode(of.getKundenplz(), "UTF-8"));
        }
        if (of.getKundenort() != null && !"".equals(of.getKundenort())) {
            urlParam.append("&ort=");
            urlParam.append(urlCoder.encode(of.getKundenort(), "UTF-8"));
        }
        if (of.getKundenland() != null && !"".equals(of.getKundenland())) {
            urlParam.append("&land=");
            urlParam.append(urlCoder.encode(of.getKundenland(), "UTF-8"));
        }

        return urlParam.toString();
    }

    /**
     * extrahiert mit einem Regex die Email aus einem String
     */
    private String extractEmail(final String email) {
        String extractedEmail = email;
        try {
            final Pattern p = Pattern
                    .compile("[A-Za-z0-9._-]+@[A-Za-z0-9][A-Za-z0-9.-]{0,61}[A-Za-z0-9]\\.[A-Za-z.]{2,6}");
            final Matcher m = p.matcher(email);

            if (m.find()) {
                extractedEmail = email.substring(m.start(), m.end());
            }

        } catch (final Exception e) {
            LOG.error("extractEmail(String email): " + email + "\040" + e.toString());
        }

        return extractedEmail;
    }

    /**
     * Prüft auf URL-hacking bei modify (Bestellformular)
     */
    private boolean checkPermission(final UserInfo ui, final BestellFormParam bp, final Connection cn) {

        boolean check = false;
        Text t = new Text();

        try {

            if (bp != null && bp.getId() != null) {

                if (bp.getId() == 0) {
                    check = true; // Bestellformular eingeloggt
                    return check;
                }
                if (bp.getId() == -1) { // neues Bestellformulat IP-basiert
                    if (t.hasIP(cn, ui.getKonto())) {
                        check = true;
                    }
                    return check;
                }
                if (bp.getId() == BestellformNumber.KKID.getValue() && bp.getKennung() != null) { // Konto-Kennung
                    t = new Text(cn, TextType.ACCOUNT_ID_OVERRIDES_IP, bp.getKennung());
                    if (t.getKonto().getId() != null && t.getKonto().getId().equals(ui.getKonto().getId())) {
                        check = true;
                        return check;
                    }
                }
                if (bp.getId() == BestellformNumber.BKID.getValue() && bp.getKennung() != null) { // Broker-Kennung
                    t = new Text(cn, TextType.ACCOUNT_ID_OVERRIDDEN_BY_IP, bp.getKennung());
                    if (t.getKonto().getId() != null && t.getKonto().getId().equals(ui.getKonto().getId())) {
                        check = true;
                        return check;
                    }
                }
                final BestellFormParam bpCompare = new BestellFormParam(bp.getId(), cn);
                // Prüfung, ob die ID zum Konto gehört! (URL-hacking)
                if (bpCompare.getKid() != null && bpCompare.getKid().equals(ui.getKonto().getId())) {
                    check = true;
                    return check;
                }
            }

        } catch (final Exception e) {
            LOG.error("checkPermission(UserInfo ui, Long id): " + e.toString());
        }

        return check;
    }

    /**
     * Prüft Eingaben bei einem BestellFormParam auf die logischen Abhängigkeiten,
     * setzt automatisch abhängige Werte und gibt bei fehlenden Werten ggf. eine
     * Fehlermeldung aus
     */
    private BestellFormParam checkBPLogic(final BestellFormParam bp) {

        // serielle Ausgabe um ggf. auf der jsp mehrsprachige Fehlermeldungen zu triggern

        try {

            final Message m = new Message();
            final Check ck = new Check();

            if (ck.isMinLength(bp.getLieferart_value1(), 1) || ck.isMinLength(bp.getLieferart_value2(), 1)
                    || ck.isMinLength(bp.getLieferart_value3(), 1)) {

                if (ck.isMinLength(bp.getLieferart_value1(), 1)) {
                    bp.setLieferart(true); // gültige Eingaben erfolgt
                } else {
                    m.setMessage("bestellformconfigure.deliveryway");
                    bp.setMessage(m);
                }

            }

            if (bp.isFreitxt1() && !ck.isMinLength(bp.getFreitxt1_name(), 1)) {
                m.setMessage("bestellformconfigure.frei1");
                bp.setMessage(m);
            } else {
                if (!bp.isFreitxt1() && ck.isMinLength(bp.getFreitxt1_name(), 1)) {
                    bp.setFreitxt1_name(""); // verhindert ,dass Werte bei nicht aktivierter Option in DB geschrieben
                    // werden
                }
            }
            if (bp.isFreitxt2() && !ck.isMinLength(bp.getFreitxt2_name(), 1)) {
                m.setMessage("bestellformconfigure.frei2");
                bp.setMessage(m);
            } else {
                if (!bp.isFreitxt2() && ck.isMinLength(bp.getFreitxt2_name(), 1)) {
                    bp.setFreitxt2_name(""); // verhindert ,dass Werte bei nicht aktivierter Option in DB geschrieben
                    // werden
                }
            }
            if (bp.isFreitxt3() && !ck.isMinLength(bp.getFreitxt3_name(), 1)) {
                m.setMessage("bestellformconfigure.frei3");
                bp.setMessage(m);
            } else {
                if (!bp.isFreitxt3() && ck.isMinLength(bp.getFreitxt3_name(), 1)) {
                    bp.setFreitxt3_name(""); // verhindert ,dass Werte bei nicht aktivierter Option in DB geschrieben
                    // werden
                }
            }

            if (ck.isMinLength(bp.getOption_value1(), 1) || ck.isMinLength(bp.getOption_value2(), 1)
                    || ck.isMinLength(bp.getOption_value3(), 1)) {

                if (ck.isMinLength(bp.getOption_value1(), 1)) {
                    bp.setOption(true); // gültige Eingaben erfolgt
                } else {
                    m.setMessage("bestellformconfigure.option");
                    bp.setMessage(m);
                }

            } else {
                bp.setOption_name("");
                bp.setOption_comment("");
                bp.setOption_linkout("");
                bp.setOption_linkoutname("");
            }

            if (bp.isGebuehren()) {
                if (!ck.isMinLength(bp.getLink_gebuehren(), 1)) {
                    m.setMessage("bestellformconfigure.fee");
                    bp.setMessage(m);
                } else {
                    if (!ck.isUrl(bp.getLink_gebuehren())) {
                        m.setMessage("bestellformconfigure.fee_link");
                        bp.setMessage(m);
                    }
                }
            } else {
                if (!bp.isGebuehren() && ck.isMinLength(bp.getLink_gebuehren(), 1)) {
                    bp.setLink_gebuehren(""); // verhindert ,dass Werte bei nicht aktivierter Option in DB geschrieben
                    // werden
                }
            }
            if (bp.isAgb()) {
                if (!ck.isMinLength(bp.getLink_agb(), 1)) {
                    m.setMessage("bestellformconfigure.agb");
                    bp.setMessage(m);
                } else {
                    if (!ck.isUrl(bp.getLink_agb())) {
                        m.setMessage("bestellformconfigure.agb_link");
                        bp.setMessage(m);
                    }
                }
            } else {
                if (!bp.isAgb() && ck.isMinLength(bp.getLink_agb(), 1)) {
                    bp.setLink_agb(""); // verhindert ,dass Werte bei nicht aktivierter Option in DB geschrieben werden
                }
            }

        } catch (final Exception e) {
            LOG.error("checkBPLogic(BestellFormParam bp): " + e.toString());
        }

        return bp;
    }

}
