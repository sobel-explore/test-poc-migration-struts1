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

import java.util.Date;
import java.util.List;
import java.util.SortedSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.mail.internet.InternetAddress;
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
import util.Encrypt;
import util.MHelper;
import util.ReadSystemConfigurations;
import util.ThreadSafeSimpleDateFormat;
import util.ThreadedWebcontent;
import util.TimeZones;
import ch.dbs.admin.KontoAdmin;
import ch.dbs.entity.AbstractBenutzer;
import ch.dbs.entity.Bibliothekar;
import ch.dbs.entity.Countries;
import ch.dbs.entity.Konto;
import ch.dbs.entity.Text;
import ch.dbs.entity.VKontoBenutzer;
import ch.dbs.form.ActiveMenusForm;
import ch.dbs.form.BillingForm;
import ch.dbs.form.ErrorMessage;
import ch.dbs.form.KontoForm;
import ch.dbs.form.LoginForm;
import ch.dbs.form.Message;
import ch.dbs.form.UserForm;
import ch.dbs.form.UserInfo;
import enums.Result;

public final class KontoAction extends DispatchAction {

    private static final Logger LOG = LoggerFactory.getLogger(KontoAction.class);
    // This link is used to check if, the GBV credentials are valid
    private static final String GBV_LINK = "http://gso.gbv.de/login/FORM/REQUEST?DBS_ID=2.1&DB=2.1&USER_KEY=";
    private static final String GBV_REDIRECT = "&REDIRECT=http%3A%2F%2Fgso.gbv.de%2Frequest%2FFORCETT%3DHTML%2FDB%3D2.1%2FFORM%2FCOPY%3FPPN%3D185280552%26LANGCODE%3DDU";
    private static final String TIMEZONES = "timezones";

    /**
     * Hinzufügen eines neuen Benutzers vorbereiten
     */
    public ActionForward prepareNewKonto(final ActionMapping mp, final ActionForm form, final HttpServletRequest rq,
            final HttpServletResponse rp) {

        String forward = Result.FAILURE.getValue();
        final Text cn = new Text();

        try {

            final TimeZones tz = new TimeZones();
            final SortedSet<String> setTZ = tz.getTimeZonesAsString();
            rq.setAttribute(TIMEZONES, setTZ);

            final KontoForm kf = new KontoForm();
            final Countries country = new Countries();

            final List<Countries> allPossCountries = country.getAllCountries(cn.getConnection());
            kf.setCountries(allPossCountries);
            forward = Result.SUCCESS.getValue();
            rq.setAttribute("kontoform", kf);

            //Navigation: Tab Neues Konto aktiv schalten
            final ActiveMenusForm mf = new ActiveMenusForm();
            mf.setActivemenu("newkonto");
            rq.setAttribute(Result.ACTIVEMENUS.getValue(), mf);

        } finally {
            cn.close();
        }

        return mp.findForward(forward);
    }

