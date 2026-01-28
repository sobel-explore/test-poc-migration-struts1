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

package test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import util.Encrypt;
import ch.dbs.entity.AbstractBenutzer;
import ch.dbs.entity.Benutzer;
import ch.dbs.entity.Bibliothekar;
import ch.dbs.entity.Konto;
import ch.dbs.entity.Text;
import ch.dbs.entity.VKontoBenutzer;
import ch.dbs.form.KontoForm;
import ch.dbs.form.LoginForm;
import ch.dbs.form.UserForm;
import ch.ddl.entity.Position;

public class PrepareTestObjects {

    /** Passwort welches zum Login benötigt wird */
    public static final String LOGINPW = "testpw";

    /** E-Mail welche zum Login benötigt wird */
    public static final String BNEMAIL = "testitestmail@eldali.ch";

    public static final Long KONTOID = Long.valueOf(1);

    public static final String BIBLIONAME1 = "Biblioname1";
    public static final String BIBLIONAME2 = "Biblioname2";

    /**
     * Erstellt ein Loginform mit Linkresolver-Angaben
     * 
     * @return LoginForm
     */
    public static LoginForm getloginForm() {
        final LoginForm lf = new LoginForm();

        lf.setMediatype("Artikel");
        lf.setIssn("0803-5253");
        lf.setJahr("1995");
        lf.setJahrgang("84");
        lf.setHeft("9");
        lf.setSeiten("1019-28");
        lf.setArtikeltitel("Swedish+population+reference+standards+for+height%2C+weight+and+body+mass+index+attained+at+6+to+16+years+%28girls%29+or+19+years+%28boys%29");
        lf.setZeitschriftentitel("Acta+paediatrica+%28Oslo%2C+Norway+%3A+1992%29");
        lf.setAuthor("Lindgren+G");
        lf.setGenre("Journal+Article");
        lf.setPmid("info%3Apmid%2F8652953");
        lf.setResolver(true);

        return lf;
    }

    /**
     * Erstellt ein Test-Kontoform
     * 
     * @return KontoForm
     */
    public static KontoForm getKontoValues() {

        final KontoForm kf = new KontoForm();

        kf.setBiblioname(BIBLIONAME1);
        kf.setAdresse("adresse");
        kf.setPLZ("1234");
        kf.setLand("CH");
        kf.setTelefon("12345678");
        kf.setBibliotheksmail("info@doctor-doc.com");
        kf.setDbsmail("dbs@mail.bl");
        kf.setUserlogin(false);

        kf.setEzbid(""); //verhindert NullPointer in KontoAction

        return kf;

    }

