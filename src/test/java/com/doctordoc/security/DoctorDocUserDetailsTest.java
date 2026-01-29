//  Copyright (C) 2005 - 2010  Markus Fischer, Pascal Steiner
//
//  This program is free software; you can redistribute it and/or
//  modify it under the terms of the GNU General Public License
//  as published by the Free Software Foundation; version 2 of the License.

package com.doctordoc.security;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;

import com.doctordoc.entity.Benutzer;
import com.doctordoc.entity.Konto;

/**
 * Unit tests for DoctorDocUserDetails.
 */
class DoctorDocUserDetailsTest {

    private Benutzer testUser;
    private Benutzer testLibrarian;
    private Benutzer testAdmin;
    private DoctorDocUserDetails userDetails;

    @BeforeEach
    void setUp() {
        testUser = new Benutzer();
        testUser.setId(1L);
        testUser.setEmail("user@test.ch");
        testUser.setPassword("hashedPassword");
        testUser.setVorname("Test");
        testUser.setName("User");
        testUser.setRechte(Benutzer.ROLE_USER);
        testUser.setKontostatus(true);
        testUser.setLoginopt(true);

        testLibrarian = new Benutzer();
        testLibrarian.setId(2L);
        testLibrarian.setEmail("librarian@test.ch");
        testLibrarian.setRechte(Benutzer.ROLE_LIBRARIAN);
        testLibrarian.setKontostatus(true);
        testLibrarian.setLoginopt(true);

        testAdmin = new Benutzer();
        testAdmin.setId(3L);
        testAdmin.setEmail("admin@test.ch");
        testAdmin.setRechte(Benutzer.ROLE_ADMIN);
        testAdmin.setKontostatus(true);
        testAdmin.setLoginopt(true);
    }

    @Nested
    @DisplayName("UserDetails Interface Implementation")
    class UserDetailsInterfaceTests {

        @Test
        @DisplayName("Should return correct username (email)")
        void testGetUsername() {
            userDetails = new DoctorDocUserDetails(testUser);

            assertEquals("user@test.ch", userDetails.getUsername());
        }

        @Test
        @DisplayName("Should return correct password")
        void testGetPassword() {
            userDetails = new DoctorDocUserDetails(testUser);

            assertEquals("hashedPassword", userDetails.getPassword());
        }

        @Test
        @DisplayName("Should return true for non-expired account")
        void testIsAccountNonExpired() {
            userDetails = new DoctorDocUserDetails(testUser);

            assertTrue(userDetails.isAccountNonExpired());
        }

        @Test
        @DisplayName("Should return account locked status based on kontostatus")
        void testIsAccountNonLocked() {
            userDetails = new DoctorDocUserDetails(testUser);
            assertTrue(userDetails.isAccountNonLocked());

            testUser.setKontostatus(false);
            userDetails = new DoctorDocUserDetails(testUser);
            assertFalse(userDetails.isAccountNonLocked());
        }

        @Test
        @DisplayName("Should return true for non-expired credentials")
        void testIsCredentialsNonExpired() {
            userDetails = new DoctorDocUserDetails(testUser);

            assertTrue(userDetails.isCredentialsNonExpired());
        }

        @Test
        @DisplayName("Should return enabled status based on kontostatus and loginopt")
        void testIsEnabled() {
            userDetails = new DoctorDocUserDetails(testUser);
            assertTrue(userDetails.isEnabled());

            testUser.setKontostatus(false);
            userDetails = new DoctorDocUserDetails(testUser);
            assertFalse(userDetails.isEnabled());

            testUser.setKontostatus(true);
            testUser.setLoginopt(false);
            userDetails = new DoctorDocUserDetails(testUser);
            assertFalse(userDetails.isEnabled());
        }
    }

    @Nested
    @DisplayName("Authorities Tests")
    class AuthoritiesTests {

        @Test
        @DisplayName("Should return ROLE_USER for regular user")
        void testGetAuthoritiesUser() {
            userDetails = new DoctorDocUserDetails(testUser);

            Collection<? extends GrantedAuthority> authorities = userDetails.getAuthorities();

            assertEquals(1, authorities.size());
            assertTrue(authorities.stream().anyMatch(a -> a.getAuthority().equals("ROLE_USER")));
        }