    /**
     * neues Konto anlegen
     */
    public ActionForward addNewKonto(final ActionMapping mp, final ActionForm fm, final HttpServletRequest rq,
            final HttpServletResponse rp) {

        final ActiveMenusForm mf = new ActiveMenusForm();
        mf.setActivemenu("newkonto");
        rq.setAttribute(Result.ACTIVEMENUS.getValue(), mf);

        String forward = Result.SUCCESS.getValue();
        final KontoForm kf = (KontoForm) fm;
        final Konto k = new Konto(kf); //Konto erstellen und mit Values aus KontoForm abfuellen
        final Check check = new Check();
        final Text cn = new Text();
        final Countries country = new Countries();

        // GBV-Thread-Management
        final ThreadedWebcontent gbvthread = new ThreadedWebcontent();
        final ExecutorService executor = Executors.newCachedThreadPool();
        Future<String> gbvcontent = null;

        try {

            if (ReadSystemConfigurations.isAllowRegisterLibraryAccounts()) {

                // ggf. Leerschläge entfernen
                if (k.getBibliotheksmail() != null) {
                    k.setBibliotheksmail(k.getBibliotheksmail().trim());
                }
                if (k.getDbsmail() != null) {
                    k.setDbsmail(k.getDbsmail().trim());
                }

                //Angaben rudimentaer Validieren
                if (check.isMinLength(k.getBibliotheksname(), 1) && check.isMinLength(k.getAdresse(), 1)
                        && check.isMinLength(k.getPLZ(), 4) && check.isMinLength(k.getOrt(), 1)
                        && check.isMinLength(k.getLand(), 2) && check.isMinLength(k.getTelefon(), 4)
                        && check.isEmail(k.getBibliotheksmail()) && check.isEmail(k.getDbsmail())) {

                    // mit einem neuen Thread ist hier die Timeout-Kontrolle besser
                    if (kf.getGbvbenutzername() != null && !kf.getGbvbenutzername().equals("")) {
                        final StringBuffer gbvLink = new StringBuffer(GBV_LINK);
                        gbvLink.append(kf.getGbvbenutzername());
                        gbvLink.append("&PASSWORD=");
                        gbvLink.append(kf.getGbvpasswort());
                        gbvLink.append(GBV_REDIRECT);
                        gbvthread.setLink(gbvLink.toString());
                        gbvcontent = executor.submit(gbvthread);
                    }

                    // grundsätzlich auf true stellen. Wird erst geprüft, wenn tatsächlich ein Benutzername eingegeben wird
                    boolean checkgbv = true;
                    // falls beim GBV ein Benutzername angegeben wurde
                    if (kf.getGbvbenutzername() != null && !kf.getGbvbenutzername().equals("")) {
                        try {
                            checkgbv = false;
                            String gbvanswer = "";
                            gbvanswer = gbvcontent.get(3, TimeUnit.SECONDS);
                            if (gbvanswer.contains("Abmelden")) {
                                checkgbv = true;
                            } else {
                                forward = "subitofailed"; // Pfad i.O.
                                kf.setMessage("error.gbv_values");
                                final TimeZones tz = new TimeZones();
                                final SortedSet<String> setTZ = tz.getTimeZonesAsString();
                                rq.setAttribute(TIMEZONES, setTZ);
                                final List<Countries> allPossCountries = country.getAllCountries(cn.getConnection());
                                kf.setCountries(allPossCountries);
                                rq.setAttribute("kontoform", kf);
                            }
                        } catch (final TimeoutException e) {
                            LOG.info("GBV-Timeout: " + e.toString());
                            forward = "subitofailed"; // Pfad i.O.
                            kf.setMessage("error.gbv_timeout");
                            final TimeZones tz = new TimeZones();
                            final SortedSet<String> setTZ = tz.getTimeZonesAsString();
                            rq.setAttribute(TIMEZONES, setTZ);
                            final List<Countries> allPossCountries = country.getAllCountries(cn.getConnection());
                            kf.setCountries(allPossCountries);
                            rq.setAttribute("kontoform", kf);
                        } catch (final Exception e) {
                            LOG.error("GBV-Thread failed in modifykonto: " + e.toString());
                        } finally {
                            // ungefährlich, falls der Task schon beendet ist.
                            // Stellt sicher, dass nicht noch unnötige Ressourcen belegt werden
                            gbvcontent.cancel(true);
                        }

                    }

                    if (checkgbv) {

                        kf.setKonto(k); // Konto in Kontoform schreiben
                        rq.getSession().setAttribute("kontoform", kf); // Konto in Kontoform in Session legen

                    } // benötigt keine separate else-Schlaufe, da der Negativfall schon oben behandelt wurde

                } else {
                    forward = "subitofailed";
                    kf.setMessage("error.values");

                    final TimeZones tz = new TimeZones();
                    final SortedSet<String> setTZ = tz.getTimeZonesAsString();
                    rq.setAttribute(TIMEZONES, setTZ);

                    final List<Countries> allPossCountries = country.getAllCountries(cn.getConnection());
                    kf.setCountries(allPossCountries);

                    rq.setAttribute("kontoform", kf);
                }

            } else {
                forward = "subitofailed";
                kf.setMessage("error.berechtigung");

                final TimeZones tz = new TimeZones();
                final SortedSet<String> setTZ = tz.getTimeZonesAsString();
                rq.setAttribute(TIMEZONES, setTZ);

                final List<Countries> allPossCountries = country.getAllCountries(cn.getConnection());
                kf.setCountries(allPossCountries);

                rq.setAttribute("kontoform", kf);
            }

        } finally {
            cn.close();
        }

        return mp.findForward(forward);
    }

