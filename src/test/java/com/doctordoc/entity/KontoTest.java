//  Copyright (C) 2005 - 2010  Markus Fischer, Pascal Steiner
//
//  This program is free software; you can redistribute it and/or
//  modify it under the terms of the GNU General Public License
//  as published by the Free Software Foundation; version 2 of the License.

package com.doctordoc.entity;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for Konto entity.
 */
class KontoTest {

    private Konto konto;

    @BeforeEach
    void setUp() {
        konto = new Konto();
    }

    @Nested
    @DisplayName("Default Values")
    class DefaultValues {

        @Test
        @DisplayName("Should have default timezone")
        void testDefaultTimezone() {
            assertEquals("Europe/Berlin", konto.getTimezone());
        }

        @Test
        @DisplayName("Should have default delivery options")
        void testDefaultDeloptions() {
            assertEquals("post", konto.getDefaultDeloptions());
        }

        @Test
        @DisplayName("Should have showpubsuppliers true by default")
        void testDefaultShowpubsuppliers() {
            assertTrue(konto.isShowpubsuppliers());
        }

        @Test
        @DisplayName("Should have showprivsuppliers false by default")
        void testDefaultShowprivsuppliers() {
            assertFalse(konto.isShowprivsuppliers());
        }

        @Test
        @DisplayName("Should have kontostatus false by default")
        void testDefaultKontostatus() {
            assertFalse(konto.isKontostatus());
        }

        @Test
        @DisplayName("Should have userlogin false by default")
        void testDefaultUserlogin() {
            assertFalse(konto.isUserlogin());
        }
    }

    @Nested
    @DisplayName("Basic Getters and Setters")
    class BasicGettersSetters {

        @Test
        @DisplayName("Should set and get ID")
        void testIdGetterSetter() {
            konto.setId(123L);
            assertEquals(123L, konto.getId());
        }

        @Test
        @DisplayName("Should set and get DID")
        void testDidGetterSetter() {
            konto.setDid(456L);
            assertEquals(456L, konto.getDid());
        }

        @Test
        @DisplayName("Should set and get bibliotheksname")
        void testBibliotheksnameGetterSetter() {
            konto.setBibliotheksname("Test Bibliothek");
            assertEquals("Test Bibliothek", konto.getBibliotheksname());
        }

        @Test
        @DisplayName("Should set and get ISIL")
        void testIsilGetterSetter() {
            konto.setIsil("DE-123");
            assertEquals("DE-123", konto.getIsil());
        }

        @Test
        @DisplayName("Should set and get timezone")
        void testTimezoneGetterSetter() {
            konto.setTimezone("America/New_York");
            assertEquals("America/New_York", konto.getTimezone());
        }
    }

    @Nested
    @DisplayName("Address Fields")
    class AddressFields {

        @Test
        @DisplayName("Should set and get address")
        void testAdresseGetterSetter() {
            konto.setAdresse("Teststrasse 1");
            assertEquals("Teststrasse 1", konto.getAdresse());
        }

        @Test
        @DisplayName("Should set and get address supplement")
        void testAdressenzusatzGetterSetter() {
            konto.setAdressenzusatz("Postfach 123");
            assertEquals("Postfach 123", konto.getAdressenzusatz());
        }

        @Test
        @DisplayName("Should set and get PLZ")
        void testPlzGetterSetter() {
            konto.setPlz("12345");
            assertEquals("12345", konto.getPlz());
        }

        @Test
        @DisplayName("Should set and get Ort")
        void testOrtGetterSetter() {
            konto.setOrt("Teststadt");
            assertEquals("Teststadt", konto.getOrt());
        }

        @Test
        @DisplayName("Should set and get Land")
        void testLandGetterSetter() {
            konto.setLand("CH");
            assertEquals("CH", konto.getLand());
        }
    }

    @Nested
    @DisplayName("Fax Fields")
    class FaxFields {

        @Test
        @DisplayName("Should set and get fax number")
        void testFaxnoGetterSetter() {
            konto.setFaxno("+41 44 123 4567");
            assertEquals("+41 44 123 4567", konto.getFaxno());
        }

        @Test
        @DisplayName("Should set and get fax username")
        void testFaxusernameGetterSetter() {
            konto.setFaxusername("faxuser");
            assertEquals("faxuser", konto.getFaxusername());
        }

        @Test
        @DisplayName("Should set and get fax password")
        void testFaxpasswordGetterSetter() {
            konto.setFaxpassword("faxpass");
            assertEquals("faxpass", konto.getFaxpassword());
        }

