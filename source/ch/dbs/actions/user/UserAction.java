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

package ch.dbs.actions.user;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.SortedMap;
import java.util.TimeZone;
import java.util.TreeMap;

import javax.mail.internet.InternetAddress;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.actions.DispatchAction;
import org.apache.struts.util.MessageResources;
import org.apache.struts.util.RequestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import util.Auth;
import util.Check;
import util.Encrypt;
import util.MHelper;
import util.PasswordGenerator;
import util.ReadSystemConfigurations;
import util.ThreadSafeSimpleDateFormat;
import ch.dbs.entity.AbstractBenutzer;
import ch.dbs.entity.Benutzer;
import ch.dbs.entity.Bestellungen;
import ch.dbs.entity.Countries;
import ch.dbs.entity.Konto;
import ch.dbs.entity.OrderState;
import ch.dbs.entity.Text;
import ch.dbs.entity.VKontoBenutzer;
import ch.dbs.form.ActiveMenusForm;
import ch.dbs.form.ErrorMessage;
import ch.dbs.form.KontoForm;
import ch.dbs.form.LoginForm;
import ch.dbs.form.Message;
import ch.dbs.form.OrderForm;
import ch.dbs.form.OverviewForm;
import ch.dbs.form.SearchesForm;
import ch.dbs.form.UserForm;
import ch.dbs.form.UserInfo;
import ch.dbs.login.Gtc;
import enums.Result;
import enums.TextType;

public final class UserAction extends DispatchAction {

    private static final Logger LOG = LoggerFactory.getLogger(UserAction.class);
    private static final String UEBERSICHT = "uebersicht";

    public ActionForward stati(final ActionMapping mp, final ActionForm form, final HttpServletRequest rq,
            final HttpServletResponse rp) {

        final Auth auth = new Auth();
        // make sure the user is logged in
        if (!auth.isLogin(rq)) {
            return mp.findForward(Result.ERROR_TIMEOUT.getValue());
        }
        // if activated on system level, access will be restricted to paid only
        if (auth.isPaidOnly(rq)) {
            return mp.findForward(Result.ERROR_PAID_ONLY.getValue());
        }

        final ActiveMenusForm mf = new ActiveMenusForm();
        mf.setActivemenu(UEBERSICHT);
        rq.setAttribute(Result.ACTIVEMENUS.getValue(), mf);

        return mp.findForward(Result.SUCCESS.getValue());
    }

    /**
     * Übersicht Zeigt Bestellungen an. Kann durch Benutzer festgelegt werden,
     * nach welchem Feld er ab- und aufsteigend sortieren will
     */
    public ActionForward overview(final ActionMapping mp, final ActionForm fm, final HttpServletRequest rq,
            final HttpServletResponse rp) {

        final Auth auth = new Auth();
        // make sure the user is logged in
        if (!auth.isLogin(rq)) {
            return mp.findForward(Result.ERROR_TIMEOUT.getValue());
        }
        // if activated on system level, access will be restricted to paid only
        if (auth.isPaidOnly(rq)) {
            return mp.findForward(Result.ERROR_PAID_ONLY.getValue());
        }

        // Make sure method is only accessible when user is logged in
        String forward = Result.FAILURE.getValue();

        final OverviewForm of = (OverviewForm) fm;
        final Bestellungen b = new Bestellungen();

        final Check check = new Check();
        final Text cn = new Text();

        try {

            forward = Result.SUCCESS.getValue();
            final UserInfo ui = (UserInfo) rq.getSession().getAttribute("userinfo");

            final ActiveMenusForm mf = new ActiveMenusForm();
            mf.setActivemenu(UEBERSICHT);
            rq.setAttribute(Result.ACTIVEMENUS.getValue(), mf);

            // hier wird geprüft, ob Suchkriterien eingegeben wurden, oder die Sortierung einer Suche vorliegt
            if (!checkIfInputIsNotEmpty(of) && !of.isS()) {

                // Auflistung möglicher Bestellstati um Statuswechsel dem User / Bibliothekar / Admin anzubieten
                // wird auch für checkFilterCriteriasAgainstAllTextsFromTexttypPlusKontoTexts benötigt
                of.setStatitexts(cn.getAllTextPlusKontoTexts(TextType.STATE_ORDER, ui.getKonto().getId(),
                        cn.getConnection()));

                // angegebener Zeitraum prüfen, resp. Defaultbereich von 1 Monat zusammenstellen
                check.checkDateRegion(of, 1, ui.getKonto().getTimezone());
                // Ueberprüfung der Sortierkriterien, ob diese gültig sind. Wenn ja, Sortierung anwenden
                check.checkSortOrderValues(of);
                //Check, damit nur gültige Sortierkriterien daherkommen
                check.checkFilterCriteriasAgainstAllTextsFromTexttypPlusKontoTexts(of);
                // Ueberprüfung der Sortierkriterien, ob diese gültig sind. Wenn ja, Sortierung anwenden
                check.checkOrdersSortCriterias(of);

                // Benutzer dürfen nur ihre eigenen Bestellungen sehen
                if (!auth.isBibliothekar(rq) && !auth.isAdmin(rq)) {
                    if (of.getFilter() == null) {
                        of.setBestellungen(b.getAllUserOrders(ui.getBenutzer(), of, cn.getConnection()));
                    } else {
                        of.setBestellungen(b.getAllUserOrdersPerStatus(ui.getBenutzer(), of, false, cn.getConnection()));
                    }
                } else { // Bibliothekare dürfen nur Bestellungen ihrers Kontos sehen
                    if (of.getFilter() == null) {
                        of.setBestellungen(b.getOrdersPerKonto(ui.getKonto(), of, cn.getConnection()));
                    } else {
                        of.setBestellungen(b.getOrdersPerKontoPerStatus(ui.getKonto().getId(), of, false,
                                cn.getConnection()));
                    }
                }

                // Suchfelder bestimmen
                final SortedMap<String, String> result = composeSortedLocalisedOrderSearchFields(rq);
                rq.setAttribute("sortedSearchFields", result);

                rq.setAttribute("overviewform", of);

            } else { // Umleitung auf Suche
                forward = "search";
            }

        } finally {
            cn.close();
        }

        return mp.findForward(forward);
    }

    /**
     * Wechselt den Status einer Bestellung, unter Berücksichtigung allfällig
     * bestehender Sortierung und Filterung
     */
    public ActionForward changestat(final ActionMapping mp, final ActionForm form, final HttpServletRequest rq,
            final HttpServletResponse rp) {

        final Auth auth = new Auth();
        // make sure the user is logged in
        if (!auth.isLogin(rq)) {
            return mp.findForward(Result.ERROR_TIMEOUT.getValue());
        }
        // if activated on system level, access will be restricted to paid only
        if (auth.isPaidOnly(rq)) {
            return mp.findForward(Result.ERROR_PAID_ONLY.getValue());
        }

        final OverviewForm of = (OverviewForm) form;
        final OrderState orderstate = new OrderState();

        String forward = Result.FAILURE.getValue();

        final UserInfo ui = (UserInfo) rq.getSession().getAttribute("userinfo");

        final Text t = new Text();

        try {
            final Text status = new Text(t.getConnection(), of.getTid(), TextType.STATE_ORDER);
            final Bestellungen b = new Bestellungen(t.getConnection(), of.getBid());
            if (b != null && status != null) {
                orderstate.changeOrderState(b, ui.getKonto().getTimezone(), status, null, ui.getBenutzer().getEmail(),
                        t.getConnection());
                forward = Result.SUCCESS.getValue();
                rq.setAttribute("overviewform", of);
            }

        } catch (final Exception e) {
            forward = Result.FAILURE.getValue();

            final ErrorMessage em = new ErrorMessage();
            em.setError("error.system");
            em.setLink("searchfree.do?activemenu=suchenbestellen");
            rq.setAttribute(Result.ERRORMESSAGE.getValue(), em);
            LOG.error("changestat: " + e.toString());

        } finally {
            t.close();
        }

        return mp.findForward(forward);
    }