    /**
     * Neuen Bibliothekar anlegen und mit Konto verknüpfen
     */
    public ActionForward addNewBibliothekar(final ActionMapping mp, final ActionForm fm, final HttpServletRequest rq,
            final HttpServletResponse rp) {

        String forward = Result.FAILURE.getValue();
        final Check check = new Check();
        final Bibliothekar u = new Bibliothekar();
        final UserForm uf = (UserForm) fm;
        final Text cn = new Text();
        final Auth auth = new Auth();

        final ActiveMenusForm mf = new ActiveMenusForm();
        mf.setActivemenu("newkonto");
        rq.setAttribute(Result.ACTIVEMENUS.getValue(), mf);

        try {

            // Seite kann nur aufgerufen werden, wenn vorher ein Konto angelegt wurde und kein userinfo im rq ist...
            if (auth.isKontoform(rq) && ReadSystemConfigurations.isAllowRegisterLibraryAccounts()) {

                final KontoForm kf = (KontoForm) rq.getSession().getAttribute("kontoform"); // Konto aus Kontoform holen
                final Konto k = new Konto(kf);

                if (kf.getEzbid().contains("bibid=")) { // Prüfung, ob EZB-ID vorhanden
                    final String ezb = kf.getEzbid().substring(kf.getEzbid().indexOf("bibid=") + 6);
                    if (ezb.contains("&")) { // falls EZB-ID mitten im String
                        k.setEzbid(ezb.substring(0, ezb.indexOf('&')));
                    } else { // falls EZB-ID am Ende des Strings
                        k.setEzbid(ezb);
                    }
                } else { // falls keine gültige URL angegeben wurde
                    k.setEzbid("");
                }

                // ggf. Leerschläge entfernen
                if (uf.getEmail() != null) {
                    uf.setEmail(uf.getEmail().trim());
                }

                //TODO: Korrekte Fehlermeldunen an Benutzer ausgeben.
                // Minimale länge wird nicht als korrekter Fehler zurückgemeldet
                if (check.isMinLength(uf.getName(), 2) && check.isMinLength(uf.getVorname(), 2)
                        && check.isEmail(uf.getEmail()) && check.isMinLength(uf.getPassword(), 6)) {

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
                    u.setLoginopt(true); // Login erlaubt
                    u.setKontostatus(true); // Userkonto aktiv
                    final Encrypt e = new Encrypt();
                    u.setPassword(e.makeSHA(uf.getPassword()));
                    u.setUserbestellung(true); // Bestellungen bei Subito erlaubt
                    u.setGbvbestellung(true); // Bestellungen beim GBV erlaubt
                    u.setKontovalidation(uf.isKontovalidation());
                    u.setRechte(2); // Berechtigung Bibliothekar

                    final Date d = new Date();
                    final ThreadSafeSimpleDateFormat fmt = new ThreadSafeSimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    final String datum = fmt.format(d, k.getTimezone());
                    u.setDatum(datum);

                    // Konto abspeichern und wieder holen wegen KID
                    k.setId(k.save(cn.getConnection()));
                    final AbstractBenutzer b = new AbstractBenutzer();
                    b.saveNewUser(u, k.getTimezone(), cn.getConnection());
                    final VKontoBenutzer vKontoBenutzer = new VKontoBenutzer();
                    vKontoBenutzer.setKontoBibliothekar(u, k, cn.getConnection());
                    forward = "weiter";

                    // Nachricht über neues Konto abschicken
                    final StringBuffer message = new StringBuffer(272);
                    message.append("There has been registered a new library account in ");
                    message.append(ReadSystemConfigurations.getApplicationName());
                    message.append("\n\nLibrary account: ");
                    message.append(k.getBibliotheksname());
                    message.append("\nCity: ");
                    message.append(k.getOrt());
                    message.append("\nCountry: ");
                    message.append(k.getLand());
                    message.append("\nLibrary email: ");
                    message.append(k.getBibliotheksmail());
                    message.append("\nType of account (0 = free | 1 = 1 year enhanced | 2 = 1 year faxserver | "
                            + "3 = 3 months faxserver): ");
                    message.append(k.getKontotyp());
                    message.append("\nLibrarian: ");
                    message.append(u.getVorname());
                    message.append('\040');
                    message.append(u.getName());
                    message.append("\nLibrarian email: ");
                    message.append(u.getEmail());
                    final InternetAddress[] to = new InternetAddress[1];
                    to[0] = new InternetAddress(ReadSystemConfigurations.getSystemEmail());
                    final MHelper mh = new MHelper(to, "New library account!", message.toString());
                    mh.send();

                    // Bestätigungsemail mit Angaben zu den nächsten Schritten und Möglichkeiten
                    final StringBuffer mg = new StringBuffer(400);
                    mg.append("Dear\040");
                    if (u.getAnrede().equals("Frau")) {
                        mg.append("Ms\040");
                        mg.append(u.getVorname());
                        mg.append('\040');
                        mg.append(u.getName());
                    }
                    if (u.getAnrede().equals("Herr")) {
                        mg.append("Mr\040");
                        mg.append(u.getVorname());
                        mg.append('\040');
                        mg.append(u.getName());
                    }
                    mg.append("\n\nWelcome at ");
                    mg.append(ReadSystemConfigurations.getApplicationName());
                    mg.append("!\n\nTo use the IP based oderform for your patrons within your institution, register your "
                            + "IP: Log in => Account => IP ranges");
                    mg.append("\n\nThis link will show you your IP: http://www.whatismyip.com (if your institution uses "
                            + "an IP range instead of a single IP, ask your IT)\n\nCheck out the How-To to use ");
                    mg.append(ReadSystemConfigurations.getApplicationName());
                    mg.append(" as a linkresolver (in connection with the services of EZB/ZDB): ");
                    mg.append(ReadSystemConfigurations.getServerInstallation());
                    mg.append("/howto_openurl.do\012\012"
                            + "Your team consists of several librarians and you want each one of them to have their "
                            + "own ID + PW? Create their accounts as normal patrons and contact us which addresses "
                            + "should be granted as librarians.\012\012");
                    if (k.getKontotyp() != 0) {
                        mg.append("Thank you for choosing the option \"Fax to PDF\". We'll "
                                + "register you for this service and we will contact you with the details as soon as "
                                + "possible.\012\012");
                    }
                    mg.append("We hope you enjoy using ");
                    mg.append(ReadSystemConfigurations.getApplicationName());
                    mg.append("!\n\nGet in contact with us by using our mailing list if you have any questions: "
                            + "https://lists.sourceforge.net/lists/listinfo/doctor-doc-general\012\012"
                            + "Best regards\012Your Team\040");
                    mg.append(ReadSystemConfigurations.getApplicationName());
                    final InternetAddress[] sendto = new InternetAddress[1];
                    sendto[0] = new InternetAddress(u.getEmail());
                    final MHelper mailh = new MHelper(sendto, "Your account at "
                            + ReadSystemConfigurations.getApplicationName(), mg.toString());
                    mailh.send();

                    // Kontoform in Session leeren
                    rq.getSession().setAttribute("kontoform", null);

                    final LoginForm lf = new LoginForm();
                    lf.setEmail(uf.getEmail());
                    lf.setPassword(uf.getPassword());

                    rq.setAttribute("loginform", lf); // um Kunde nach Registration gleich einzuloggen

                } else {
                    final ErrorMessage em = new ErrorMessage("error.values");
                    rq.setAttribute(Result.ERRORMESSAGE.getValue(), em);
                    forward = "userfailed";
                    rq.setAttribute("kontoform", kf);
                    rq.setAttribute("userform", uf);
                }

            } else {
                final ErrorMessage em = new ErrorMessage("error.berechtigung");
                em.setLink("anmeldungkonto.do");
                rq.setAttribute(Result.ERRORMESSAGE.getValue(), em);
            }

        } catch (final Exception e) {
            LOG.error(e.toString());
            final ErrorMessage em = new ErrorMessage("error.system", "login.do");
            rq.setAttribute(Result.ERRORMESSAGE.getValue(), em);
            forward = Result.FAILURE.getValue();
        } finally {
            cn.close();
        }

        return mp.findForward(forward);
    }