        @Test
        @DisplayName("Should set and get popfax end date")
        void testPopfaxendGetterSetter() {
            LocalDate date = LocalDate.of(2025, 12, 31);
            konto.setPopfaxend(date);
            assertEquals(date, konto.getPopfaxend());
        }

        @Test
        @DisplayName("Should set and get external fax")
        void testFaxExternGetterSetter() {
            konto.setFaxExtern("+41 44 987 6543");
            assertEquals("+41 44 987 6543", konto.getFaxExtern());
        }
    }

    @Nested
    @DisplayName("Contact Fields")
    class ContactFields {

        @Test
        @DisplayName("Should set and get telefon")
        void testTelefonGetterSetter() {
            konto.setTelefon("+41 44 111 2222");
            assertEquals("+41 44 111 2222", konto.getTelefon());
        }

        @Test
        @DisplayName("Should set and get bibliotheksmail")
        void testBibliotheksmailGetterSetter() {
            konto.setBibliotheksmail("bib@example.com");
            assertEquals("bib@example.com", konto.getBibliotheksmail());
        }

        @Test
        @DisplayName("Should set and get dbsmail")
        void testDbsmailGetterSetter() {
            konto.setDbsmail("dbs@example.com");
            assertEquals("dbs@example.com", konto.getDbsmail());
        }

        @Test
        @DisplayName("Should set and get dbsmailpw")
        void testDbsmailpwGetterSetter() {
            konto.setDbsmailpw("dbspassword");
            assertEquals("dbspassword", konto.getDbsmailpw());
        }
    }

    @Nested
    @DisplayName("GBV Fields")
    class GbvFields {

        @Test
        @DisplayName("Should set and get GBV benutzername")
        void testGbvbenutzernameGetterSetter() {
            konto.setGbvbenutzername("gbvuser");
            assertEquals("gbvuser", konto.getGbvbenutzername());
        }

        @Test
        @DisplayName("Should set and get GBV passwort")
        void testGbvpasswortGetterSetter() {
            konto.setGbvpasswort("gbvpass");
            assertEquals("gbvpass", konto.getGbvpasswort());
        }

        @Test
        @DisplayName("Should set and get GBV requester ID")
        void testGbvrequesteridGetterSetter() {
            konto.setGbvrequesterid("GBV-12345");
            assertEquals("GBV-12345", konto.getGbvrequesterid());
        }

        @Test
        @DisplayName("Should set and get gbvbestellung")
        void testGbvbestellungGetterSetter() {
            konto.setGbvbestellung(true);
            assertTrue(konto.isGbvbestellung());
        }
    }

    @Nested
    @DisplayName("IDS Fields")
    class IdsFields {

        @Test
        @DisplayName("Should set and get IDS ID")
        void testIdsidGetterSetter() {
            konto.setIdsid("IDS-001");
            assertEquals("IDS-001", konto.getIdsid());
        }

        @Test
        @DisplayName("Should set and get IDS passwort")
        void testIdspasswortGetterSetter() {
            konto.setIdspasswort("idspass");
            assertEquals("idspass", konto.getIdspasswort());
        }
    }

    @Nested
    @DisplayName("Billing Fields")
    class BillingFields {

        @Test
        @DisplayName("Should set and get billing")
        void testBillingGetterSetter() {
            konto.setBilling(100L);
            assertEquals(100L, konto.getBilling());
        }

        @Test
        @DisplayName("Should set and get billingtype")
        void testBillingtypeGetterSetter() {
            konto.setBillingtype(2L);
            assertEquals(2L, konto.getBillingtype());
        }

        @Test
        @DisplayName("Should set and get threshold value")
        void testThresholdValueGetterSetter() {
            konto.setThresholdValue(500);
            assertEquals(500, konto.getThresholdValue());
        }

        @Test
        @DisplayName("Should set and get accounting rhythm")
        void testAccountingRhythm() {
            konto.setAccountingRhythmvalue(30L);
            konto.setAccountingRhythmday(15L);
            konto.setAccountingRhythmtimeout(60L);

            assertEquals(30L, konto.getAccountingRhythmvalue());
            assertEquals(15L, konto.getAccountingRhythmday());
            assertEquals(60L, konto.getAccountingRhythmtimeout());
        }
    }

    @Nested
    @DisplayName("Order Limits")
    class OrderLimits {

        @Test
        @DisplayName("Should set and get maxordersu")
        void testMaxordersuGetterSetter() {
            konto.setMaxordersu(50L);
            assertEquals(50L, konto.getMaxordersu());
        }

        @Test
        @DisplayName("Should set and get maxordersutotal")
        void testMaxordersutotalGetterSetter() {
            konto.setMaxordersutotal(100L);
            assertEquals(100L, konto.getMaxordersutotal());
        }