    /**
     * Wechselt den Status einer Bestellung, unter Berücksichtigung allfällig
     * bestehender Sortierung und Filterung
     */
    public ActionForward changenotes(final ActionMapping mp, final ActionForm form, final HttpServletRequest rq,
            final HttpServletResponse rp) {

        final Auth auth = new Auth();
        // make sure the user is logged in
        if (!auth.isLogin(rq)) {
            return mp.findForward(Result.ERROR_TIMEOUT.getValue());
        }
        // if activated on system level, access will be restricted to paid only
        if (auth.isPaidOnly(rq)) {
            return mp.findForward(Result.ERROR_PAID_ONLY.getValue());
        }

        final OverviewForm of = (OverviewForm) form;

        String forward = Result.FAILURE.getValue();

        final Text cn = new Text();

        try {
            final Bestellungen b = new Bestellungen(cn.getConnection(), of.getBid());
            b.setNotizen(of.getNotizen());
            if (b != null) {
                b.update(cn.getConnection());
            }
            forward = Result.SUCCESS.getValue();
            rq.setAttribute("overviewform", of);

        } catch (final Exception e) {
            forward = Result.FAILURE.getValue();

            final ErrorMessage em = new ErrorMessage();
            em.setError("error.system");
            em.setLink("searchfree.do?activemenu=suchenbestellen");
            rq.setAttribute(Result.ERRORMESSAGE.getValue(), em);
            LOG.error("changenotes: " + e.toString());

        } finally {
            cn.close();
        }

        return mp.findForward(forward);
    }

    /**
     * Konto wechseln
     */
    public ActionForward changekonto(final ActionMapping mp, final ActionForm form, final HttpServletRequest rq,
            final HttpServletResponse rp) {

        final Auth auth = new Auth();
        // make sure the user is logged in
        if (!auth.isLogin(rq)) {
            return mp.findForward(Result.ERROR_TIMEOUT.getValue());
        }

        final KontoForm kf = (KontoForm) form;
        final OrderForm pageForm = new OrderForm(kf);
        String forward = Result.FAILURE.getValue();

        final UserInfo ui = (UserInfo) rq.getSession().getAttribute("userinfo");
        final Text cn = new Text();
        try {
            // check that user is allowed to change to this account...
            final VKontoBenutzer vkb = new VKontoBenutzer();
            if (vkb.isUserFromKonto(kf.getKid(), ui.getBenutzer().getId(), cn.getConnection())
                    || ui.getBenutzer().getRechte() == 3) {
                ui.setKonto(new Konto(kf.getKid(), cn.getConnection()));
            } else {
                // ...we have some form hacking => logout
                final ActiveMenusForm mf = new ActiveMenusForm();
                mf.setActivemenu(Result.LOGIN.getValue());
                rq.setAttribute(Result.ACTIVEMENUS.getValue(), mf);
                LOG.info("changekonto: prevented URL-hacking! " + ui.getBenutzer().getEmail());
                return mp.findForward("logout");
            }

        } finally {
            cn.close();
        }
        rq.getSession().setAttribute("userinfo", ui);

        if (auth.isUserlogin(rq) && auth.isUserAccount(rq)) {
            forward = Result.SUCCESS.getValue();
        } else {
            rq.getSession().setAttribute("userinfo", null); // userinfo aus Session löschen
            final ErrorMessage em = new ErrorMessage("error.berechtigung", "login.do");
            rq.setAttribute(Result.ERRORMESSAGE.getValue(), em);
        }

        final ActiveMenusForm mf = new ActiveMenusForm();
        mf.setActivemenu("suchenbestellen");
        rq.setAttribute(Result.ACTIVEMENUS.getValue(), mf);

        if (kf.isResolver()) {
            // hier kommen auch Artikelangaben aus der Übergabe des Linkresolvers mit...
            rq.setAttribute("orderform", pageForm); // Übergabe in jedem Fall
            // Die Bestellberechtigung wird in der Methode prepare geprüft!
            if (forward.equals(Result.SUCCESS.getValue()) && auth.isBenutzer(rq)) {
                forward = "order";
            }
            // Bibliothekar oder Admin auf Checkavailability
            if (forward.equals(Result.SUCCESS.getValue()) && !auth.isBenutzer(rq)) {
                forward = "checkavailability";
            }
        }
        if (kf.getKundenemail() != null && !kf.getKundenemail().equals("")) {
            // Übergabe von Userangaben von Link aus Bestellform-Email
            if (forward.equals(Result.SUCCESS.getValue()) && !auth.isBenutzer(rq)) {
                forward = "adduser";
            }
            final UserForm uf = new UserForm(kf);
            rq.setAttribute("userform", uf);
        }

        return mp.findForward(forward);
    }

    /**
     * Benutzer setzen wenn Logininfos Login bei verschiedenen konten zulassen
     */
    public ActionForward setuser(final ActionMapping mp, final ActionForm form, final HttpServletRequest rq,
            final HttpServletResponse rp) {

        final Auth auth = new Auth();
        // if activated on system level, access will be restricted to paid only
        if (auth.isPaidOnly(rq)) {
            return mp.findForward(Result.ERROR_PAID_ONLY.getValue());
        }

        final LoginForm authuserlist = (LoginForm) rq.getSession().getAttribute("authuserlist");
        final UserInfo ui = new UserInfo();
        final LoginForm lf = (LoginForm) form; // Infos mit welchem Benutzer gearbeitet werden soll

        String forward = Result.FAILURE.getValue();
        final ActiveMenusForm mf = new ActiveMenusForm();
        mf.setActivemenu(Result.LOGIN.getValue());

        final Text cn = new Text();

        //  Ueberprüfung ob Auswahl aus LoginForm tatsächlich authorisierte Benutzer sind
        final Gtc gtc = new Gtc();

        try {

            for (final UserInfo authlist : authuserlist.getUserinfolist()) {
                if (lf.getUserid().equals(authlist.getBenutzer().getId())) {

                    //Benutzer und Kontos in ui setzen
                    ui.setBenutzer(authlist.getBenutzer());
                    ui.setKontos(authlist.getKontos());
                    rq.getSession().setAttribute("userinfo", ui);

                    //Bei nur einem Konto dieses gleich setzen
                    if (ui.getKontos().size() == 1) {
                        ui.setKonto(ui.getKontos().get(0));
                        // Last-Login Datum beim Benutzer hinterlegen
                        final AbstractBenutzer u = ui.getBenutzer();
                        u.updateLastuse(u, ui.getKonto().getTimezone(), cn.getConnection());

                        if (gtc.isAccepted(ui.getBenutzer(), cn.getConnection())) {
                            forward = Result.SUCCESS.getValue();
                            mf.setActivemenu("suchenbestellen");
                        } else {
                            forward = "gtc";
                        }
                    }
                    //Falls Benutzer unter mehreren Kontos arbeiten darf weiterleitung zur Kontoauswahl
                    if (ui.getKontos().size() > 1) {
                        // Last-Login Datum beim Benutzer hinterlegen
                        final AbstractBenutzer u = ui.getBenutzer();
                        u.updateLastuse(u, ui.getKontos().get(0).getTimezone(), cn.getConnection());
                        forward = "kontochoose";
                    }
                }
            }

            // Fehlermeldung bereitstellen falls mittels URL-hacking versucht wurde zu manipulieren
            if (forward.equals(Result.FAILURE.getValue())) {
                rq.getSession().setAttribute("userinfo", null);
                final ErrorMessage em = new ErrorMessage("error.hack", "login.do");
                rq.setAttribute(Result.ERRORMESSAGE.getValue(), em);
                LOG.info("setuser: prevented URL-hacking! " + ui.getBenutzer().getEmail());
            }

            // Angaben vom Linkresolver
            if (lf.isResolver()) {
                // hier kommen auch Artikelangaben aus der Übergabe des Linkresolvers mit...
                rq.setAttribute("orderform", lf); // Übergabe in jedem Fall
                // Die Bestellberechtigung wird in der Methode prepare geprüft!
                if (forward.equals(Result.SUCCESS.getValue()) && auth.isBenutzer(rq)) {
                    forward = "order";
                }
                // Bibliothekar oder Admin auf Checkavailability
                if (forward.equals(Result.SUCCESS.getValue()) && !auth.isBenutzer(rq)) {
                    forward = "checkavailability";
                }
            }

            rq.setAttribute(Result.ACTIVEMENUS.getValue(), mf);

        } finally {
            cn.close();
        }

        return mp.findForward(forward);
    }

