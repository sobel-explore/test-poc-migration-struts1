//  Copyright (C) 2005 - 2010  Markus Fischer, Pascal Steiner
//
//  This program is free software; you can redistribute it and/or
//  modify it under the terms of the GNU General Public License
//  as published by the Free Software Foundation; version 2 of the License.

package com.doctordoc.security;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import com.doctordoc.entity.Benutzer;
import com.doctordoc.entity.Konto;
import com.doctordoc.repository.BenutzerRepository;
import com.doctordoc.service.UserService;

/**
 * Unit tests for DoctorDocUserDetailsService.
 */
@ExtendWith(MockitoExtension.class)
class DoctorDocUserDetailsServiceTest {

    @Mock
    private BenutzerRepository benutzerRepository;

    @Mock
    private UserService userService;

    @InjectMocks
    private DoctorDocUserDetailsService userDetailsService;

    private Benutzer testUser;
    private Konto testKonto;

    @BeforeEach
    void setUp() {
        testUser = new Benutzer();
        testUser.setId(1L);
        testUser.setEmail("user@test.ch");
        testUser.setPassword("hashedPassword");
        testUser.setVorname("Test");
        testUser.setName("User");
        testUser.setRechte(Benutzer.ROLE_LIBRARIAN);
        testUser.setKontostatus(true);
        testUser.setLoginopt(true);

        testKonto = new Konto();
        testKonto.setId(1L);
        testKonto.setBibliotheksname("Test Bibliothek");
        testKonto.setKontostatus(true);

        testUser.setKontos(new HashSet<>(Arrays.asList(testKonto)));
    }

    @Nested
    @DisplayName("Load User By Username Tests")
    class LoadUserByUsernameTests {

        @Test
        @DisplayName("Should load user by email")
        void testLoadUserByUsername() {
            when(benutzerRepository.findByEmailAndLoginoptTrue("user@test.ch"))
                    .thenReturn(Optional.of(testUser));
            when(userService.getAllowedKontos(testUser))
                    .thenReturn(Arrays.asList(testKonto));

            UserDetails userDetails = userDetailsService.loadUserByUsername("user@test.ch");

            assertNotNull(userDetails);
            assertEquals("user@test.ch", userDetails.getUsername());
            assertTrue(userDetails instanceof DoctorDocUserDetails);

            DoctorDocUserDetails ddUserDetails = (DoctorDocUserDetails) userDetails;
            assertEquals(1, ddUserDetails.getAvailableKontos().size());
            // Should auto-select when only one konto
            assertNotNull(ddUserDetails.getActiveKonto());
        }

        @Test
        @DisplayName("Should throw exception when user not found")
        void testLoadUserByUsernameNotFound() {
            when(benutzerRepository.findByEmailAndLoginoptTrue("unknown@test.ch"))
                    .thenReturn(Optional.empty());

            assertThrows(UsernameNotFoundException.class, () ->
                    userDetailsService.loadUserByUsername("unknown@test.ch")
            );
        }

        @Test
        @DisplayName("Should not auto-select konto when multiple available")
        void testLoadUserByUsernameMultipleKontos() {
            Konto konto2 = new Konto();
            konto2.setId(2L);
            konto2.setBibliotheksname("Another Bibliothek");

            when(benutzerRepository.findByEmailAndLoginoptTrue("user@test.ch"))
                    .thenReturn(Optional.of(testUser));
            when(userService.getAllowedKontos(testUser))
                    .thenReturn(Arrays.asList(testKonto, konto2));

            UserDetails userDetails = userDetailsService.loadUserByUsername("user@test.ch");

            DoctorDocUserDetails ddUserDetails = (DoctorDocUserDetails) userDetails;
            assertEquals(2, ddUserDetails.getAvailableKontos().size());
            assertNull(ddUserDetails.getActiveKonto());
        }
    }

    @Nested
    @DisplayName("Load User By Email And Password Tests")
    class LoadUserByEmailAndPasswordTests {

        @Test
        @DisplayName("Should load user with valid credentials")
        void testLoadUserByEmailAndPassword() {
            when(benutzerRepository.findByEmailAndPassword("user@test.ch", "hashedPassword"))
                    .thenReturn(Optional.of(testUser));
            when(userService.getAllowedKontos(testUser))
                    .thenReturn(Arrays.asList(testKonto));

            DoctorDocUserDetails userDetails = userDetailsService
                    .loadUserByEmailAndPassword("user@test.ch", "hashedPassword");

            assertNotNull(userDetails);
            assertEquals("user@test.ch", userDetails.getUsername());
            assertEquals(1, userDetails.getAvailableKontos().size());
            assertNotNull(userDetails.getActiveKonto());
        }

        @Test
        @DisplayName("Should throw exception for invalid credentials")
        void testLoadUserByEmailAndPasswordInvalid() {
            when(benutzerRepository.findByEmailAndPassword("user@test.ch", "wrongPassword"))
                    .thenReturn(Optional.empty());

            assertThrows(UsernameNotFoundException.class, () ->
                    userDetailsService.loadUserByEmailAndPassword("user@test.ch", "wrongPassword")
            );
        }

        @Test
        @DisplayName("Should throw exception when user not allowed to login")
        void testLoadUserByEmailAndPasswordNotAllowedToLogin() {
            testUser.setLoginopt(false);

            when(benutzerRepository.findByEmailAndPassword("user@test.ch", "hashedPassword"))
                    .thenReturn(Optional.of(testUser));

            assertThrows(UsernameNotFoundException.class, () ->
                    userDetailsService.loadUserByEmailAndPassword("user@test.ch", "hashedPassword")
            );
        }

        @Test
        @DisplayName("Should not auto-select konto when multiple available")
        void testLoadUserByEmailAndPasswordMultipleKontos() {
            Konto konto2 = new Konto();
            konto2.setId(2L);

            when(benutzerRepository.findByEmailAndPassword("user@test.ch", "hashedPassword"))
                    .thenReturn(Optional.of(testUser));
            when(userService.getAllowedKontos(testUser))
                    .thenReturn(Arrays.asList(testKonto, konto2));

            DoctorDocUserDetails userDetails = userDetailsService
                    .loadUserByEmailAndPassword("user@test.ch", "hashedPassword");

            assertEquals(2, userDetails.getAvailableKontos().size());
            assertNull(userDetails.getActiveKonto());
        }
    }
}
