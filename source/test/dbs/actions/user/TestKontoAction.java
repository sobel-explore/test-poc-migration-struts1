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

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import servletunit.struts.MockStrutsTestCase;
import test.PrepareTestObjects;
import ch.dbs.entity.Konto;
import ch.dbs.form.KontoForm;
import ch.dbs.form.UserForm;
import enums.Result;

public class TestKontoAction extends MockStrutsTestCase {

    private transient Konto k = new Konto();
    private final transient Long id = PrepareTestObjects.KONTOID;

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

    @Test
    public void testAddNewKonto() throws Exception {

        // remove old testobjects from db
        PrepareTestObjects.clearTestObjects();

        //    Kontoform vorbereiten

        k = new Konto(id, k.getConnection());
        final KontoForm kf = new KontoForm(k);
        kf.setValuesFromKonto();

        //Kontoform absenden
        setRequestPathInfo("/anmeldungkonto_.do");
        addRequestParameter("method", "addNewKonto");
        setActionForm(kf);
        actionPerform();

        //Kontrolle ob es geklappt hat
        verifyNoActionErrors();
        verifyForward(Result.FAILURE.getValue());
    }

    @Ignore("Test requires specific database state")
    @Test
    public void testAddNewBibliothekar() throws Exception {

        // Kontoform vorbereiten
        final KontoForm kf = PrepareTestObjects.getKontoValues();

        // UserForm vorbereiten
        final UserForm uf = PrepareTestObjects.getUserForm();

        //Kontoforms absenden
        setRequestPathInfo("/anmeldungbibliothekar_.do");
        addRequestParameter("method", "addNewBibliothekar");
        getSession().setAttribute("kontoform", kf);

        setActionForm(uf);
        actionPerform();

        //Kontrolle ob es geklappt hat
        verifyNoActionErrors();
        verifyForward("weiter");

    }

    @Test
    public void testAddNewBibliothekarIdenticalUser() throws Exception {

        // Kontoform vorbereiten
        final KontoForm kf = PrepareTestObjects.getKontoValues();
        kf.setBiblioname(PrepareTestObjects.BIBLIONAME2);

        // UserForm vorbereiten
        final UserForm uf = PrepareTestObjects.getUserForm();

        //Kontoforms absenden
        setRequestPathInfo("/anmeldungbibliothekar_.do");
        addRequestParameter("method", "addNewBibliothekar");
        getSession().setAttribute("kontoform", kf);

        setActionForm(uf);
        actionPerform();

        //Kontrolle ob es geklappt hat
        verifyNoActionErrors();
        verifyForward("weiter");

        // remove testobjects from db
        PrepareTestObjects.clearTestObjects();
    }

    //  @Test
    //  public void testAddNewFaxentry() throws Exception {
    //
    //    Fax f = new Fax();
    //    f.setFrom("Fromnummer");
    //    f.setKid("1");
    //    f.setPages("30");
    //    f.setPopfaxid("123456");
    //    f.setState("20");
    //    f.setStatedate("9999-09-09");
    //    f.saveNewFax(f.getConnection());
    //    f.close();
    ////    Kontoform vorbereiten
    //
    //    k = new Konto(id, k.getConnection());
    //    KontoForm kf = new KontoForm(k);
    //    kf.setValuesFromKonto();
    //
    //    //Konto Subitobn modifizieren, damit Test durch Dublettenkontrollle Subitobn kommt
    //    k.setSubitobenutzername("blaaa");
    //    k.update(k.getConnection());
    //
    //    //Kontoform absenden
    //    setRequestPathInfo("/anmeldungkonto_.do");
    //    addRequestParameter("method", "addNewKonto");
    //    setActionForm(kf);
    //    actionPerform();
    //
    //    //Kontrolle ob es geklappt hat
    //    verifyNoActionErrors();
    //    verifyForward(Result.FAILURE.getValue());
    //  }

}