    /**
     * ändern eines Kontos vorbereiten
     */
    public ActionForward prepareModifyKonto(final ActionMapping mp, final ActionForm fm, final HttpServletRequest rq,
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
        final KontoForm kf = (KontoForm) fm;
        final Text cn = new Text();
        final Countries country = new Countries();

        try {

            final TimeZones tz = new TimeZones();
            final SortedSet<String> setTZ = tz.getTimeZonesAsString();
            rq.setAttribute(TIMEZONES, setTZ);

            final Konto k = new Konto(ui.getKonto().getId(), cn.getConnection());

            forward = Result.SUCCESS.getValue();

            if (auth.isAdmin(rq)) {
                kf.setFaxusername(k.getFaxusername());
                kf.setFaxpassword(k.getFaxpassword());
                kf.setGbvrequesterid(k.getGbvrequesterid());
            }

            kf.setKonto(k);
            kf.setValuesFromKonto();

            final List<Countries> allPossCountries = country.getAllCountries(cn.getConnection());
            kf.setCountries(allPossCountries);

            rq.setAttribute("kontoform", kf);

        } finally {
            cn.close();
        }

        return mp.findForward(forward);
    }

    /**
     * Kontoangaben ändern
     */
    public ActionForward modifyKonto(final ActionMapping mp, final ActionForm fm, final HttpServletRequest rq,
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
        final KontoForm kf = (KontoForm) fm;
        final Check check = new Check();
        final Text cn = new Text();
        final Countries country = new Countries();
        // GBV-Thread-Management
        final ThreadedWebcontent gbvthread = new ThreadedWebcontent();
        final ExecutorService executor = Executors.newCachedThreadPool();
        Future<String> gbvcontent = null;

        try {

            // ggf. Leerschläge entfernen
            if (kf.getBibliotheksmail() != null) {
                kf.setBibliotheksmail(kf.getBibliotheksmail().trim());
            }
            if (kf.getDbsmail() != null) {
                kf.setDbsmail(kf.getDbsmail().trim());
            }

            if (check.isMinLength(kf.getBiblioname(), 1) && check.isMinLength(kf.getAdresse(), 1)
                    && check.isMinLength(kf.getPLZ(), 4) && check.isMinLength(kf.getOrt(), 1)
                    && check.isMinLength(kf.getLand(), 2) && check.isMinLength(kf.getTelefon(), 4)
                    && check.isEmail(kf.getBibliotheksmail()) && check.isEmail(kf.getDbsmail())) {

                final String[] allowedFiletypes = { "jpg", "jpeg", "gif", "png" };

                if (kf.getInstlogolink() == null || kf.getInstlogolink().equals("") || // no entry is valid
                        check.isUrlAndFiletype(kf.getInstlogolink(), allowedFiletypes)) {

                    // mit einem neuen Thread ist hier die Timeout-Kontrolle besser
                    if (kf.getGbvbenutzername() != null && !kf.getGbvbenutzername().equals("")) {
                        final StringBuffer gbvLink = new StringBuffer(GBV_LINK);
                        gbvLink.append(kf.getGbvbenutzername());
                        gbvLink.append("&PASSWORD=");
                        gbvLink.append(kf.getGbvpasswort());
                        gbvLink.append(GBV_REDIRECT);
                        gbvthread.setLink(gbvLink.toString());
                        gbvcontent = executor.submit(gbvthread);
                    }

                    // default true: Wird erst geprüft, wenn tatsächlich ein Benutzername eingegeben wird
                    boolean checkgbv = true;
                    // falls beim GBV ein Benutzername angegeben wurde
                    if (kf.getGbvbenutzername() != null && !kf.getGbvbenutzername().equals("")) {
                        try {
                            checkgbv = false;
                            String gbvanswer = "";
                            gbvanswer = gbvcontent.get(3, TimeUnit.SECONDS);
                            if (!gbvanswer.contains("Bitte identifizieren Sie sich")) {
                                checkgbv = true;
                            } else {
                                forward = "subitofailed"; // Pfad i.O.
                                kf.setMessage("error.gbv_values");
                                final TimeZones tz = new TimeZones();
                                final SortedSet<String> setTZ = tz.getTimeZonesAsString();
                                rq.setAttribute(TIMEZONES, setTZ);
                                final List<Countries> allPossCountries = country.getAllCountries(cn.getConnection());
                                kf.setCountries(allPossCountries);
                            }
                        } catch (final TimeoutException e) {
                            LOG.info("GBV-Timeout: " + e.toString());
                            forward = "subitofailed"; // Pfad i.O.
                            kf.setMessage("error.gbv_timeout");
                            final TimeZones tz = new TimeZones();
                            final SortedSet<String> setTZ = tz.getTimeZonesAsString();
                            rq.setAttribute(TIMEZONES, setTZ);
                            final List<Countries> allPossCountries = country.getAllCountries(cn.getConnection());
                            kf.setCountries(allPossCountries);
                        } catch (final Exception e) {
                            LOG.error("GBV-Thread failed in modifykonto: " + e.toString());
                        } finally {
                            // ungefährlich, falls der Task schon beendet ist.
                            // Stellt sicher, dass nicht noch unnötige Ressourcen belegt werden
                            gbvcontent.cancel(true);
                        }

                    }

                    if (checkgbv) {

                        forward = Result.SUCCESS.getValue();

                        final Konto k = new Konto(ui.getKonto().getId(), cn.getConnection());

                        if (auth.isAdmin(rq)) {
                            // Faxno == null => Deaktivierung von Faxserver-Cronjob
                            if (kf.getFaxno().equals("")) {
                                k.setFaxno(null);
                            } else {
                                k.setFaxno(kf.getFaxno());
                            }
                            k.setFaxusername(kf.getFaxusername()); // nur Admin
                            k.setFaxpassword(kf.getFaxpassword()); // nur Admin
                            k.setGbvrequesterid(kf.getGbvrequesterid()); // nur Admin
                        }

                        if (kf.getGbvbenutzername() == null || kf.getGbvbenutzername().equals("")) {
                            k.setGbvbenutzername(null);
                        } else {
                            k.setGbvbenutzername(kf.getGbvbenutzername().trim());
                        } // null wegen Anzeigelogik
                        if (kf.getGbvpasswort() == null || kf.getGbvpasswort().equals("")) {
                            k.setGbvpasswort(null);
                        } else {
                            k.setGbvpasswort(kf.getGbvpasswort().trim());
                        } // null wegen Anzeigelogik
                        if (kf.getIdsid() == null || kf.getIdsid().equals("")) {
                            k.setIdsid(null);
                        } else {
                            k.setIdsid(kf.getIdsid().trim());
                        }
                        if (kf.getIdspasswort() == null || kf.getIdspasswort().equals("")) {
                            k.setIdspasswort(null);
                        } else {
                            k.setIdspasswort(kf.getIdspasswort().trim());
                        }
                        if (kf.getBiblioname() != null) {
                            k.setBibliotheksname(kf.getBiblioname().trim());
                        } else {
                            k.setBibliotheksname(kf.getBiblioname());
                        }
                        // if (kf.getIsil() != null) { k.setIsil(correctIsil(kf.getIsil().trim())); } else {
                        // k.setIsil(kf.getIsil()); }// Sollte nicht mehr nötig sein da bereits in Formbean
                        // abgehandelt
                        k.setIsil(kf.getIsil());
                        if (kf.getAdresse() != null) {
                            k.setAdresse(kf.getAdresse().trim());
                        } else {
                            k.setAdresse(kf.getAdresse());
                        }
                        if (kf.getAdressenzusatz() != null) {
                            k.setAdressenzusatz(kf.getAdressenzusatz().trim());
                        } else {
                            k.setAdressenzusatz(kf.getAdressenzusatz());
                        }
                        if (kf.getPLZ() != null) {
                            k.setPLZ(kf.getPLZ().trim());
                        } else {
                            k.setPLZ(kf.getPLZ());
                        }
                        if (kf.getOrt() != null) {
                            k.setOrt(kf.getOrt().trim());
                        } else {
                            k.setOrt(kf.getOrt());
                        }
                        k.setLand(kf.getLand());
                        k.setTimezone(kf.getTimezone());
                        if (kf.getTelefon() != null) {
                            k.setTelefon(kf.getTelefon().trim());
                        } else {
                            k.setTelefon(kf.getTelefon());
                        }
                        if (kf.getFax_extern() != null) {
                            k.setFax_extern(kf.getFax_extern().trim());
                        } else {
                            k.setFax_extern(kf.getFax_extern());
                        }
                        if (kf.getBibliotheksmail() != null) {
                            k.setBibliotheksmail(kf.getBibliotheksmail().trim());
                        } else {
                            k.setBibliotheksmail(kf.getBibliotheksmail());
                        }
                        if (kf.getDbsmail() != null) {
                            k.setDbsmail(kf.getDbsmail().trim());
                        } else {
                            k.setDbsmail(kf.getDbsmail());
                        }
                        k.setEzbid(kf.getEzbid());
                        k.setInstlogolink(kf.getInstlogolink());
                        k.setZdb(kf.isZdb());
                        k.setUserlogin(kf.isUserlogin());
                        k.setUserbestellung(kf.isUserbestellung()); // SUBITO
                        k.setGbvbestellung(kf.isGbvbestellung()); // GBV
                        k.setOrderlimits(kf.getOrderlimits());
                        k.setMaxordersu(kf.getMaxordersu());
                        k.setMaxordersutotal(kf.getMaxordersutotal());
                        k.setMaxordersj(kf.getMaxordersj());

                        k.setDefault_deloptions(kf.getDefault_deloptions());

                        k.update(cn.getConnection());
                        //                                k.setCn(cn.getConnection());

                        // veränderte Kontos in Session legen, damit neue Angaben angezeigt werden. Z.B. kontochange
                        ui.setKonto(k);
                        final List<Konto> kontolist = ui.getKonto()
                                .getLoginKontos(ui.getBenutzer(), cn.getConnection());
                        ui.setKontoanz(kontolist.size()); // Anzahl Kontos im UserInfo hinterlegen
                        ui.setKontos(kontolist);
                        rq.getSession().setAttribute("userinfo", ui);

                    } // benötigt keine separate else-Schlaufe, da der Negativfall schon oben behandelt wurde

                } else {
                    forward = "missingvalues";
                    kf.setMessage("error.url");
                    final TimeZones tz = new TimeZones();
                    final SortedSet<String> setTZ = tz.getTimeZonesAsString();
                    rq.setAttribute(TIMEZONES, setTZ);
                    final List<Countries> allPossCountries = country.getAllCountries(cn.getConnection());
                    kf.setCountries(allPossCountries);
                }

            } else {
                forward = "missingvalues";
                kf.setMessage("error.values");
                final TimeZones tz = new TimeZones();
                final SortedSet<String> setTZ = tz.getTimeZonesAsString();
                rq.setAttribute(TIMEZONES, setTZ);
                final List<Countries> allPossCountries = country.getAllCountries(cn.getConnection());
                kf.setCountries(allPossCountries);
            }

            rq.setAttribute("kontoform", kf);

        } finally {
            cn.close();
        }

        return mp.findForward(forward);
    }