    /**
     * Dient dazu beim Aufruf von prepareAddUser ab journalorder.jsp, alle
     * Bestellangaben zu behalten...
     */
    public ActionForward keepOrderDetails(final ActionMapping mp, final ActionForm form, final HttpServletRequest rq,
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
        // if activated on system level, access will be restricted to paid only
        if (auth.isPaidOnly(rq)) {
            return mp.findForward(Result.ERROR_PAID_ONLY.getValue());
        }

        final OrderForm pageForm = (OrderForm) form;
        rq.getSession().setAttribute("ofjo", pageForm);

        final UserInfo ui = (UserInfo) rq.getSession().getAttribute("userinfo");
        // damit kann in prepareAddUser kontrolliert werden, dass odervalues dabei sind!
        ui.setKeepordervalues(true);
        rq.getSession().setAttribute("userinfo", ui);

        return mp.findForward(Result.SUCCESS.getValue());
    }

    /**
     * Hinzufuegen eines neuen Benutzers vorbereiten
     */
    public ActionForward prepareAddUser(final ActionMapping mp, final ActionForm form, final HttpServletRequest rq,
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
        // if activated on system level, access will be restricted to paid only
        if (auth.isPaidOnly(rq)) {
            return mp.findForward(Result.ERROR_PAID_ONLY.getValue());
        }

        String forward = Result.FAILURE.getValue();
        final ErrorMessage em = new ErrorMessage();
        final Text cn = new Text();
        final Countries country = new Countries();
        final Konto konto = new Konto();

        try {

            final UserInfo ui = (UserInfo) rq.getSession().getAttribute("userinfo");

            if (ui.isKeepordervalues()) { // hier wird festegestellt, ob keepOrderDetails durchlaufen wurde
                ui.setKeepordervalues(false);
                ui.setKeepordervalues2(false);
                // keepordervalues aus Session löschen, damit nicht etwas hängen bleibt...
                rq.getSession().setAttribute("userinfo", ui);
                ui.setKeepordervalues2(true);
            } else {
                // ggf. orderform unterdrücken, welches noch in der Session hängt...
                rq.getSession().setAttribute("ofjo", null);
            }

            final List<Konto> allPossKontos = konto.getAllAllowedKontosAndSelectActive(ui, cn.getConnection());
            final ArrayList<KontoForm> lkf = new ArrayList<KontoForm>();

            for (final Konto k : allPossKontos) {
                final KontoForm kf = new KontoForm();
                kf.setKonto(k);
                lkf.add(kf);
            }

            final List<Countries> allPossCountries = country.getAllCountries(cn.getConnection());
            final List<Text> categories = cn.getAllKontoText(TextType.USER_CATEGORY, ui.getKonto().getId(),
                    cn.getConnection());

            ui.setKontos(allPossKontos);
            ui.setCountries(allPossCountries);
            rq.setAttribute("categories", categories);
            forward = Result.SUCCESS.getValue();
            rq.setAttribute("ui", ui);

            rq.setAttribute(Result.ERRORMESSAGE.getValue(), em); // unterdrückt falsche Fehlermeldungen aus Bestellform...

        } finally {
            cn.close();
        }

        return mp.findForward(forward);
    }

    /**
     * Benutzereinstellungen ändern
     */
    public ActionForward changeuserdetails(final ActionMapping mp, final ActionForm form, final HttpServletRequest rq,
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
        // if activated on system level, access will be restricted to paid only
        if (auth.isPaidOnly(rq)) {
            return mp.findForward(Result.ERROR_PAID_ONLY.getValue());
        }

        String forward = Result.FAILURE.getValue();
        ErrorMessage em = new ErrorMessage();
        final Countries country = new Countries();
        final Text cn = new Text();
        final VKontoBenutzer vKontoBenutzer = new VKontoBenutzer();

        try {

            final UserForm uf = (UserForm) form;
            final UserInfo ui = (UserInfo) rq.getSession().getAttribute("userinfo");
            AbstractBenutzer u = new AbstractBenutzer();
            u = u.getUser(uf.getBid(), cn.getConnection());

            // Sicherstellen, dass nur Kunden vom eigenen Konto bearbeitet werden können...
            if (u != null && vKontoBenutzer.isUserFromKonto(ui.getKonto().getId(), u.getId(), cn.getConnection())) {

                uf.setUser(u);

                // selektiert alle Konten unter denen ein Kunde angehängt ist
                final List<Konto> kontolist = ui.getKonto().getKontosForBenutzer(u, cn.getConnection());
                final List<Konto> allPossKontos = ui.getKonto().getLoginKontos(ui.getBenutzer(), cn.getConnection());
                for (final Konto k : kontolist) {
                    int y = 0;
                    for (final Konto uik : allPossKontos) {
                        if (uik.getId().longValue() == k.getId().longValue()) {
                            uik.setSelected(true);
                            allPossKontos.set(y, uik);
                        }
                        y++;
                    }
                }
                final ArrayList<KontoForm> lkf = new ArrayList<KontoForm>();
                for (final Konto k : allPossKontos) {
                    final KontoForm kf = new KontoForm();
                    kf.setKonto(k);
                    lkf.add(kf);
                }

                final List<Countries> allPossCountries = country.getAllCountries(cn.getConnection());
                final List<Text> categories = cn.getAllKontoText(TextType.USER_CATEGORY, ui.getKonto().getId(),
                        cn.getConnection());

                ui.setKontos(allPossKontos);
                ui.setCountries(allPossCountries);
                forward = Result.SUCCESS.getValue();
                rq.setAttribute("categories", categories);
                rq.setAttribute("ui", ui);
                rq.setAttribute("userform", uf);

            } else {
                forward = Result.FAILURE.getValue();
                em = new ErrorMessage("error.hack");
                em.setLink("searchfree.do?activemenu=suchenbestellen");
                LOG.info("changeuserdetails: prevented URL-hacking! " + ui.getBenutzer().getEmail());
            }

            rq.setAttribute(Result.ERRORMESSAGE.getValue(), em); // unterdrückt falsche Fehlermeldungen aus Bestellform...

        } finally {
            cn.close();
        }

        return mp.findForward(forward);
    }

