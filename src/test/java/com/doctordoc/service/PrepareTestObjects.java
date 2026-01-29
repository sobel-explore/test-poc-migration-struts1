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

package com.doctordoc.service;

import java.time.LocalDateTime;

import com.doctordoc.dto.KontoDto;
import com.doctordoc.dto.LoginDto;
import com.doctordoc.dto.UserDto;
import com.doctordoc.entity.Benutzer;
import com.doctordoc.entity.Konto;

/**
 * Helper class for creating test objects.
 * Migrated from test.PrepareTestObjects for Spring Boot compatibility.
 */
public class PrepareTestObjects {

    public static final String LOGIN_PASSWORD = "testpw";
    public static final String TEST_EMAIL = "testitestmail@eldali.ch";
    public static final Long KONTO_ID = 1L;
    public static final String BIBLIO_NAME_1 = "Biblioname1";
    public static final String BIBLIO_NAME_2 = "Biblioname2";

    /**
     * Creates a test LoginDto with link resolver data.
     */
    public static LoginDto getLoginDto() {
        return new LoginDto(
            TEST_EMAIL,
            LOGIN_PASSWORD,
            null,
            null,
            false,
            "", "", "", "", "", null,
            "", "", "", "", "", "", "", "", "", "", "", "", "", "Artikel", "0"
        );
    }

    /**
     * Creates a test KontoDto.
     */
    public static KontoDto getKontoDto() {
        KontoDto kontoDto = new KontoDto();
        kontoDto.setBibliotheksname(BIBLIO_NAME_1);
        kontoDto.setAdresse("Testadresse");
        kontoDto.setPlz("1234");
        kontoDto.setLand("CH");
        kontoDto.setTelefon("12345678");
        kontoDto.setBibliotheksmail("info@doctor-doc.com");
        kontoDto.setDbsmail("dbs@mail.bl");
        kontoDto.setUserlogin(false);
        kontoDto.setEzbid("");
        return kontoDto;
    }

    /**
     * Creates a test Konto entity.
     */
    public static Konto getKonto() {
        Konto konto = new Konto();
        konto.setBibliotheksname(BIBLIO_NAME_1);
        konto.setAdresse("Testadresse");
        konto.setPlz("1234");
        konto.setOrt("Testort");
        konto.setLand("CH");
        konto.setTelefon("12345678");
        konto.setBibliotheksmail("info@doctor-doc.com");
        konto.setDbsmail("dbs@mail.bl");
        konto.setUserlogin(false);
        konto.setKontostatus(true);
        return konto;
    }

    /**
     * Creates a test UserDto.
     */
    public static UserDto getUserDto() {
        UserDto userDto = new UserDto();
        userDto.setAnrede("Herr");
        userDto.setName("Testname");
        userDto.setVorname("Testvorname");
        userDto.setEmail(TEST_EMAIL);
        return userDto;
    }

    /**
     * Creates a test Benutzer (user) entity.
     */
    public static Benutzer getBenutzer() {
        Benutzer benutzer = new Benutzer();
        benutzer.setAnrede("Herr");
        benutzer.setName("Testname");
        benutzer.setVorname("Testvorname");
        benutzer.setEmail(TEST_EMAIL);
        benutzer.setPassword(LOGIN_PASSWORD);
        benutzer.setRechte(Benutzer.ROLE_USER);
        benutzer.setKontostatus(true);
        benutzer.setDatum(LocalDateTime.now());
        return benutzer;
    }

    /**
     * Creates a test Bibliothekar (librarian) entity.
     */
    public static Benutzer getBibliothekar() {
        Benutzer bibliothekar = new Benutzer();
        bibliothekar.setAnrede("Herr");
        bibliothekar.setName("Testname");
        bibliothekar.setVorname("Testvorname");
        bibliothekar.setEmail(TEST_EMAIL);
        bibliothekar.setPassword(LOGIN_PASSWORD);
        bibliothekar.setRechte(Benutzer.ROLE_LIBRARIAN);
        bibliothekar.setKontostatus(true);
        bibliothekar.setDatum(LocalDateTime.now());
        return bibliothekar;
    }

    /**
     * Creates a test Administrator entity.
     */
    public static Benutzer getAdministrator() {
        Benutzer admin = new Benutzer();
        admin.setAnrede("Herr");
        admin.setName("Admin");
        admin.setVorname("Test");
        admin.setEmail("admin@test.ch");
        admin.setPassword(LOGIN_PASSWORD);
        admin.setRechte(Benutzer.ROLE_ADMIN);
        admin.setKontostatus(true);
        admin.setDatum(LocalDateTime.now());
        return admin;
    }
}