    /**
     * Auflistung Kontos fuer Admins zum Kontos deaktivieren oder reaktivieren
     */
    public ActionForward listKontos(final ActionMapping mp, final ActionForm fm, final HttpServletRequest rq,
            final HttpServletResponse rp) {

        final Auth auth = new Auth();
        // make sure the user is logged in
        if (!auth.isLogin(rq)) {
            return mp.findForward(Result.ERROR_TIMEOUT.getValue());
        }
        // check access rights
        if (!auth.isAdmin(rq)) {
            return mp.findForward(Result.ERROR_MISSING_RIGHTS.getValue());
        }

        String forward = Result.FAILURE.getValue();

        final KontoForm kf = (KontoForm) fm;
        final Konto k = new Konto();

        try {
            if (kf.getSubmenu().equals("premium")) {
                kf.setKontos(k.getAdvancedKontos(k.getConnection()));
            } else if (kf.getSubmenu().equals("openbill")) {
                kf.setKontos(k.getKontosOpenBill(k.getConnection()));
            } else {
                kf.setKontos(k.getAllKontos(k.getConnection()));
            }

            rq.setAttribute("kontoform", kf);
            if (kf.getKontos() == null) {
                final ErrorMessage em = new ErrorMessage("error.missingaccounts", "searchfree.do");
                rq.setAttribute(Result.ERRORMESSAGE.getValue(), em);
            } else {
                forward = Result.SUCCESS.getValue();
            }
        } finally {
            k.close();
        }

        return mp.findForward(forward);
    }