    /**
     * Generates a new password and sends it to the email adress specified.
     */
    public ActionForward pwreset(final ActionMapping mp, final ActionForm form, final HttpServletRequest rq,
            final HttpServletResponse rp) {

        final LoginForm lf = (LoginForm) form;
        String forward = Result.FAILURE.getValue();
        final Text cn = new Text();
        final MessageResources messageResources = getResources(rq);
        final ErrorMessage em = new ErrorMessage();
        AbstractBenutzer u = new AbstractBenutzer();

        try {

            u = u.getLoginUserFromEmail(lf.getEmail(), cn.getConnection());
            //Check ob der Benutzer berechtigt ist sein Passwort zurückzusetzen
            if (u != null) {
                if (u.isLoginopt() || u.getRechte() > 1) {
                    // Passwort erzeugen
                    final PasswordGenerator p = new PasswordGenerator(8);
                    final String pw = p.getRandomString();
                    // Passwort codieren und in DB speichern
                    final Encrypt e = new Encrypt();
                    u.setPassword(e.makeSHA(pw));
                    final Konto tz = new Konto(); // we need this for setting a default timezone
                    u.updateUser(u, tz.getTimezone(), cn.getConnection());
                    // Benutzer per Mail das neue Passwort mitteilen
                    final InternetAddress[] recipients = new InternetAddress[1];
                    recipients[0] = new InternetAddress(u.getEmail());
                    final StringBuffer msg = new StringBuffer();
                    msg.append(messageResources.getMessage("resend.email.intro"));
                    msg.append("\n\n");
                    msg.append(pw);
                    msg.append("\n\n");
                    msg.append(messageResources.getMessage("resend.email.greetings"));
                    msg.append('\n');
                    msg.append(messageResources.getMessage("resend.email.team"));
                    msg.append('\040');
                    msg.append(ReadSystemConfigurations.getApplicationName());

                    final String subject = messageResources.getMessage("resend.email.subject") + "\040"
                            + ReadSystemConfigurations.getApplicationName();

                    final MHelper m = new MHelper(recipients, subject, msg.toString());
                    m.send();

                    forward = Result.SUCCESS.getValue();
                    rq.setAttribute("message", new Message("message.pwreset", "login.do"));

                } else {
                    em.setError("error.pwreset");
                    em.setLink("login.do");
                    rq.setAttribute(Result.ERRORMESSAGE.getValue(), em);
                    LOG.warn(lf.getEmail() + " ist registriert und hat versucht sich "
                            + "unberechtigterweise eine Loginberechtigung per Email zu schicken!");
                }
            } else {
                em.setError("error.unknown_email");
                em.setLink("login.do");
                rq.setAttribute(Result.ERRORMESSAGE.getValue(), em);
                LOG.warn(lf.getEmail() + " (unbekannt) hat versucht sich "
                        + "unberechtigterweise eine Loginberechtigung per Email zu schicken!");
            }

        } catch (final Exception e) {
            em.setError("error.system");
            em.setLink("login.do");
            rq.setAttribute(Result.ERRORMESSAGE.getValue(), em);
            LOG.error(e.toString());
        } finally {
            cn.close();
        }

        return mp.findForward(forward);
    }

    /**
     * Uebersicht Benutzer des Kontos
     */
    public ActionForward showkontousers(final ActionMapping mp, final ActionForm fm, final HttpServletRequest rq,
            final HttpServletResponse rp) {

        final Auth auth = new Auth();
        // make sure the user is logged in
        if (!auth.isLogin(rq)) {
            return mp.findForward(Result.ERROR_TIMEOUT.getValue());
        }
        // if activated on system level, access will be restricted to paid only
        if (auth.isPaidOnly(rq)) {
            return mp.findForward(Result.ERROR_PAID_ONLY.getValue());
        }

        final UserForm uf = new UserForm();

        final UserInfo ui = (UserInfo) rq.getSession().getAttribute("userinfo");
        // Bei URL-hacking: User sehen nur sich selber
        if (auth.isBenutzer(rq)) {
            final ArrayList<AbstractBenutzer> ul = new ArrayList<AbstractBenutzer>();
            ul.add(ui.getBenutzer());
            uf.setUsers(ul);
        }
        // Bibliothekare sehen alle Benutzer eines Kontos
        if (auth.isBibliothekar(rq) || auth.isAdmin(rq)) {
            final Text t = new Text();
            try {
                uf.setUsers(ui.getBenutzer().getKontoUser(ui.getKonto(), t.getConnection()));
            } finally {
                t.close();
            }
        }
        rq.setAttribute("userform", uf);

        return mp.findForward(Result.SUCCESS.getValue());
    }

    /**
     * neuer User anlegen abschliessen, User bearbeiten
     */
    public ActionForward modifykontousers(final ActionMapping mp, final ActionForm fm, final HttpServletRequest rq,
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
        // if activated on system level, access will be restricted to paid only
        if (auth.isPaidOnly(rq)) {
            return mp.findForward(Result.ERROR_PAID_ONLY.getValue());
        }

        String forward = Result.FAILURE.getValue();
        final Text cn = new Text();
        final Check check = new Check();
        final VKontoBenutzer vKontoBenutzer = new VKontoBenutzer();

        try {

            final UserInfo ui = (UserInfo) rq.getSession().getAttribute("userinfo");
            final UserForm uf = (UserForm) fm;

            // ggf. Leerschläge entfernen
            if (uf.getEmail() != null) {
                uf.setEmail(uf.getEmail().trim());
            }

            final boolean name = check.isMinLength(uf.getName(), 2);
            final boolean vorname = check.isMinLength(uf.getVorname(), 2);
            final boolean email = check.isEmail(uf.getEmail());
            final boolean kont = uf.getKontos() != null;
            final boolean land = check.isMinLength(uf.getLand(), 2);

            //Test, ob alle Sollangaben gemacht wurden
            if (name && vorname && email && kont && land) {

                final String[] kontos = uf.getKontos();

                forward = Result.SUCCESS.getValue();
                AbstractBenutzer u = new Benutzer();

                if (uf.getBid() != null) {
                    u = u.getUser(uf.getBid(), cn.getConnection());
                }

                u.setAnrede(uf.getAnrede());
                if (uf.getVorname() != null) {
                    u.setVorname(uf.getVorname().trim());
                } else {
                    u.setVorname(uf.getVorname());
                }
                if (uf.getName() != null) {
                    u.setName(uf.getName().trim());
                } else {
                    u.setName(uf.getName());
                }
                if (uf.getEmail() != null) {
                    u.setEmail(uf.getEmail().trim());
                } else {
                    u.setEmail(uf.getEmail());
                }
                if (uf.getTelefonnrg() != null) {
                    u.setTelefonnrg(uf.getTelefonnrg().trim());
                } else {
                    u.setTelefonnrg(uf.getTelefonnrg());
                }
                if (uf.getTelefonnrp() != null) {
                    u.setTelefonnrp(uf.getTelefonnrp().trim());
                } else {
                    u.setTelefonnrp(uf.getTelefonnrp());
                }
                if (uf.getInstitut() != null) {
                    u.setInstitut(uf.getInstitut().trim());
                } else {
                    u.setInstitut(uf.getInstitut());
                }
                if (uf.getAbteilung() != null) {
                    u.setAbteilung(uf.getAbteilung().trim());
                } else {
                    u.setAbteilung(uf.getAbteilung());
                }

                u.setCategory(new Text(cn.getConnection(), Long.valueOf(uf.getCategory()), TextType.USER_CATEGORY));

                if (uf.getAdresse() != null) {
                    u.setAdresse(uf.getAdresse().trim());
                } else {
                    u.setAdresse(uf.getAdresse());
                }
                if (uf.getAdresszusatz() != null) {
                    u.setAdresszusatz(uf.getAdresszusatz().trim());
                } else {
                    u.setAdresszusatz(uf.getAdresszusatz());
                }
                if (uf.getPlz() != null) {
                    u.setPlz(uf.getPlz().trim());
                } else {
                    u.setPlz(uf.getPlz());
                }
                if (uf.getOrt() != null) {
                    u.setOrt(uf.getOrt().trim());
                } else {
                    u.setOrt(uf.getOrt());
                }
                u.setLand(uf.getLand());
                u.setLoginopt(uf.isLoginopt());
                u.setKontostatus(uf.isKontostatus());
                final Encrypt e = new Encrypt();
                u.setPassword(e.makeSHA(uf.getPassword()));
                u.setUserbestellung(uf.isUserbestellung()); // SUBITO
                u.setGbvbestellung(uf.isGbvbestellung()); // GBV
                u.setKontovalidation(uf.isKontovalidation());
                u.setBilling(uf.getBilling());
                u.setGtc(uf.getGtc());
                u.setGtcdate(uf.getGtcdate());

                if (uf.getBid() == null) {
                    final Date d = new Date();
                    final ThreadSafeSimpleDateFormat fmt = new ThreadSafeSimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    final String datum = fmt.format(d, ui.getKonto().getTimezone());
                    u.setDatum(datum);
                    uf.setBid(u.saveNewUser(u, ui.getKonto().getTimezone(), cn.getConnection()));
                    // Sicherstellen, dass nur Kunden vom eigenen Konto bearbeitet werden können...
                } else if (vKontoBenutzer.isUserFromKonto(ui.getKonto().getId(), uf.getBid(), cn.getConnection())) {
                    u.setId(uf.getBid());
                    u.updateUser(u, ui.getKonto().getTimezone(), cn.getConnection());
                } else {
                    ErrorMessage em = new ErrorMessage();
                    forward = Result.FAILURE.getValue();
                    em = new ErrorMessage("error.hack");
                    em.setLink("searchfree.do?activemenu=suchenbestellen");
                    rq.setAttribute(Result.ERRORMESSAGE.getValue(), em);
                    LOG.info("modifykontousers: prevented URL-hacking! " + ui.getBenutzer().getEmail());
                }

                // Sicherstellen, dass nur Kunden vom eigenen Konto bearbeitet werden können...
                if (u.getId() == null
                        || vKontoBenutzer.isUserFromKonto(ui.getKonto().getId(), u.getId(), cn.getConnection())) {

                    if (u.getId() != null) {
                        vKontoBenutzer.deleteAllKontoEntries(u, cn.getConnection());
                    }
                    for (final String konto : kontos) {
                        final Konto k = new Konto(Long.parseLong(konto), cn.getConnection());
                        vKontoBenutzer.setKontoUser(u, k, cn.getConnection());
                    }

                    final AbstractBenutzer b = new AbstractBenutzer(uf, cn.getConnection());
                    uf.setUser(b);

                    // d.h. hier wurde ein User angelegt aus der journalorder.jsp
                    // => mit Bestellangaben wieder direkt zurück
                    if (uf.isKeepordervalues2() && (rq.getSession().getAttribute("ofjo") != null)) {
                        uf.setKeepordervalues2(false);
                        forward = "returntojournalorder";
                        OrderForm pageForm = new OrderForm();
                        pageForm = (OrderForm) rq.getSession().getAttribute("ofjo");
                        pageForm.setForuser(uf.getBid().toString()); // Vorselektion neu angelegter User
                        pageForm.setUid(uf.getBid().toString()); // Vorselektion neu angelegter User
                        if (pageForm.getUid() == null) {
                            pageForm.setUid("0");
                        } // sonst kracht es auf jsp
                        rq.setAttribute("ofjo", pageForm);

                        if (pageForm.getOrigin() != null) {
                            forward = "returntojournalsave";
                        }
                        rq.getSession().setAttribute("ofjo", null);
                    }
                    rq.setAttribute("userform", uf);
                }

            } else {
                final ErrorMessage em = new ErrorMessage();
                if (!vorname) {
                    em.setError("error.vorname");
                }
                if (!name) {
                    em.setError("error.name");
                }
                if (!email) {
                    em.setError("error.mail");
                }
                if (!kont) {
                    em.setError("error.kontos");
                }
                if (!land) {
                    em.setError("error.land");
                }
                rq.setAttribute(Result.ERRORMESSAGE.getValue(), em);
                final AbstractBenutzer b = new AbstractBenutzer(uf, cn.getConnection());
                uf.setUser(b);
                rq.setAttribute("userform", uf);
                forward = "missing";
            }

            final Message m = new Message("message.modifyuser");
            m.setLink("listkontousers.do?method=showkontousers&activemenu=bibliokunden");
            rq.setAttribute("message", m);

        } finally {
            cn.close();
        }

        return mp.findForward(forward);
    }

