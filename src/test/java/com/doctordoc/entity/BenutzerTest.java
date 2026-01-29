//  Copyright (C) 2005 - 2010  Markus Fischer, Pascal Steiner
//
//  This program is free software; you can redistribute it and/or
//  modify it under the terms of the GNU General Public License
//  as published by the Free Software Foundation; version 2 of the License.

package com.doctordoc.entity;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for Benutzer entity.
 */
class BenutzerTest {

    private Benutzer benutzer;

    @BeforeEach
    void setUp() {
        benutzer = new Benutzer();
    }

    @Nested
    @DisplayName("Role Constants")
    class RoleConstants {

        @Test
        @DisplayName("Should have correct role values")
        void testRoleValues() {
            assertEquals(1, Benutzer.ROLE_USER);
            assertEquals(2, Benutzer.ROLE_LIBRARIAN);
            assertEquals(3, Benutzer.ROLE_ADMIN);
        }
    }

    @Nested
    @DisplayName("Role Checks")
    class RoleChecks {

        @Test
        @DisplayName("Should identify admin correctly")
        void testIsAdmin() {
            benutzer.setRechte(Benutzer.ROLE_ADMIN);

            assertTrue(benutzer.isAdmin());
            assertFalse(benutzer.isLibrarian());
            assertFalse(benutzer.isUser());
        }

        @Test
        @DisplayName("Should identify librarian correctly")
        void testIsLibrarian() {
            benutzer.setRechte(Benutzer.ROLE_LIBRARIAN);

            assertFalse(benutzer.isAdmin());
            assertTrue(benutzer.isLibrarian());
            assertFalse(benutzer.isUser());
        }

        @Test
        @DisplayName("Should identify user correctly")
        void testIsUser() {
            benutzer.setRechte(Benutzer.ROLE_USER);

            assertFalse(benutzer.isAdmin());
            assertFalse(benutzer.isLibrarian());
            assertTrue(benutzer.isUser());
        }

        @Test
        @DisplayName("Should have default role as user")
        void testDefaultRole() {
            assertEquals(Benutzer.ROLE_USER, benutzer.getRechte());
            assertTrue(benutzer.isUser());
        }
    }

    @Nested
    @DisplayName("Role Name")
    class RoleName {

        @Test
        @DisplayName("Should return ADMIN for admin role")
        void testRoleNameAdmin() {
            benutzer.setRechte(Benutzer.ROLE_ADMIN);

            assertEquals("ADMIN", benutzer.getRoleName());
        }

        @Test
        @DisplayName("Should return LIBRARIAN for librarian role")
        void testRoleNameLibrarian() {
            benutzer.setRechte(Benutzer.ROLE_LIBRARIAN);

            assertEquals("LIBRARIAN", benutzer.getRoleName());
        }

        @Test
        @DisplayName("Should return USER for user role")
        void testRoleNameUser() {
            benutzer.setRechte(Benutzer.ROLE_USER);

            assertEquals("USER", benutzer.getRoleName());
        }

        @Test
        @DisplayName("Should return USER for unknown role")
        void testRoleNameUnknown() {
            benutzer.setRechte(99);

            assertEquals("USER", benutzer.getRoleName());
        }
    }

    @Nested
    @DisplayName("Full Name")
    class FullName {

        @Test
        @DisplayName("Should return full name with both parts")
        void testFullNameBothParts() {
            benutzer.setVorname("Max");
            benutzer.setName("Muster");

            assertEquals("Max Muster", benutzer.getFullName());
        }

        @Test
        @DisplayName("Should return only vorname when name is null")
        void testFullNameOnlyVorname() {
            benutzer.setVorname("Max");
            benutzer.setName(null);

            assertEquals("Max", benutzer.getFullName());
        }

        @Test
        @DisplayName("Should return only vorname when name is empty")
        void testFullNameNameEmpty() {
            benutzer.setVorname("Max");
            benutzer.setName("");

            assertEquals("Max", benutzer.getFullName());
        }

        @Test
        @DisplayName("Should return only name when vorname is null")
        void testFullNameOnlyName() {
            benutzer.setVorname(null);
            benutzer.setName("Muster");

            assertEquals("Muster", benutzer.getFullName());
        }

        @Test
        @DisplayName("Should return only name when vorname is empty")
        void testFullNameVornameEmpty() {
            benutzer.setVorname("");
            benutzer.setName("Muster");

            assertEquals("Muster", benutzer.getFullName());
        }

        @Test
        @DisplayName("Should return empty string when both are null")
        void testFullNameBothNull() {
            benutzer.setVorname(null);
            benutzer.setName(null);

            assertEquals("", benutzer.getFullName());
        }

        @Test
        @DisplayName("Should return empty string when both are empty")
        void testFullNameBothEmpty() {
            benutzer.setVorname("");
            benutzer.setName("");

            assertEquals("", benutzer.getFullName());
        }
    }

    @Nested
    @DisplayName("Librarycard Truncation")
    class LibrarycardTruncation {

        @Test
        @DisplayName("Should truncate librarycard longer than 50 chars")
        void testLibrarycardTruncation() {
            String longCard = "A".repeat(60);
            benutzer.setLibrarycard(longCard);

            assertEquals(49, benutzer.getLibrarycard().length());
        }