    /**
     * Admins können einen Kontotypen ändern Beim Wechsel auf den typ Gratis
     * wird das Kontoablaufdatum auf null gesetzt
     */
    public ActionForward changeKontoTyp(final ActionMapping mp, final ActionForm fm, final HttpServletRequest rq,
            final HttpServletResponse rp) {

        final Auth auth = new Auth();
        // make sure the user is logged in
        if (!auth.isLogin(rq)) {
            return mp.findForward(Result.ERROR_TIMEOUT.getValue());
        }
        // check access rights
        if (!auth.isAdmin(rq)) {
            return mp.findForward(Result.ERROR_MISSING_RIGHTS.getValue());
        }

        String forward = Result.FAILURE.getValue();
        final Text cn = new Text();

        try {
            final KontoForm kf = (KontoForm) fm;
            final Konto k = new Konto(kf.getKid(), cn.getConnection());
            k.setKontotyp(kf.getKontotyp());
            if (k.getKontotyp() == 0) {
                k.setExpdate(null);
            } // Gratiskontos laufen nicht ab!
            k.update(cn.getConnection());

            final Message em = new Message("message.changekontotyp", k.getBibliotheksname(),
                    "kontoadmin.do?method=listKontos");
            rq.setAttribute("message", em);
            forward = Result.SUCCESS.getValue();
        } catch (final Exception e) {
            LOG.error("changeKonto" + e.toString());
            final ErrorMessage em = new ErrorMessage("error.changetype", e.getMessage(),
                    "kontoadmin.do?method=listKontos");
            rq.setAttribute(Result.ERRORMESSAGE.getValue(), em);
        } finally {
            cn.close();
        }

        return mp.findForward(forward);
    }