        @Test
        @DisplayName("Should set and get maxordersj")
        void testMaxordersjGetterSetter() {
            konto.setMaxordersj(500L);
            assertEquals(500L, konto.getMaxordersj());
        }

        @Test
        @DisplayName("Should set and get orderlimits")
        void testOrderlimitsGetterSetter() {
            konto.setOrderlimits(1);
            assertEquals(1, konto.getOrderlimits());
        }
    }

    @Nested
    @DisplayName("Status and Type Fields")
    class StatusTypeFields {

        @Test
        @DisplayName("Should set and get userlogin")
        void testUserloginGetterSetter() {
            konto.setUserlogin(true);
            assertTrue(konto.isUserlogin());
        }

        @Test
        @DisplayName("Should set and get userbestellung")
        void testUserbestellungGetterSetter() {
            konto.setUserbestellung(true);
            assertTrue(konto.isUserbestellung());
        }

        @Test
        @DisplayName("Should set and get kontostatus")
        void testKontostatusGetterSetter() {
            konto.setKontostatus(true);
            assertTrue(konto.isKontostatus());
        }

        @Test
        @DisplayName("Should set and get kontotyp")
        void testKontotypGetterSetter() {
            konto.setKontotyp(2);
            assertEquals(2, konto.getKontotyp());
        }

        @Test
        @DisplayName("Should set and get defaultDeloptions")
        void testDefaultDeloptionsGetterSetter() {
            konto.setDefaultDeloptions("email");
            assertEquals("email", konto.getDefaultDeloptions());
        }
    }

    @Nested
    @DisplayName("Date Fields")
    class DateFields {

        @Test
        @DisplayName("Should set and get paydate")
        void testPaydateGetterSetter() {
            LocalDate date = LocalDate.of(2025, 1, 15);
            konto.setPaydate(date);
            assertEquals(date, konto.getPaydate());
        }

        @Test
        @DisplayName("Should set and get expdate")
        void testExpdateGetterSetter() {
            LocalDate date = LocalDate.of(2025, 12, 31);
            konto.setExpdate(date);
            assertEquals(date, konto.getExpdate());
        }

        @Test
        @DisplayName("Should set and get edatum")
        void testEdatumGetterSetter() {
            LocalDate date = LocalDate.of(2024, 1, 1);
            konto.setEdatum(date);
            assertEquals(date, konto.getEdatum());
        }
    }

    @Nested
    @DisplayName("GTC Fields")
    class GtcFields {

        @Test
        @DisplayName("Should set and get gtc")
        void testGtcGetterSetter() {
            konto.setGtc("accepted");
            assertEquals("accepted", konto.getGtc());
        }

        @Test
        @DisplayName("Should set and get gtcdate")
        void testGtcdateGetterSetter() {
            LocalDateTime date = LocalDateTime.of(2024, 6, 15, 10, 30);
            konto.setGtcdate(date);
            assertEquals(date, konto.getGtcdate());
        }
    }

    @Nested
    @DisplayName("Miscellaneous Fields")
    class MiscFields {

        @Test
        @DisplayName("Should set and get ezbid")
        void testEzbidGetterSetter() {
            konto.setEzbid("EZB-001");
            assertEquals("EZB-001", konto.getEzbid());
        }

        @Test
        @DisplayName("Should set and get instlogolink")
        void testInstlogolinkGetterSetter() {
            konto.setInstlogolink("https://example.com/logo.png");
            assertEquals("https://example.com/logo.png", konto.getInstlogolink());
        }

        @Test
        @DisplayName("Should set and get zdb")
        void testZdbGetterSetter() {
            konto.setZdb(true);
            assertTrue(konto.isZdb());
        }

        @Test
        @DisplayName("Should set and get showprivsuppliers")
        void testShowprivsuppliersGetterSetter() {
            konto.setShowprivsuppliers(true);
            assertTrue(konto.isShowprivsuppliers());
        }

        @Test
        @DisplayName("Should set and get showpubsuppliers")
        void testShowpubsuppliersGetterSetter() {
            konto.setShowpubsuppliers(false);
            assertFalse(konto.isShowpubsuppliers());
        }

        @Test
        @DisplayName("Should set and get ilvformnr")
        void testIlvformnrGetterSetter() {
            konto.setIlvformnr(3);
            assertEquals(3, konto.getIlvformnr());
        }

        @Test
        @DisplayName("Should set and get selected (transient)")
        void testSelectedGetterSetter() {
            konto.setSelected(true);
            assertTrue(konto.isSelected());
        }
    }
}