    /**
     * Prepare changing or creating of categories.
     */
    public ActionForward prepareCategories(final ActionMapping mp, final ActionForm fm, final HttpServletRequest rq,
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
        // if activated on system level, access will be restricted to paid only
        if (auth.isPaidOnly(rq)) {
            return mp.findForward(Result.ERROR_PAID_ONLY.getValue());
        }

        final Text cn = new Text();

        try {

            final UserInfo ui = (UserInfo) rq.getSession().getAttribute("userinfo");

            final List<Text> categories = cn.getAllKontoText(TextType.USER_CATEGORY, ui.getKonto().getId(),
                    cn.getConnection());
            // only set into request, if we have at least one category
            if (!categories.isEmpty()) {
                rq.setAttribute("categories", categories);
            }

        } finally {
            cn.close();
        }

        return mp.findForward(Result.SUCCESS.getValue());
    }

    /**
     * Changes a given category.
     */
    public ActionForward changeCategory(final ActionMapping mp, final ActionForm fm, final HttpServletRequest rq,
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
        // if activated on system level, access will be restricted to paid only
        if (auth.isPaidOnly(rq)) {
            return mp.findForward(Result.ERROR_PAID_ONLY.getValue());
        }

        String forward = Result.FAILURE.getValue();
        final Text cn = new Text();

        try {

            final UserInfo ui = (UserInfo) rq.getSession().getAttribute("userinfo");
            final String id = rq.getParameter("id");
            final String sav = rq.getParameter("sav");
            final String del = rq.getParameter("del");
            final String mod = rq.getParameter("mod");
            final String category = rq.getParameter("category");
            boolean save = false;
            boolean delete = false;
            boolean modify = false;

            if ("true".equals(sav)) {
                save = true;
            }
            if ("true".equals(del)) {
                delete = true;
            }
            if ("true".equals(mod)) {
                modify = true;
            }

            if (save && category != null) {
                forward = Result.SUCCESS.getValue();
                // don't save empty string
                if (!"".equals(category) && !" ".equals(category)) {
                    // save new category
                    final Text txt = new Text();
                    txt.setInhalt(category);
                    txt.setTexttype(TextType.USER_CATEGORY);
                    txt.setKonto(ui.getKonto());
                    txt.saveNewText(cn.getConnection(), txt);
                }

                final List<Text> categories = cn.getAllKontoText(TextType.USER_CATEGORY, ui.getKonto().getId(),
                        cn.getConnection());

                // only set into request, if we have at least one category
                if (!categories.isEmpty()) {
                    rq.setAttribute("categories", categories);
                }

            } else {

                if (id != null && StringUtils.isNumeric(id)) {

                    // Make sure the Text() and the category belong to the given account
                    final Text txt = new Text(cn.getConnection(), Long.valueOf(id), ui.getKonto().getId(),
                            TextType.USER_CATEGORY);

                    if (txt.getId() != null) {
                        forward = Result.SUCCESS.getValue();

                        // delete
                        if (delete) {
                            txt.deleteText(cn.getConnection(), txt);
                            // reset all adressen
                            ui.getBenutzer().resetCategories(txt.getId(), cn.getConnection());
                        } else if (modify && category != null) {
                            // update
                            txt.setInhalt(category);
                            txt.updateText(cn.getConnection(), txt);
                        } else {
                            // prepare for update
                            rq.setAttribute("categoryText", txt);
                        }

                        final List<Text> categories = cn.getAllKontoText(TextType.USER_CATEGORY, ui.getKonto().getId(),
                                cn.getConnection());

                        // only set into request, if we have at least one category
                        if (!categories.isEmpty()) {
                            rq.setAttribute("categories", categories);
                        }

                    } else { // URL-hacking
                        final ErrorMessage em = new ErrorMessage("error.hack", "login.do");
                        rq.setAttribute(Result.ERRORMESSAGE.getValue(), em);
                        LOG.info("changeCategory: prevented URL-hacking 2! " + ui.getBenutzer().getEmail());
                    }

                } else { // URL-hacking
                    final ErrorMessage em = new ErrorMessage("error.hack", "login.do");
                    rq.setAttribute(Result.ERRORMESSAGE.getValue(), em);
                    LOG.info("changeCategory: prevented URL-hacking 1! " + ui.getBenutzer().getEmail());
                }

            }

        } finally {
            cn.close();
        }

        return mp.findForward(forward);
    }