    /**
     * Admins können Kontostatus ändern
     */
    public ActionForward changeKontoState(final ActionMapping mp, final ActionForm fm, final HttpServletRequest rq,
            final HttpServletResponse rp) {

        final Auth auth = new Auth();
        // make sure the user is logged in
        if (!auth.isLogin(rq)) {
            return mp.findForward(Result.ERROR_TIMEOUT.getValue());
        }
        // check access rights
        if (!auth.isAdmin(rq)) {
            return mp.findForward(Result.ERROR_MISSING_RIGHTS.getValue());
        }

        String forward = Result.FAILURE.getValue();
        final Text cn = new Text();

        try {
            final KontoForm kf = (KontoForm) fm;
            final Konto k = new Konto(kf.getKid(), cn.getConnection());
            k.setKontostatus(kf.isKontostatus());
            k.update(cn.getConnection());

            final Message em = new Message("message.changekontostatus", k.getBibliotheksname(),
                    "kontoadmin.do?method=listKontos");
            rq.setAttribute("message", em);
            forward = Result.SUCCESS.getValue();

        } catch (final Exception e) {
            LOG.error("changeKontoState: " + e.toString());
            final ErrorMessage em = new ErrorMessage("error.changestate", e.getMessage(),
                    "kontoadmin.do?method=listKontos");
            rq.setAttribute(Result.ERRORMESSAGE.getValue(), em);
        } finally {
            cn.close();
        }

        return mp.findForward(forward);
    }

