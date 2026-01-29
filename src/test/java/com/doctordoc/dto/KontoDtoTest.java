//  Copyright (C) 2005 - 2010  Markus Fischer, Pascal Steiner
//
//  This program is free software; you can redistribute it and/or
//  modify it under the terms of the GNU General Public License
//  as published by the Free Software Foundation; version 2 of the License.

package com.doctordoc.dto;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

/**
 * Tests for KontoDto validation and defaults.
 */
class KontoDtoTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Nested
    @DisplayName("Default Values Tests")
    class DefaultValuesTests {

        @Test
        @DisplayName("New DTO should have default timezone")
        void testDefaultTimezone() {
            KontoDto dto = new KontoDto();
            assertEquals("Europe/Berlin", dto.getTimezone());
        }

        @Test
        @DisplayName("New DTO should have default deloptions")
        void testDefaultDeloptions() {
            KontoDto dto = new KontoDto();
            assertEquals("post", dto.getDefaultDeloptions());
        }

        @Test
        @DisplayName("New DTO should have kontostatus true by default")
        void testDefaultKontostatus() {
            KontoDto dto = new KontoDto();
            assertTrue(dto.isKontostatus());
        }

        @Test
        @DisplayName("New DTO should have showpubsuppliers true by default")
        void testDefaultShowpubsuppliers() {
            KontoDto dto = new KontoDto();
            assertTrue(dto.isShowpubsuppliers());
        }
    }

    @Nested
    @DisplayName("Getter/Setter Tests")
    class GetterSetterTests {

        @Test
        @DisplayName("Should get and set id")
        void testId() {
            KontoDto dto = new KontoDto();
            dto.setId(1L);
            assertEquals(1L, dto.getId());
        }

        @Test
        @DisplayName("Should get and set bibliotheksname")
        void testBibliotheksname() {
            KontoDto dto = new KontoDto();
            dto.setBibliotheksname("Test Library");
            assertEquals("Test Library", dto.getBibliotheksname());
        }

        @Test
        @DisplayName("Should get and set isil")
        void testIsil() {
            KontoDto dto = new KontoDto();
            dto.setIsil("DE-123");
            assertEquals("DE-123", dto.getIsil());
        }

        @Test
        @DisplayName("Should get and set adresse")
        void testAdresse() {
            KontoDto dto = new KontoDto();
            dto.setAdresse("Test Street 1");
            assertEquals("Test Street 1", dto.getAdresse());
        }

        @Test
        @DisplayName("Should get and set adressenzusatz")
        void testAdressenzusatz() {
            KontoDto dto = new KontoDto();
            dto.setAdressenzusatz("Building A");
            assertEquals("Building A", dto.getAdressenzusatz());
        }

        @Test
        @DisplayName("Should get and set plz")
        void testPlz() {
            KontoDto dto = new KontoDto();
            dto.setPlz("12345");
            assertEquals("12345", dto.getPlz());
        }

        @Test
        @DisplayName("Should get and set ort")
        void testOrt() {
            KontoDto dto = new KontoDto();
            dto.setOrt("Berlin");
            assertEquals("Berlin", dto.getOrt());
        }

        @Test
        @DisplayName("Should get and set land")
        void testLand() {
            KontoDto dto = new KontoDto();
            dto.setLand("DE");
            assertEquals("DE", dto.getLand());
        }

        @Test
        @DisplayName("Should get and set timezone")
        void testTimezone() {
            KontoDto dto = new KontoDto();
            dto.setTimezone("America/New_York");
            assertEquals("America/New_York", dto.getTimezone());
        }

        @Test
        @DisplayName("Should get and set faxno")
        void testFaxno() {
            KontoDto dto = new KontoDto();
            dto.setFaxno("+49 30 12345");
            assertEquals("+49 30 12345", dto.getFaxno());
        }

        @Test
        @DisplayName("Should get and set telefon")
        void testTelefon() {
            KontoDto dto = new KontoDto();
            dto.setTelefon("+49 30 67890");
            assertEquals("+49 30 67890", dto.getTelefon());
        }

        @Test
        @DisplayName("Should get and set bibliotheksmail")
        void testBibliotheksmail() {
            KontoDto dto = new KontoDto();
            dto.setBibliotheksmail("lib@test.de");
            assertEquals("lib@test.de", dto.getBibliotheksmail());
        }

        @Test
        @DisplayName("Should get and set dbsmail")
        void testDbsmail() {
            KontoDto dto = new KontoDto();
            dto.setDbsmail("dbs@test.de");
            assertEquals("dbs@test.de", dto.getDbsmail());
        }

        @Test
        @DisplayName("Should get and set dbsmailpw")
        void testDbsmailpw() {
            KontoDto dto = new KontoDto();
            dto.setDbsmailpw("password123");
            assertEquals("password123", dto.getDbsmailpw());
        }

        @Test
        @DisplayName("Should get and set ezbid")
        void testEzbid() {
            KontoDto dto = new KontoDto();
            dto.setEzbid("EZB-123");
            assertEquals("EZB-123", dto.getEzbid());
        }

        @Test
        @DisplayName("Should get and set instlogolink")
        void testInstlogolink() {
            KontoDto dto = new KontoDto();
            dto.setInstlogolink("http://example.com/logo.png");
            assertEquals("http://example.com/logo.png", dto.getInstlogolink());
        }

        @Test
        @DisplayName("Should get and set zdb")
        void testZdb() {
            KontoDto dto = new KontoDto();
            dto.setZdb(true);
            assertTrue(dto.isZdb());
        }

        @Test
        @DisplayName("Should get and set billing")
        void testBilling() {
            KontoDto dto = new KontoDto();
            dto.setBilling(100L);
            assertEquals(100L, dto.getBilling());
        }

        @Test
        @DisplayName("Should get and set billingtype")
        void testBillingtype() {
            KontoDto dto = new KontoDto();
            dto.setBillingtype(1L);
            assertEquals(1L, dto.getBillingtype());
        }

        @Test
        @DisplayName("Should get and set maxordersu")
        void testMaxordersu() {
            KontoDto dto = new KontoDto();
            dto.setMaxordersu(10L);
            assertEquals(10L, dto.getMaxordersu());
        }

        @Test
        @DisplayName("Should get and set maxordersutotal")
        void testMaxordersutotal() {
            KontoDto dto = new KontoDto();
            dto.setMaxordersutotal(50L);
            assertEquals(50L, dto.getMaxordersutotal());
        }

        @Test
        @DisplayName("Should get and set maxordersj")
        void testMaxordersj() {
            KontoDto dto = new KontoDto();
            dto.setMaxordersj(100L);
            assertEquals(100L, dto.getMaxordersj());
        }

        @Test
        @DisplayName("Should get and set orderlimits")
        void testOrderlimits() {
            KontoDto dto = new KontoDto();
            dto.setOrderlimits(5);
            assertEquals(5, dto.getOrderlimits());
        }

        @Test
        @DisplayName("Should get and set userlogin")
        void testUserlogin() {
            KontoDto dto = new KontoDto();
            dto.setUserlogin(true);
            assertTrue(dto.isUserlogin());
        }

        @Test
        @DisplayName("Should get and set userbestellung")
        void testUserbestellung() {
            KontoDto dto = new KontoDto();
            dto.setUserbestellung(true);
            assertTrue(dto.isUserbestellung());
        }

        @Test
        @DisplayName("Should get and set gbvbestellung")
        void testGbvbestellung() {
            KontoDto dto = new KontoDto();
            dto.setGbvbestellung(true);
            assertTrue(dto.isGbvbestellung());
        }

        @Test
        @DisplayName("Should get and set kontostatus")
        void testKontostatus() {
            KontoDto dto = new KontoDto();
            dto.setKontostatus(false);
            assertFalse(dto.isKontostatus());
        }

        @Test
        @DisplayName("Should get and set kontotyp")
        void testKontotyp() {
            KontoDto dto = new KontoDto();
            dto.setKontotyp(2);
            assertEquals(2, dto.getKontotyp());
        }

        @Test
        @DisplayName("Should get and set defaultDeloptions")
        void testDefaultDeloptions() {
            KontoDto dto = new KontoDto();
            dto.setDefaultDeloptions("email");
            assertEquals("email", dto.getDefaultDeloptions());
        }

        @Test
        @DisplayName("Should get and set showprivsuppliers")
        void testShowprivsuppliers() {
            KontoDto dto = new KontoDto();
            dto.setShowprivsuppliers(true);
            assertTrue(dto.isShowprivsuppliers());
        }

        @Test
        @DisplayName("Should get and set showpubsuppliers")
        void testShowpubsuppliers() {
            KontoDto dto = new KontoDto();
            dto.setShowpubsuppliers(false);
            assertFalse(dto.isShowpubsuppliers());
        }

        @Test
        @DisplayName("Should get and set gbvbenutzername")
        void testGbvbenutzername() {
            KontoDto dto = new KontoDto();
            dto.setGbvbenutzername("gbvuser");
            assertEquals("gbvuser", dto.getGbvbenutzername());
        }

        @Test
        @DisplayName("Should get and set gbvpasswort")
        void testGbvpasswort() {
            KontoDto dto = new KontoDto();
            dto.setGbvpasswort("gbvpass");
            assertEquals("gbvpass", dto.getGbvpasswort());
        }

        @Test
        @DisplayName("Should get and set gbvrequesterid")
        void testGbvrequesterid() {
            KontoDto dto = new KontoDto();
            dto.setGbvrequesterid("REQ-123");
            assertEquals("REQ-123", dto.getGbvrequesterid());
        }

        @Test
        @DisplayName("Should get and set idsid")
        void testIdsid() {
            KontoDto dto = new KontoDto();
            dto.setIdsid("IDS-456");
            assertEquals("IDS-456", dto.getIdsid());
        }

        @Test
        @DisplayName("Should get and set idspasswort")
        void testIdspasswort() {
            KontoDto dto = new KontoDto();
            dto.setIdspasswort("idspass");
            assertEquals("idspass", dto.getIdspasswort());
        }
    }

    @Nested
    @DisplayName("Validation Tests")
    class ValidationTests {

        @Test
        @DisplayName("Should require bibliotheksname")
        void testBibliotheksnameRequired() {
            KontoDto dto = new KontoDto();
            dto.setBibliotheksname(null);
            Set<ConstraintViolation<KontoDto>> violations = validator.validate(dto);
            assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("bibliotheksname")));
        }

        @Test
        @DisplayName("Should reject blank bibliotheksname")
        void testBibliotheksnameBlank() {
            KontoDto dto = new KontoDto();
            dto.setBibliotheksname("");
            Set<ConstraintViolation<KontoDto>> violations = validator.validate(dto);
            assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("bibliotheksname")));
        }

        @Test
        @DisplayName("Should accept valid bibliotheksname")
        void testBibliotheksnameValid() {
            KontoDto dto = new KontoDto();
            dto.setBibliotheksname("Test Library");
            Set<ConstraintViolation<KontoDto>> violations = validator.validate(dto);
            assertFalse(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("bibliotheksname")));
        }

        @Test
        @DisplayName("Should reject invalid email format for bibliotheksmail")
        void testBibliotheksmailInvalid() {
            KontoDto dto = new KontoDto();
            dto.setBibliotheksname("Test");
            dto.setBibliotheksmail("invalid-email");
            Set<ConstraintViolation<KontoDto>> violations = validator.validate(dto);
            assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("bibliotheksmail")));
        }

        @Test
        @DisplayName("Should accept valid email format for bibliotheksmail")
        void testBibliotheksmailValid() {
            KontoDto dto = new KontoDto();
            dto.setBibliotheksname("Test");
            dto.setBibliotheksmail("library@example.com");
            Set<ConstraintViolation<KontoDto>> violations = validator.validate(dto);
            assertFalse(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("bibliotheksmail")));
        }

        @Test
        @DisplayName("Should reject invalid email format for dbsmail")
        void testDbsmailInvalid() {
            KontoDto dto = new KontoDto();
            dto.setBibliotheksname("Test");
            dto.setDbsmail("invalid-email");
            Set<ConstraintViolation<KontoDto>> violations = validator.validate(dto);
            assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("dbsmail")));
        }
    }
}