    /**
     * User aus Konto löschen,oder gänzlich wenn er keinem Konto mehr angehört
     */
    public ActionForward deleteKontousers(final ActionMapping mp, final ActionForm fm, final HttpServletRequest rq,
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
        // if activated on system level, access will be restricted to paid only
        if (auth.isPaidOnly(rq)) {
            return mp.findForward(Result.ERROR_PAID_ONLY.getValue());
        }

        String forward = Result.FAILURE.getValue();
        final Text cn = new Text();
        final VKontoBenutzer vKontoBenutzer = new VKontoBenutzer();

        try {

            final Benutzer u = new Benutzer();
            final UserForm uf = (UserForm) fm;
            final UserInfo ui = (UserInfo) rq.getSession().getAttribute("userinfo");
            final Konto k = ui.getKonto();
            if (vKontoBenutzer.isUserFromKonto(k.getId(), uf.getBid(), cn.getConnection())) {
                forward = Result.SUCCESS.getValue();

                if (uf.getBid() != null) {
                    u.setId(uf.getBid());
                    final VKontoBenutzer vkb = new VKontoBenutzer();
                    vkb.deleteSingleKontoEntry(u, k, cn.getConnection());
                }

                final List<Konto> rkv = ui.getKonto().getLoginKontos(u, cn.getConnection());
                final AbstractBenutzer b = new AbstractBenutzer();
                if (rkv.isEmpty()) {
                    b.deleteUser(u, cn.getConnection());
                }

                rq.setAttribute("userform", uf);
            } else {
                ErrorMessage em = new ErrorMessage();
                em = new ErrorMessage("error.hack");
                em.setLink("searchfree.do?activemenu=suchenbestellen");
                rq.setAttribute(Result.ERRORMESSAGE.getValue(), em);
                LOG.info("modifykontousers: prevented URL-hacking! " + ui.getBenutzer().getEmail());
            }

            final Message m = new Message("message.deleteuser");
            m.setLink("listkontousers.do?method=showkontousers&activemenu=bibliokunden");
            rq.setAttribute("message", m);

        } finally {
            cn.close();
        }

        return mp.findForward(forward);
    }

    /**
     * Bereitet die Suche nach Bestellungen vor
     */
    public ActionForward prepareSearch(final ActionMapping mp, final ActionForm fm, final HttpServletRequest rq,
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
        // if activated on system level, access will be restricted to paid only
        if (auth.isPaidOnly(rq)) {
            return mp.findForward(Result.ERROR_PAID_ONLY.getValue());
        }

        final Text cn = new Text();

        try {

            final OverviewForm of = (OverviewForm) fm;
            final UserInfo ui = (UserInfo) rq.getSession().getAttribute("userinfo");

            final ActiveMenusForm mf = new ActiveMenusForm();
            mf.setActivemenu(UEBERSICHT);
            rq.setAttribute(Result.ACTIVEMENUS.getValue(), mf);

            final Check check = new Check();
            // angegebener Zeitraum prüfen, resp. Defaultbereich von 3 Monaten zusammenstellen
            check.checkDateRegion(of, 3, ui.getKonto().getTimezone());

            // Suchfelder bestimmen
            final SortedMap<String, String> result = composeSortedLocalisedOrderSearchFields(rq);
            rq.setAttribute("sortedSearchFields", result);

            // Bestellstati
            of.setStatitexts(cn.getAllTextPlusKontoTexts(TextType.STATE_ORDER, ui.getKonto().getId(),
                    cn.getConnection()));
            // Waehrungen
            of.setWaehrungen(cn.getAllTextPlusKontoTexts(TextType.CURRENCY, ui.getKonto().getId(), cn.getConnection()));

            rq.setAttribute("overviewform", of);

        } finally {
            cn.close();
        }

        return mp.findForward(Result.SUCCESS.getValue());
    }

    /**
     * Suche nach Bestellungen
     */
    public ActionForward search(final ActionMapping mp, final ActionForm fm, final HttpServletRequest rq,
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
        // if activated on system level, access will be restricted to paid only
        if (auth.isPaidOnly(rq)) {
            return mp.findForward(Result.ERROR_PAID_ONLY.getValue());
        }

        String forward = Result.FAILURE.getValue();
        final Bestellungen b = new Bestellungen();

        final OverviewForm of = (OverviewForm) fm;
        final UserInfo ui = (UserInfo) rq.getSession().getAttribute("userinfo");
        final Check check = new Check();
        final Text cn = new Text();

        try {

            forward = "result";

            final ActiveMenusForm mf = new ActiveMenusForm();
            mf.setActivemenu(UEBERSICHT);
            rq.setAttribute(Result.ACTIVEMENUS.getValue(), mf);

            // wird für checkFilterCriteriasAgainstAllTextsFromTexttypPlusKontoTexts benötigt
            of.setStatitexts(cn.getAllTextPlusKontoTexts(TextType.STATE_ORDER, ui.getKonto().getId(),
                    cn.getConnection()));

            // angegebener Zeitraum prüfen, resp. Defaultbereich von 3 Monaten zusammenstellen
            check.checkDateRegion(of, 3, ui.getKonto().getTimezone());
            // Ueberprüfung der Sortierkriterien, ob diese gültig sind. Wenn ja, Sortierung anwenden
            check.checkSortOrderValues(of);
            //Check, damit nur gültige Sortierkriterien daherkommen
            check.checkFilterCriteriasAgainstAllTextsFromTexttypPlusKontoTexts(of);
            // Ueberprüfung der Sortierkriterien, ob diese gültig sind. Wenn ja, Sortierung anwenden
            check.checkOrdersSortCriterias(of);

            //          Suchfelder bestimmen
            final SortedMap<String, String> result = composeSortedLocalisedOrderSearchFields(rq);
            rq.setAttribute("sortedSearchFields", result);

            // Bestellstati
            of.setStatitexts(cn.getAllTextPlusKontoTexts(TextType.STATE_ORDER, ui.getKonto().getId(),
                    cn.getConnection()));
            // Waehrungen
            of.setWaehrungen(cn.getAllTextPlusKontoTexts(TextType.CURRENCY, ui.getKonto().getId(), cn.getConnection()));

            final String dateFrom = of.getYfrom() + "-" + of.getMfrom() + "-" + of.getDfrom() + " 00:00:00";
            final String dateTo = of.getYto() + "-" + of.getMto() + "-" + of.getDto() + " 24:00:00";

            List<SearchesForm> searches = new ArrayList<SearchesForm>();

            if (of.isS()) { // Suchkriterien aus Session holen
                searches = ui.getSearches();
            }

            // hier wird ein Array aus den Suchbedingungen aus der JSP zusammengestellt,
            // falls etwas eingegeben wurde...
            // überschreibt Session, falls eine neue Suche eingegeben wurde
            if (checkIfInputIsNotEmpty(of)) {
                if (of.getInput1() != null && !of.getInput1().equals("")) {
                    searches.add(getSearchForm(of.getValue1(), of.getCondition1(), of.getInput1(), of.getBoolean1()));
                }
                if (of.getInput2() != null && !of.getInput2().equals("")) {
                    searches.add(getSearchForm(of.getValue2(), of.getCondition2(), of.getInput2(), of.getBoolean2()));
                }
            }

            if (!searches.isEmpty()) { // Suche, falls etwas im searchesForm liegt...

                //               Suchkriterien in userinfo und Session legen, damit Sortierung funktioniert
                ui.setSearches(searches);
                of.setS(true); // Variable Suche auf true setzen
                rq.getSession().setAttribute("userinfo", ui);

                PreparedStatement pstmt = null;
                try {
                    pstmt = composeSearchLogic(searches, ui.getKonto(), of.getSort(), of.getSortorder(), dateFrom,
                            dateTo, cn.getConnection());
                    of.setBestellungen(b.searchOrdersPerKonto(pstmt, cn.getConnection()));
                } catch (final Exception e) { // Fehler aus Methode abfangen
                    // zusätzliche Ausgabe von Fehlermeldung, falls versucht wurde
                    // Bestellungen nach Kunde > als erlaubter Zeitraum (Datenschutz)
                    forward = Result.FAILURE.getValue();
                    final ErrorMessage em = new ErrorMessage("error.system", "searchorder.do?method=prepareSearch");
                    rq.setAttribute(Result.ERRORMESSAGE.getValue(), em);
                    LOG.error("search: " + e.toString());
                } finally {
                    if (pstmt != null) {
                        try {
                            pstmt.close();
                        } catch (final SQLException e) {
                            LOG.error("search: " + e.toString());
                        }
                    }
                }
            } else {
                of.setS(false); // Suchvariable auf false setzen
            }

            rq.setAttribute("overviewform", of);

        } finally {
            cn.close();
        }

        return mp.findForward(forward);
    }