    public static List<Konto> getTestkonto() {

        final List<Konto> kl = new ArrayList<Konto>();
        final Konto cn = new Konto();
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            pstmt = cn.getConnection().prepareStatement(
                    "SELECT * FROM konto WHERE `biblioname` = ? or `biblioname` = ?");
            pstmt.setString(1, BIBLIONAME1);
            pstmt.setString(2, BIBLIONAME2);

            rs = pstmt.executeQuery();

            while (rs.next()) {
                kl.add(new Konto(rs));
            }

        } catch (final Exception e) {
            System.out.println("getKontoValues() trat folgender Fehler auf: \012" + e);
        } finally {
            if (rs != null) {
                try {
                    rs.close();
                } catch (final SQLException e) {
                    final Logger LOG = LoggerFactory.getLogger(PrepareTestObjects.class);
                    LOG.error(e.toString());
                }
            }
            if (pstmt != null) {
                try {
                    pstmt.close();
                } catch (final SQLException e) {
                    final Logger LOG = LoggerFactory.getLogger(PrepareTestObjects.class);
                    LOG.error(e.toString());
                }
            }
            cn.close();
        }
        return kl;
    }

    public static void clearTestObjects() {
        // UserForm vorbereiten
        //    UserForm uf = PrepareTestObjects.getUserForm();
        final List<Konto> oldkl = PrepareTestObjects.getTestkonto();

        final Konto cn = new Konto();

        for (final Konto k : oldkl) {
            System.out.println("Konto ID = " + k.getId());
            k.deleteSelf(cn.getConnection());
        }

        final VKontoBenutzer vkb = new VKontoBenutzer();
        final List<AbstractBenutzer> alb = new AbstractBenutzer()
        .getAllUserFromEmail(BNEMAIL, cn.getConnection());
        for (final AbstractBenutzer b : alb) {
            vkb.deleteAllKontoEntries(b, cn.getConnection());
            b.deleteUser(b, cn.getConnection());
        }
        cn.close();
    }

    /**
     * Erstellt ein Test-UserForm
     */
    public static UserForm getUserForm() {

        final UserForm uf = new UserForm();

        uf.setAnrede("Herr");
        uf.setName("Testname"); //Mussfeld beim Bibliothekar erstellen
        uf.setVorname("Testvorname"); //Mussfeld beim Bibliothekar erstellen
        uf.setEmail(BNEMAIL); //Mussfeld beim Bibliothekar erstellen
        uf.setKontostatus(true);
        final Encrypt e = new Encrypt();
        uf.setPassword(e.makeSHA(LOGINPW)); //Das Passwort muss mindestens 7 Zeichen lang sein

        return uf;
    }

    /**
     * Erstellt ein Testkontoobjekt
     * 
     * @return Konto
     */
    public static Konto getKonto() {

        return new Konto(getKontoValues());

    }

    /**
     * Erstellt ein TestBenutzerObjekt
     * 
     * @return Benutzer
     */
    public static Benutzer getBenutzer() {

        final Text t = new Text();

        final Benutzer b = new Benutzer(getUserForm(), t.getConnection());

        t.close();

        return b;
    }

    /**
     * Holt eine Testbenutzer aus der Datenbank
     * 
     * @param cn
     * @return
     */
    public static Benutzer getTestBenutzerFromDb(final Connection cn) {
        Benutzer b = null;
        final AbstractBenutzer ab = new AbstractBenutzer();
        final Encrypt e = new Encrypt();
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            pstmt = cn.prepareStatement("SELECT * FROM `benutzer` AS b " + "WHERE b.mail = ? AND b.pw = ? "
                    + "AND b.rechte = 1 ");
            pstmt.setString(1, BNEMAIL);
            pstmt.setString(2, e.makeSHA(LOGINPW));
            rs = pstmt.executeQuery();

            while (rs.next()) {
                b = (Benutzer) ab.getUser(rs, cn);
            }
        } catch (final Exception err) {
            final String f = "In PrepareTestObjects.getTestBenutzerFromDb(Connection cn) trat folgender Fehler auf:\n";
            System.out.println(f + err);
        } finally {
            if (rs != null) {
                try {
                    rs.close();
                } catch (final SQLException ex) {
                    System.out.println(ex);
                }
            }
            if (pstmt != null) {
                try {
                    pstmt.close();
                } catch (final SQLException ex) {
                    System.out.println(ex);
                }
            }
        }

        return b;
    }

    /**
     * Holt eine Testbibliothekar aus der Datenbank
     * 
     * @param cn
     * @return
     */
    public static Bibliothekar getTestBibliothekarFromDb(final Connection cn) {
        Bibliothekar b = null;
        final AbstractBenutzer ab = new AbstractBenutzer();
        final Encrypt e = new Encrypt();
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            pstmt = cn.prepareStatement("SELECT * FROM `benutzer` AS b " + "WHERE b.mail = ? AND b.pw = ? "
                    + "AND b.rechte = 2 ");
            pstmt.setString(1, BNEMAIL);
            pstmt.setString(2, e.makeSHA(LOGINPW));
            rs = pstmt.executeQuery();

            while (rs.next()) {
                b = (Bibliothekar) ab.getUser(rs, cn);
            }
        } catch (final Exception err) {
            final String f = "In PrepareTestObjects.getTestBenutzerFromDb(Connection cn) trat folgender Fehler auf:\n";
            System.out.println(f + err);
        } finally {
            if (rs != null) {
                try {
                    rs.close();
                } catch (final SQLException ex) {
                    System.out.println(ex);
                }
            }
            if (pstmt != null) {
                try {
                    pstmt.close();
                } catch (final SQLException ex) {
                    System.out.println(ex);
                }
            }
        }

        return b;
    }

    /**
     * Holt ein Konto aus der Datenbank.
     * 
     * @param Connection cn
     * @param String kontoname
     * @return Konto k
     */
    public static Konto getTestKontoFromDb(final String kontoname, final Connection cn) {
        Konto k = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            pstmt = cn.prepareStatement("SELECT * FROM `konto` AS k WHERE k.biblioname = ?");
            pstmt.setString(1, kontoname);
            rs = pstmt.executeQuery();

            while (rs.next()) {
                k = new Konto(rs);
            }
        } catch (final Exception err) {
            final String f = "getTestKontoFromDb(String kontoname, Connection cn) trat folgender Fehler auf:\n";
            System.out.println(f + err);
        } finally {
            if (rs != null) {
                try {
                    rs.close();
                } catch (final SQLException e) {
                    final Logger LOG = LoggerFactory.getLogger(PrepareTestObjects.class);
                    LOG.error(e.toString());
                }
            }
            if (pstmt != null) {
                try {
                    pstmt.close();
                } catch (final SQLException e) {
                    final Logger LOG = LoggerFactory.getLogger(PrepareTestObjects.class);
                    LOG.error(e.toString());
                }
            }
        }

        return k;
    }

    /**
     * Erstellt ein TestBibliothekarObjekt
     * 
     * @return Bibliothekar
     */
    public static Bibliothekar getBibliothekar() {

        final Text t = new Text();

        final Bibliothekar b = new Bibliothekar(getUserForm(), t.getConnection());

        t.close();

        return b;
    }

    /**
     * Erstellt ein TestPositionenObjekt
     * 
     * @return Position
     */
    public static Position getPosition() {
        final Position p = new Position();
        p.setAutor("autor");
        p.setBenutzer(getTestBibliothekarFromDb(p.getConnection()));
        p.setDeloptions("deloptions");
        p.setFileformat("fileformat");
        p.setHeft("heft");
        p.setJahr("jahr");
        p.setJahrgang("jahrgang");
        p.setKapitel("kapitel");
        p.setKonto(getTestKontoFromDb("Bibliothek soH/BZ-GS", p.getConnection()));
        p.setMediatype("mediatype");
        p.setOrderdate(new java.sql.Date(1));
        p.setPreis("preis");
        p.setPriority("priority");
        p.setSeiten("seiten");
        p.setTitel("titel");
        p.setWaehrung("waehrung");
        p.setZeitschrift_verlag("zeitschrift_verlag");

        p.close();
        return p;
    }

}