        @Test
        @DisplayName("Should return ROLE_LIBRARIAN for librarian")
        void testGetAuthoritiesLibrarian() {
            userDetails = new DoctorDocUserDetails(testLibrarian);

            Collection<? extends GrantedAuthority> authorities = userDetails.getAuthorities();

            assertEquals(1, authorities.size());
            assertTrue(authorities.stream().anyMatch(a -> a.getAuthority().equals("ROLE_LIBRARIAN")));
        }

        @Test
        @DisplayName("Should return ROLE_ADMIN for admin")
        void testGetAuthoritiesAdmin() {
            userDetails = new DoctorDocUserDetails(testAdmin);

            Collection<? extends GrantedAuthority> authorities = userDetails.getAuthorities();

            assertEquals(1, authorities.size());
            assertTrue(authorities.stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")));
        }
    }

    @Nested
    @DisplayName("Convenience Methods")
    class ConvenienceMethods {

        @Test
        @DisplayName("Should return benutzer entity")
        void testGetBenutzer() {
            userDetails = new DoctorDocUserDetails(testUser);

            assertEquals(testUser, userDetails.getBenutzer());
        }

        @Test
        @DisplayName("Should return user ID")
        void testGetUserId() {
            userDetails = new DoctorDocUserDetails(testUser);

            assertEquals(1L, userDetails.getUserId());
        }

        @Test
        @DisplayName("Should return full name")
        void testGetFullName() {
            userDetails = new DoctorDocUserDetails(testUser);

            assertEquals("Test User", userDetails.getFullName());
        }

        @Test
        @DisplayName("Should correctly identify user role")
        void testIsUser() {
            userDetails = new DoctorDocUserDetails(testUser);
            assertTrue(userDetails.isUser());
            assertFalse(userDetails.isLibrarian());
            assertFalse(userDetails.isAdmin());
        }

        @Test
        @DisplayName("Should correctly identify librarian role")
        void testIsLibrarian() {
            userDetails = new DoctorDocUserDetails(testLibrarian);
            assertFalse(userDetails.isUser());
            assertTrue(userDetails.isLibrarian());
            assertFalse(userDetails.isAdmin());
        }

        @Test
        @DisplayName("Should correctly identify admin role")
        void testIsAdmin() {
            userDetails = new DoctorDocUserDetails(testAdmin);
            assertFalse(userDetails.isUser());
            assertFalse(userDetails.isLibrarian());
            assertTrue(userDetails.isAdmin());
        }
    }

    @Nested
    @DisplayName("Konto Management")
    class KontoManagement {

        @Test
        @DisplayName("Should set and get active konto")
        void testActiveKonto() {
            userDetails = new DoctorDocUserDetails(testUser);
            Konto konto = new Konto();
            konto.setId(1L);
            konto.setBibliotheksname("Test Bibliothek");

            userDetails.setActiveKonto(konto);

            assertEquals(konto, userDetails.getActiveKonto());
            assertEquals("Test Bibliothek", userDetails.getActiveKonto().getBibliotheksname());
        }

        @Test
        @DisplayName("Should set and get available kontos")
        void testAvailableKontos() {
            userDetails = new DoctorDocUserDetails(testUser);
            List<Konto> kontos = new ArrayList<>();
            kontos.add(new Konto());
            kontos.add(new Konto());

            userDetails.setAvailableKontos(kontos);

            assertEquals(2, userDetails.getAvailableKontos().size());
        }

        @Test
        @DisplayName("Should detect multiple kontos")
        void testHasMultipleKontos() {
            userDetails = new DoctorDocUserDetails(testUser);

            userDetails.setAvailableKontos(null);
            assertFalse(userDetails.hasMultipleKontos());

            List<Konto> kontos = new ArrayList<>();
            kontos.add(new Konto());
            userDetails.setAvailableKontos(kontos);
            assertFalse(userDetails.hasMultipleKontos());

            kontos.add(new Konto());
            userDetails.setAvailableKontos(kontos);
            assertTrue(userDetails.hasMultipleKontos());
        }
    }
}