    /**
     * Stellt den MYSQL für die Suche zusammen / Suchlogik
     */
    public PreparedStatement composeSearchLogic(final List<SearchesForm> searches, final Konto k, final String sort,
            final String sortorder, final String date_from, final String date_to, final Connection cn) throws Exception {

        final Bestellungen b = new Bestellungen();

        final StringBuffer sql = new StringBuffer(500);

        sql.append("SELECT * FROM `bestellungen` AS b INNER JOIN (`benutzer` AS u) ON ( b.UID = u.UID ) "
                + "INNER JOIN (`konto` AS k) ON ( b.KID = k.KID ) WHERE k.kid=? AND (");
        // letzte Klammer wichtig: sonst kann mit OR Bestellungen anderer Kontos ausgelesen werden...!!!

        final int max = searches.size();
        for (int i = 0; i < max; i++) {

            SearchesForm sf = searches.get(i);

            sf = searchMapping(sf); // ggf. Suchwerte in Datenbankwerte übersetzen...

            // Suche zusammenstellen
            if (composeSearchLogicTable(sf.getField(), sf.getCondition()).contains("MATCH (artikeltitel,autor,")) {
                sql.append(composeSearchLogicTable(sf.getField(), sf.getCondition()));
                sql.append("AGAINST (? IN BOOLEAN MODE) ");
            } else {
                sql.append('`');
                sql.append(composeSearchLogicTable(sf.getField(), sf.getCondition()));
                sql.append("`\040");
                sql.append(composeSearchLogicCondition(sf.getCondition()));
                sql.append("\040?\040");
            }

            // Boolsche-Verknüpfung anhängen solange noch weiter Abfragen kommen...
            if (i + 1 < max) {
                sql.append(composeSearchLogicBoolean(sf.getBool()));
                sql.append('\040');
            }

        }

        // erste Klammer wichtig: sonst kann mit OR Bestellungen anderer Kontos ausgelesen werden...!!!
        sql.append(") AND orderdate >= '");
        sql.append(date_from);
        sql.append("' AND orderdate <= '");
        sql.append(date_to);
        sql.append("' ORDER BY ");
        b.sortOrder(sql, sort, sortorder);

        PreparedStatement pstmt = cn.prepareStatement(sql.toString());
        pstmt.setLong(1, k.getId());

        // Stops the search if the allowed search range for name, first name, email,
        // remarks is exceeded
        boolean stop = false;
        for (int i = 0; i < max && !stop; i++) {

            final SearchesForm sf = searches.get(i);

            if (composeSearchLogicTable(sf.getField(), sf.getCondition()).equals("name")
                    || composeSearchLogicTable(sf.getField(), sf.getCondition()).equals("vorname")
                    || composeSearchLogicTable(sf.getField(), sf.getCondition()).equals("mail")
                    || composeSearchLogicTable(sf.getField(), sf.getCondition()).equals("systembemerkung")) {
                if (checkAnonymize(date_from, k.getTimezone())) {
                    stop = true;
                    pstmt = null;
                }
                if (pstmt == null) {
                    throw new Exception(
                            "Data privacy: exceeded allowed search range for name, firts name, email, remarks");
                }
            }

            String truncation = ""; // Normalerweise keine Trunkierung
            if (sf.getCondition().contains("contains")) {
                truncation = "%";
            } // Trunkierung für LIKE

            if (composeSearchLogicTable(sf.getField(), sf.getCondition()).contains("MATCH (artikeltitel,autor,")) {
                // Suche für IN BOOLEAN MODE zusammenstellen
                pstmt.setString(2 + i, composeSearchInBooleanMode(sf.getInput()));
            } else {
                pstmt.setString(2 + i, truncation + sf.getInput() + truncation);
            }
        }

        return pstmt;

    }

    /**
     * Übersetzt die Suchfelder in die korrekten MYSQL Table-Felder / Suchlogik
     */
    private String composeSearchLogicTable(final String field, final String condition) {

        String table = "";

        if (checkIfSearchFieldIsValid(field)) {

            // Suche in einzelnen Feldern
            if ("searchorders.artikeltitel".equals(field)) {
                table = "artikeltitel";
            }
            if ("searchorders.author".equals(field)) {
                table = "autor";
            }
            if ("searchorders.bemerkungen".equals(field)) {
                table = "systembemerkung";
            }
            if ("searchorders.delformat".equals(field)) {
                table = "fileformat";
            }
            if ("searchorders.heft".equals(field)) {
                table = "heft";
            }
            if ("searchorders.notizen".equals(field)) {
                table = "notizen";
            }
            if ("searchorders.issn".equals(field)) {
                table = "issn";
            }
            if ("searchorders.jahr".equals(field)) {
                table = "jahr";
            }
            if ("searchorders.jahrgang".equals(field)) {
                table = "jahrgang";
            }
            if ("searchorders.gender".equals(field)) {
                table = "anrede";
            }
            if ("searchorders.email".equals(field)) {
                table = "mail";
            }
            if ("searchorders.institut".equals(field)) {
                table = "institut";
            }
            if ("searchorders.department".equals(field)) {
                table = "abteilung";
            }
            if ("searchorders.name".equals(field)) {
                table = "name";
            }
            if ("searchorders.vorname".equals(field)) {
                table = "vorname";
            }
            if ("searchorders.supplier".equals(field)) {
                table = "bestellquelle";
            }
            if ("searchorders.deliveryway".equals(field)) {
                table = "deloptions";
            }
            if ("searchorders.prio".equals(field)) {
                table = "orderpriority";
            }
            if ("searchorders.seiten".equals(field)) {
                table = "seiten";
            }
            if ("searchorders.subitonr".equals(field)) {
                table = "subitonr";
            }
            if ("searchorders.gbvnr".equals(field)) {
                table = "gbvnr";
            }
            if ("searchorders.internenr".equals(field)) {
                table = "internenr";
            }
            if ("searchorders.zeitschrift".equals(field)) {
                table = "zeitschrift";
            }
            if ("searchorders.doi".equals(field)) {
                table = "doi";
            }
            if ("searchorders.pmid".equals(field)) {
                table = "pmid";
            }
            if ("searchorders.isbn".equals(field)) {
                table = "isbn";
            }
            if ("searchorders.typ".equals(field)) {
                table = "mediatype";
            }
            if ("searchorders.verlag".equals(field)) {
                table = "verlag";
            }
            if ("searchorders.buchkapitel".equals(field)) {
                table = "buchkapitel";
            }
            if ("searchorders.buchtitel".equals(field)) {
                table = "buchtitel";
            }

        } else { // Suche nach allen Feldern
            table = "MATCH (artikeltitel,autor,fileformat,heft,notizen,issn,heft,jahr,bestellquelle,"
                    + "jahrgang,bestellquelle,deloptions,seiten,subitonr,gbvnr,internenr,zeitschrift,doi,"
                    + "pmid,isbn,mediatype,verlag,buchkapitel,buchtitel) ";
            // hier wird die Suche von AND zu AND NOT umgekehrt
            if ("contains not".equals(condition) || "is not".equals(condition)) {
                table = "NOT MATCH (artikeltitel,autor,fileformat,heft,notizen,issn,heft,jahr,bestellquelle,"
                        + "jahrgang,bestellquelle,deloptions,seiten,subitonr,gbvnr,internenr,zeitschrift,doi,pmid,"
                        + "isbn,mediatype,verlag,buchkapitel,buchtitel) ";
            }
        }

        return table;
    }

