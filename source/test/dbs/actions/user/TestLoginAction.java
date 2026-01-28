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

package test.dbs.actions.user;

import java.io.File;
import java.util.Date;
import java.util.List;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import servletunit.struts.MockStrutsTestCase;
import test.PrepareTestObjects;
import util.ThreadSafeSimpleDateFormat;
import ch.dbs.entity.AbstractBenutzer;
import ch.dbs.entity.Benutzer;
import ch.dbs.entity.Bibliothekar;
import ch.dbs.entity.Konto;
import ch.dbs.entity.VKontoBenutzer;
import ch.dbs.form.KontoForm;
import ch.dbs.form.LoginForm;
import ch.dbs.form.UserInfo;
import enums.Result;

@Ignore("disabled, outdated and not working test class")
public class TestLoginAction extends MockStrutsTestCase {

    private static Konto k1;
    private static Konto k2;
    private static Bibliothekar bi;
    private static Benutzer be;
    private static VKontoBenutzer vkb;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {

    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {

    }

    @Before
    public void setUp() throws Exception {
        super.setUp();
        //     set the context directory to /WebRoot
        // to find the /WEB-INF/web.xml
        setContextDirectory(new File("war"));

    }

    @After
    public void tearDown() throws Exception {

    }

    //  @Test
    //  public void testClearTestObjectsSetup() throws Exception {
    //
    //    // Alle Testkontos und Testbenutzer sowie deren Verknuepfungen wieder löschen
    //    ArrayList<AbstractBenutzer> al = AbstractBenutzer.getAllUserFromEmail(
    //        PrepareTestObjects.BNEMAIL, k1.getConnection());
    //    if (al != null) {
    //      for (AbstractBenutzer b : al) {
    //        ArrayList<Konto> k = AbstractBenutzer.getKontosDeposited(b, k1.getConnection());
    //        for (Konto konto : k) {
    //          vkb.deletEntry(b, konto, k1.getConnection());
    //          konto.deleteSelf(k1.getConnection());
    //          AbstractBenutzer.deleteUser(b, k1.getConnection());
    //        }
    //      }
    //    }
    //    k1.close();
    //  }

    @Test
    public void testLoginGoGtc() throws Exception {

        //    Testkonton erstellen
        k1 = PrepareTestObjects.getKonto();
        k1.save(k1.getConnection());

        //Testbenutzer erstellen
        final Date d = new Date();
        final ThreadSafeSimpleDateFormat fmt = new ThreadSafeSimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        final String datum = fmt.format(d, k1.getTimezone());
        final AbstractBenutzer b = new AbstractBenutzer();
        bi = PrepareTestObjects.getBibliothekar();
        bi.setDatum(datum);
        bi.setId(b.saveNewUser(bi, k1.getTimezone(), k1.getConnection()));

        //    Benutzer mit Konten verknuepfen
        vkb = new VKontoBenutzer();
        if (bi.getId() != null && k1.getId() != null) {
            vkb.save(bi, k1, k1.getConnection());
        }
        k1.close();

        //Loginform vorbereiten
        final LoginForm lf = new LoginForm();
        lf.setEmail(PrepareTestObjects.BNEMAIL);
        lf.setPassword(PrepareTestObjects.LOGINPW);

        // Framework vorbereiten und Logininfos abschicken
        setRequestPathInfo("/logincheck.do");
        setActionForm(lf);
        actionPerform();

        // Test
        verifyNoActionErrors();
        verifyForward("gtc");
    }

    @Test
    public void testLoginDeclineGtc() throws Exception {

        //Loginform vorbereiten
        final LoginForm lf = new LoginForm();
        lf.setEmail(PrepareTestObjects.BNEMAIL);
        lf.setPassword(PrepareTestObjects.LOGINPW);

        // Framework vorbereiten und Logininfos abschicken
        setRequestPathInfo("/logincheck.do");
        setActionForm(lf);
        actionPerform();

        // GTC Ablehnen
        //    UserInfo ui = (UserInfo) getSession().getAttribute("userinfo");
        setRequestPathInfo("/gtc_.do");
        addRequestParameter("method", "decline");
        setActionForm(lf);
        actionPerform();

        // Test
        verifyNoActionErrors();
        verifyForward("decline");
    }

    @Test
    public void testAccepptGtc() throws Exception {

        //Loginform vorbereiten
        final LoginForm lf = new LoginForm();
        lf.setEmail(PrepareTestObjects.BNEMAIL);
        lf.setPassword(PrepareTestObjects.LOGINPW);

        // Framework vorbereiten und Logininfos abschicken
        setRequestPathInfo("/logincheck.do");
        setActionForm(lf);
        actionPerform();

        // GTC Annehmen
        //    UserInfo ui = (UserInfo) getSession().getAttribute("userinfo");
        setRequestPathInfo("/gtc_.do");
        addRequestParameter("method", "accept");
        setActionForm(lf);
        actionPerform();

        // Test
        verifyNoActionErrors();
        verifyForward("success");
    }

    @Test
    public void testOpenUrl() throws Exception {

        // OpenUrl abschicken
        LoginForm lf = PrepareTestObjects.getloginForm();
        setRequestPathInfo("/pl.do");
        setActionForm(lf);
        actionPerform();

        // ActionForm aus response holen
        lf = (LoginForm) getActionForm();

        // Test
        verifyNoActionErrors();
        verifyForward("success");
        assertEquals("OpenUrl wurde nicht korrekt übergeben", PrepareTestObjects.getloginForm().getJahr(), lf.getJahr());

    }

    @Test
    public void testLoginSameMailPwBibliothekarUserNotAllowedLogin() throws Exception {

        //    Testkonto 2 erstellen
        k2 = PrepareTestObjects.getKonto();
        k2.setBibliotheksname(k2.getBibliotheksname() + "zweite");
        k2.save(k1.getConnection());

        //Testbenutzer erstellen
        final Date d = new Date();
        final ThreadSafeSimpleDateFormat fmt = new ThreadSafeSimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        final String datum = fmt.format(d, k2.getTimezone());
        final AbstractBenutzer b = new AbstractBenutzer();
        be = PrepareTestObjects.getBenutzer();
        be.setDatum(datum);
        be.setId(b.saveNewUser(be, k2.getTimezone(), k1.getConnection()));

        //    Benutzer mit Konten verknuepfen
        vkb = new VKontoBenutzer();
        if (be.getId() != null && k2.getId() != null) {
            vkb.save(be, k2, k1.getConnection());
        }

        //Login
        final LoginForm lf = new LoginForm();
        lf.setEmail(PrepareTestObjects.BNEMAIL);
        lf.setPassword(PrepareTestObjects.LOGINPW);
        setRequestPathInfo("/logincheck.do");
        addRequestParameter("method", "overview");
        setActionForm(lf);
        actionPerform();

        // Test
        verifyNoActionErrors();
        verifyForward("success");

    }

    @Test
    public void testLoginSameMailPwBibliothekarUserNotAllowedLoginKontoUserLoginTrue() throws Exception {

        // Benutzer login in DB auf Benutzer erlauben
        final AbstractBenutzer b = new AbstractBenutzer();
        final Konto tz = new Konto(); // we need this for setting a default timezone
        be = PrepareTestObjects.getTestBenutzerFromDb(k1.getConnection());
        be.setLoginopt(true);
        b.updateUser(be, tz.getTimezone(), k1.getConnection());

        //Login
        final LoginForm lf = new LoginForm();
        lf.setEmail(PrepareTestObjects.BNEMAIL);
        lf.setPassword(PrepareTestObjects.LOGINPW);
        setRequestPathInfo("/logincheck.do");
        addRequestParameter("method", "overview");
        setActionForm(lf);
        actionPerform();

        // Test
        verifyNoActionErrors();
        verifyForward("success");

    }

    @Test
    public void testLoginSameMailPwBibliothekarUserAllowedLogin() throws Exception {

        // Benutzer login in DB auf Konto erlauben
        final AbstractBenutzer b = new AbstractBenutzer();
        be = PrepareTestObjects.getTestBenutzerFromDb(k1.getConnection());
        final List<Konto> kl = b.getKontosDeposited(be, k1.getConnection());
        if (!kl.isEmpty()) {
            kl.get(0).setUserlogin(true);
            kl.get(0).update(k1.getConnection());
        }

        //Login
        final LoginForm lf = new LoginForm();
        lf.setEmail(PrepareTestObjects.BNEMAIL);
        lf.setPassword(PrepareTestObjects.LOGINPW);
        setRequestPathInfo("/logincheck.do");
        setActionForm(lf);
        actionPerform();

        // Test
        verifyNoActionErrors();
        verifyForward("chooseuser");

    }

    @Test
    public void testLoginSameMailPwBibliothekarUserAllowedLoginChoose() throws Exception {

        // Login
        LoginForm lf = new LoginForm();
        lf.setEmail(PrepareTestObjects.BNEMAIL);
        lf.setPassword(PrepareTestObjects.LOGINPW);
        setRequestPathInfo("/logincheck.do");
        setActionForm(lf);
        actionPerform();

        //    Benutzerauswahl treffen
        lf = (LoginForm) getActionForm();
        lf.setUserid(lf.getUserinfolist().get(0).getBenutzer().getId());

        // Request vorbereiten und abschicken
        setRequestPathInfo("/setuser.do");
        setActionForm(lf);
        addRequestParameter("method", "setuser");
        getSession().setAttribute("authuserlist", lf);
        actionPerform();

        // Test
        verifyNoActionErrors();
        verifyForward("success");

    }

    @Test
    public void testLoginSameMailPwBibliothekarUserAllowedLoginChooseFakeChoose() throws Exception {

        // Login
        LoginForm lf = new LoginForm();
        lf.setEmail(PrepareTestObjects.BNEMAIL);
        lf.setPassword(PrepareTestObjects.LOGINPW);
        setRequestPathInfo("/logincheck.do");
        setActionForm(lf);
        actionPerform();

        //    Benutzerauswahl treffen
        lf = (LoginForm) getActionForm();
        lf.setUserid(Long.valueOf(0)); // Hier UID faelschen

        // Request vorbereiten und abschicken
        setRequestPathInfo("/setuser.do");
        setActionForm(lf);
        addRequestParameter("method", "setuser");
        getSession().setAttribute("authuserlist", lf);
        actionPerform();

        // Test
        verifyNoActionErrors();
        verifyForward(Result.FAILURE.getValue());

    }

    @Test
    public void testLoginSameMailPwBibliothekarUserAllowedLoginChooseGTC() throws Exception {

        // Login
        LoginForm lf = new LoginForm();
        lf.setEmail(PrepareTestObjects.BNEMAIL);
        lf.setPassword(PrepareTestObjects.LOGINPW);
        setRequestPathInfo("/logincheck.do");
        setActionForm(lf);
        actionPerform();

        //    Benutzerauswahl treffen
        lf = (LoginForm) getActionForm();
        lf.setUserid(lf.getUserinfolist().get(1).getBenutzer().getId()); // auf diesem Benutzer wurde die GTC noch nicht akzeptiert

        // Request vorbereiten und abschicken
        setRequestPathInfo("/setuser.do");
        setActionForm(lf);
        addRequestParameter("method", "setuser");
        getSession().setAttribute("authuserlist", lf);
        actionPerform();

        // Test
        verifyNoActionErrors();
        verifyForward("gtc");

    }

    @Test
    public void testLoginSameMailPwBibliothekarUserAllowedLoginChooseLinkResolver() throws Exception {

        //OpenUrl abschicken
        LoginForm lf = PrepareTestObjects.getloginForm();
        setRequestPathInfo("/pl.do");
        setActionForm(lf);
        actionPerform();

        // Login durchführen
        lf = (LoginForm) getActionForm(); // Abgefülltes LoginForm aus Request wieder abholen
        lf.setEmail(PrepareTestObjects.BNEMAIL);
        lf.setPassword(PrepareTestObjects.LOGINPW);
        setRequestPathInfo("/logincheck.do");
        setActionForm(lf);
        actionPerform();

        //    Benutzerauswahl treffen
        lf = (LoginForm) getActionForm();
        lf.setUserid(lf.getUserinfolist().get(0).getBenutzer().getId());
        setRequestPathInfo("/setuser.do");
        setActionForm(lf);
        addRequestParameter("method", "setuser");
        getSession().setAttribute("authuserlist", lf);
        actionPerform();

        // Test
        verifyNoActionErrors();
        verifyForward("checkavailability");

    }

    @Test
    public void testLoginSameMailPwBibliothekarUserAllowedLoginChooseUserAndChooseKontoLinkresolver() throws Exception {

        // Zweites Konto beim Benutzer hinterlegen
        final Bibliothekar bi = PrepareTestObjects.getTestBibliothekarFromDb(k1.getConnection());
        vkb = new VKontoBenutzer();
        if (bi.getId() != null && k1.getId() != null) {
            vkb.save(bi, k2, k1.getConnection());
        }

        //Loginform vorbereiten
        LoginForm lf = PrepareTestObjects.getloginForm();
        setRequestPathInfo("/pl.do");
        setActionForm(lf);
        actionPerform();

        // Login durchführen
        lf = (LoginForm) getActionForm();
        lf.setEmail(PrepareTestObjects.BNEMAIL);
        lf.setPassword(PrepareTestObjects.LOGINPW);
        setRequestPathInfo("/logincheck.do");
        setActionForm(lf);
        actionPerform();

        //    Benutzerauswahl treffen
        lf = (LoginForm) getActionForm();
        lf.setUserid(lf.getUserinfolist().get(0).getBenutzer().getId());
        setRequestPathInfo("/setuser.do");
        setActionForm(lf);
        addRequestParameter("method", "setuser");
        getSession().setAttribute("authuserlist", lf);
        actionPerform();

        // Kontoauswahl
        lf = (LoginForm) getActionForm();

        // Test
        verifyNoActionErrors();
        verifyForward("kontochoose");
        assertEquals("Angaben aus linkersolver sind nicht korrekt angekommen", PrepareTestObjects.getloginForm()
                .getJahr(), lf.getJahr());

    }

    @Test
    public void testLoginSameMailPwBibliothekarUserAllowedLoginChooseUserAndChoosenKontoLinkresolver() throws Exception {

        //Loginform vorbereiten
        LoginForm lf = PrepareTestObjects.getloginForm();
        setRequestPathInfo("/pl.do");
        setActionForm(lf);
        actionPerform();

        // Login durchführen
        lf = (LoginForm) getActionForm();
        lf.setEmail(PrepareTestObjects.BNEMAIL);
        lf.setPassword(PrepareTestObjects.LOGINPW);
        setRequestPathInfo("/logincheck.do");
        setActionForm(lf);
        actionPerform();

        //    Benutzerauswahl treffen
        lf = (LoginForm) getActionForm();
        lf.setUserid(lf.getUserinfolist().get(0).getBenutzer().getId());
        setRequestPathInfo("/setuser.do");
        setActionForm(lf);
        addRequestParameter("method", "setuser");
        getSession().setAttribute("authuserlist", lf);
        actionPerform();

        // Kontoauswahl
        setRequestPathInfo("/changekonto.do");
        addRequestParameter("method", "changekonto");
        final UserInfo ui = (UserInfo) getSession().getAttribute("userinfo");
        final KontoForm kf = new KontoForm();
        kf.setJahr(lf.getJahr()); //Nur rudimentaer, müsste anders gehe
        kf.setResolver(true); //Nur rudimentaer, müsste anders gehe
        kf.setKid(ui.getKontos().get(0).getId());
        setActionForm(kf);
        actionPerform();

        // Test
        verifyNoActionErrors();
        verifyForward("checkavailability");

    }

    @Test
    public void testLoginWrongLogin() throws Exception {

        //Loginform vorbereiten
        final LoginForm lf = new LoginForm();
        lf.setEmail("1" + PrepareTestObjects.BNEMAIL);
        lf.setPassword(PrepareTestObjects.LOGINPW);

        setRequestPathInfo("/logincheck.do");
        addRequestParameter("method", "overview");
        setActionForm(lf);
        actionPerform();

        verifyNoActionErrors();
        verifyForward(Result.FAILURE.getValue());

    }

    @Test
    public void testClearTestObjectsTearDown() throws Exception {

        PrepareTestObjects.clearTestObjects();
        k1.close();

    }
}