        @Test
        @DisplayName("Should not truncate librarycard with 50 chars")
        void testLibrarycardNoTruncation() {
            String card = "A".repeat(50);
            benutzer.setLibrarycard(card);

            assertEquals(50, benutzer.getLibrarycard().length());
        }

        @Test
        @DisplayName("Should handle null librarycard")
        void testLibrarycardNull() {
            benutzer.setLibrarycard(null);

            assertNull(benutzer.getLibrarycard());
        }
    }

    @Nested
    @DisplayName("Default Values")
    class DefaultValues {

        @Test
        @DisplayName("Should have empty anrede by default")
        void testDefaultAnrede() {
            assertEquals("", benutzer.getAnrede());
        }

        @Test
        @DisplayName("Should have empty librarycard by default")
        void testDefaultLibrarycard() {
            assertEquals("", benutzer.getLibrarycard());
        }

        @Test
        @DisplayName("Should have empty kontos set by default")
        void testDefaultKontos() {
            assertNotNull(benutzer.getKontos());
            assertTrue(benutzer.getKontos().isEmpty());
        }
    }

    @Nested
    @DisplayName("Getters and Setters")
    class GettersSetters {

        @Test
        @DisplayName("Should set and get ID")
        void testIdGetterSetter() {
            benutzer.setId(123L);

            assertEquals(123L, benutzer.getId());
        }

        @Test
        @DisplayName("Should set and get institut")
        void testInstitutGetterSetter() {
            benutzer.setInstitut("Test Institut");

            assertEquals("Test Institut", benutzer.getInstitut());
        }

        @Test
        @DisplayName("Should set and get abteilung")
        void testAbteilungGetterSetter() {
            benutzer.setAbteilung("IT");

            assertEquals("IT", benutzer.getAbteilung());
        }

        @Test
        @DisplayName("Should set and get categoryId")
        void testCategoryIdGetterSetter() {
            benutzer.setCategoryId(5L);

            assertEquals(5L, benutzer.getCategoryId());
        }

        @Test
        @DisplayName("Should set and get email")
        void testEmailGetterSetter() {
            benutzer.setEmail("test@example.com");

            assertEquals("test@example.com", benutzer.getEmail());
        }

        @Test
        @DisplayName("Should set and get password")
        void testPasswordGetterSetter() {
            benutzer.setPassword("secret");

            assertEquals("secret", benutzer.getPassword());
        }

        @Test
        @DisplayName("Should set and get address fields")
        void testAddressFields() {
            benutzer.setAdresse("Teststrasse 1");
            benutzer.setAdresszusatz("Postfach 123");
            benutzer.setPlz("12345");
            benutzer.setOrt("Teststadt");
            benutzer.setLand("CH");

            assertEquals("Teststrasse 1", benutzer.getAdresse());
            assertEquals("Postfach 123", benutzer.getAdresszusatz());
            assertEquals("12345", benutzer.getPlz());
            assertEquals("Teststadt", benutzer.getOrt());
            assertEquals("CH", benutzer.getLand());
        }

        @Test
        @DisplayName("Should set and get phone numbers")
        void testPhoneNumbers() {
            benutzer.setTelefonnrp("+41 44 111 2222");
            benutzer.setTelefonnrg("+41 44 333 4444");

            assertEquals("+41 44 111 2222", benutzer.getTelefonnrp());
            assertEquals("+41 44 333 4444", benutzer.getTelefonnrg());
        }

        @Test
        @DisplayName("Should set and get boolean flags")
        void testBooleanFlags() {
            benutzer.setValidation(true);
            benutzer.setLoginopt(true);
            benutzer.setUserbestellung(true);
            benutzer.setGbvbestellung(true);
            benutzer.setKontostatus(true);

            assertTrue(benutzer.isValidation());
            assertTrue(benutzer.isLoginopt());
            assertTrue(benutzer.isUserbestellung());
            assertTrue(benutzer.isGbvbestellung());
            assertTrue(benutzer.isKontostatus());
        }

        @Test
        @DisplayName("Should set and get billing")
        void testBillingGetterSetter() {
            benutzer.setBilling(100L);

            assertEquals(100L, benutzer.getBilling());
        }

        @Test
        @DisplayName("Should set and get timestamps")
        void testTimestamps() {
            LocalDateTime now = LocalDateTime.now();
            benutzer.setDatum(now);
            benutzer.setLastuse(now);
            benutzer.setGtcdate(now);

            assertEquals(now, benutzer.getDatum());
            assertEquals(now, benutzer.getLastuse());
            assertEquals(now, benutzer.getGtcdate());
        }

        @Test
        @DisplayName("Should set and get GTC")
        void testGtcGetterSetter() {
            benutzer.setGtc("accepted");

            assertEquals("accepted", benutzer.getGtc());
        }

        @Test
        @DisplayName("Should set and get kontos")
        void testKontosGetterSetter() {
            Set<Konto> kontos = new HashSet<>();
            Konto konto = new Konto();
            konto.setId(1L);
            kontos.add(konto);
            benutzer.setKontos(kontos);

            assertEquals(1, benutzer.getKontos().size());
        }
    }
}