    /**
     * Übersetzt die Bedingungen in die korrekten MYSQL-Begriffe / Suchlogik
     */
    private String composeSearchLogicCondition(final String condition) {

        String expression = "";

        if ("contains".equals(condition) || "contains not".equals(condition) || "is".equals(condition)
                || "is not".equals(condition) || "higher".equals(condition) || "smaller".equals(condition)) {

            if ("contains".equals(condition)) {
                expression = "LIKE";
            }
            if ("contains not".equals(condition)) {
                expression = "NOT LIKE";
            }
            if ("is".equals(condition)) {
                expression = "=";
            }
            if ("is not".equals(condition)) {
                expression = "!=";
            }
            if ("higher".equals(condition)) {
                expression = ">";
            }
            if ("smaller".equals(condition)) {
                expression = "<";
            }

        } else {
            expression = "LIKE";
        }

        return expression;
    }

    /**
     * Übersetzt die Verknüpfung in die korrekten MYSQL-Begriffe / Suchlogik
     */
    private String composeSearchLogicBoolean(final String bool) {

        String expression = "";

        if ("and".equals(bool) || "and not".equals(bool) || "or".equals(bool)) {

            if ("and".equals(bool)) {
                expression = "AND";
            }
            if ("and not".equals(bool)) {
                expression = "AND NOT";
            }
            if ("or".equals(bool)) {
                expression = "OR";
            }
        } else {
            expression = "AND";
        }

        return expression;
    }

    /**
     * Liefert eine sprachabhängig sortierte Liste an Suchfeldern für den Select
     * 
     * @param HttpServletRequest rq
     * @return Map<String, String>
     */
    private SortedMap<String, String> composeSortedLocalisedOrderSearchFields(final HttpServletRequest rq) {

        final List<String> searchFields = prepareOrderSearchFields();
        final SortedMap<String, String> result = new TreeMap<String, String>();
        //        final Locale locale = getLocale(rq);
        final Locale locale = RequestUtils.getUserLocale(rq, null);
        final MessageResources msgs = getResources(rq);
        String key = null;
        String value = null;
        for (final String searchField : searchFields) {
            value = "searchorders." + searchField;
            key = msgs.getMessage(locale, value);
            result.put(key, value);
        }

        return result;
    }

    /**
     * Ersetzt Anzeige Werte in die korrekten in der Datenbank gespeicherten
     * Werte
     */
    private SearchesForm searchMapping(final SearchesForm sf) {

        //       Mapping bei Prio => express = urgent
        if (sf.getField().equals("Priorität") && sf.getInput().equalsIgnoreCase("express")) {
            sf.setInput("urgent");
        }
        if (sf.getField().equals("Lieferant") && sf.getInput().equalsIgnoreCase("k.A.")) {
            sf.setInput("0");
        }

        return sf;
    }

    /**
     * Legt die durchsuchbaren Felder im Select fest. Enthält den hinteren Teil
     * der Einträge im Properties-File z.B. all => searchorders.all=...
     */
    private List<String> prepareOrderSearchFields() {

        final List<String> list = new ArrayList<String>();

        // the param "all" is hardcoded on the JSP, to ensure he is always on top, language independent
        //        list.add("all");
        list.add("department");
        list.add("gender");
        list.add("artikeltitel");
        list.add("author");
        list.add("bemerkungen");
        list.add("buchkapitel");
        list.add("buchtitel");
        list.add("delformat");
        list.add("doi");
        list.add("email");
        list.add("gbvnr");
        list.add("heft");
        list.add("institut");
        list.add("internenr");
        list.add("notizen");
        list.add("isbn");
        list.add("issn");
        list.add("jahr");
        list.add("jahrgang");
        list.add("supplier");
        list.add("deliveryway");
        list.add("name");
        list.add("pmid");
        list.add("prio");
        list.add("seiten");
        list.add("subitonr");
        list.add("typ");
        list.add("verlag");
        list.add("vorname");
        list.add("zeitschrift");

        return list;
    }

    /**
     * setzt die korrekten Plus- oder Minus-Zeichen vor die Begriffe für
     * "IN BOOLEAN MODE"
     */
    private String composeSearchInBooleanMode(String input) {

        if (input != null) {

            final Check check = new Check();

            // nur Buchstaben inkl. Umlaute und Zahlen zugelassen. Keine Sonderzeichen wie ";.-?!" etc.
            final List<String> words = check.getAlphanumericWordCharacters(input);
            final StringBuffer buf = new StringBuffer();
            buf.append("");

            int i = 0;
            for (final String word : words) {
                i++;
                // Ausschluss von Leerzeichen und Wörten kürzer als drei Buchstaben (min. 4 Buchstaben)
                if (word.length() > 3) {
                    buf.append("+" + word + "*"); // Jedem Wort ein Plus-Zeichen voranstellen, und Trunkierung anhängen
                    if (i < words.size()) {
                        buf.append('\040');
                    } // ggf. ein Leerschlag dazwischen setzen
                }
            }

            input = buf.toString().trim(); // ggf. überschüssigen Leerschlag entfernen...

        }

        return input;
    }

    /**
     * Prüft ob Suchwerte eingegeben wurden
     */
    private boolean checkIfInputIsNotEmpty(final OverviewForm of) {

        boolean check = false;

        if ((of.getInput1() != null && !of.getInput1().equals(""))
                || (of.getInput2() != null && !of.getInput2().equals(""))) {
            check = true;
        }

        return check;
    }

    /**
     * Prüft ob aus der Anzeige zulässige Suchfelder übermittelt werden
     * 
     * @param String input
     * @return boolean
     */
    private boolean checkIfSearchFieldIsValid(final String input) {

        boolean check = false;

        if (input != null && !input.equals("searchorders.all")) {

            final List<String> searchFields = prepareOrderSearchFields();

            for (final String searchField : searchFields) {
                final String compare = "searchorders." + searchField;
                if (input.equals(compare)) {
                    check = true;
                    break;
                }
            }
        }

        return check;
    }

    /**
     * Data privacy: checks if the allowed range is exceeded. <p></p>
     * 
     * @param Strinf date_from
     * @return true/false
     */
    private boolean checkAnonymize(final String date_from, final String timezone) {
        boolean check = false;

        if (date_from != null && ReadSystemConfigurations.isAnonymizationActivated()) {
            final Bestellungen b = new Bestellungen();
            final Calendar cal = b.stringFromMysqlToCal(date_from);
            final Calendar limit = Calendar.getInstance();
            limit.setTimeZone(TimeZone.getTimeZone(timezone));
            limit.add(Calendar.MONTH, -ReadSystemConfigurations.getAnonymizationAfterMonths());
            limit.add(Calendar.DAY_OF_MONTH, -1);
            if (cal.before(limit)) {
                check = true;
            }
        }

        return check;
    }

    /**
     * stellt ein SearchForm zusammen
     */
    private SearchesForm getSearchForm(final String field, final String condition, final String input, final String bool) {

        final SearchesForm sf = new SearchesForm();
        sf.setField(field);
        sf.setCondition(condition);
        sf.setInput(input);
        sf.setBool(bool);

        return sf;
    }

}