    /**
     * Admins können Expdate setzen
     */
    public ActionForward setExpDate(final ActionMapping mp, final ActionForm fm, final HttpServletRequest rq,
            final HttpServletResponse rp) {

        final Auth auth = new Auth();
        // make sure the user is logged in
        if (!auth.isLogin(rq)) {
            return mp.findForward(Result.ERROR_TIMEOUT.getValue());
        }
        // check access rights
        if (!auth.isAdmin(rq)) {
            return mp.findForward(Result.ERROR_MISSING_RIGHTS.getValue());
        }

        String forward = Result.FAILURE.getValue();
        final Text cn = new Text();

        try {
            final KontoForm kf = (KontoForm) fm;
            final Konto k = new Konto(kf.getKid(), cn.getConnection());
            k.setExpdate(kf.getExpdate());
            k.update(cn.getConnection());

            final Message em = new Message("message.changeexpdate", k.getBibliotheksname(),
                    "kontoadmin.do?method=listKontos");
            rq.setAttribute("message", em);
            forward = Result.SUCCESS.getValue();

        } catch (final Exception e) {
            LOG.error("setExpdate: " + e.toString());
            final ErrorMessage em = new ErrorMessage("error.timeperiode", e.getMessage(),
                    "kontoadmin.do?method=listKontos");
            rq.setAttribute(Result.ERRORMESSAGE.getValue(), em);
        } finally {
            cn.close();
        }

        return mp.findForward(forward);
    }

    /**
     * Admins können ExpDateServer setzen
     */
    public ActionForward setExpDateServer(final ActionMapping mp, final ActionForm fm, final HttpServletRequest rq,
            final HttpServletResponse rp) {

        final Auth auth = new Auth();
        // make sure the user is logged in
        if (!auth.isLogin(rq)) {
            return mp.findForward(Result.ERROR_TIMEOUT.getValue());
        }
        // check access rights
        if (!auth.isAdmin(rq)) {
            return mp.findForward(Result.ERROR_MISSING_RIGHTS.getValue());
        }

        String forward = Result.FAILURE.getValue();
        final Text cn = new Text();

        try {
            final KontoForm kf = (KontoForm) fm;
            final Konto k = new Konto(kf.getKid(), cn.getConnection());
            k.setPopfaxend(kf.getPopfaxend());
            k.update(cn.getConnection());

            final Message em = new Message("message.changeexpdatefax", k.getBibliotheksname(),
                    "kontoadmin.do?method=listKontos");
            rq.setAttribute("message", em);
            forward = Result.SUCCESS.getValue();

        } catch (final Exception e) {
            LOG.error("setExpDate: " + e.toString());
            final ErrorMessage em = new ErrorMessage("error.timeperiodefax", e.getMessage(),
                    "kontoadmin.do?method=listKontos");
            rq.setAttribute(Result.ERRORMESSAGE.getValue(), em);
        } finally {
            cn.close();
        }

        return mp.findForward(forward);
    }

    /**
     * Admins können Rechnungen vorbereiten
     */
    public ActionForward prepareBillingText(final ActionMapping mp, final ActionForm fm, final HttpServletRequest rq,
            final HttpServletResponse rp) {

        final Auth auth = new Auth();
        // make sure the user is logged in
        if (!auth.isLogin(rq)) {
            return mp.findForward(Result.ERROR_TIMEOUT.getValue());
        }
        // check access rights
        if (!auth.isAdmin(rq)) {
            return mp.findForward(Result.ERROR_MISSING_RIGHTS.getValue());
        }

        String forward = Result.FAILURE.getValue();
        final Text cn = new Text();

        try {
            BillingForm bf = (BillingForm) fm;
            final Konto k = new Konto(bf.getKontoid(), cn.getConnection());

            // Rechnung vorbereiten
            final KontoAdmin ka = new KontoAdmin();
            bf = ka.prepareBillingText(k, cn.getConnection(), null, bf);
            bf.setKonto(k);

            rq.setAttribute("billingform", bf);

            forward = Result.SUCCESS.getValue();

        } catch (final Exception e) {
            LOG.error("prepareBilling: " + e.toString());
            final ErrorMessage em = new ErrorMessage("error.preparebilling", e.getMessage(),
                    "kontoadmin.do?method=listKontos");
            rq.setAttribute(Result.ERRORMESSAGE.getValue(), em);
        } finally {
            cn.close();
        }

        return mp.findForward(forward);
    }

}
