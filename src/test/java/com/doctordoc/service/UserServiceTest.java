//  Copyright (C) 2005 - 2010  Markus Fischer, Pascal Steiner
//
//  This program is free software; you can redistribute it and/or
//  modify it under the terms of the GNU General Public License
//  as published by the Free Software Foundation; version 2 of the License.

package com.doctordoc.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.doctordoc.entity.Benutzer;
import com.doctordoc.entity.Konto;
import com.doctordoc.repository.BenutzerRepository;
import com.doctordoc.repository.KontoRepository;

/**
 * Unit tests for UserService.
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private BenutzerRepository benutzerRepository;

    @Mock
    private KontoRepository kontoRepository;

    @InjectMocks
    private UserService userService;

    private Benutzer testUser;
    private Benutzer testLibrarian;
    private Benutzer testAdmin;
    private Konto testKonto;

    @BeforeEach
    void setUp() {
        // Setup regular user
        testUser = new Benutzer();
        testUser.setId(1L);
        testUser.setEmail("user@test.ch");
        testUser.setRechte(Benutzer.ROLE_USER);
        testUser.setKontostatus(true);
        testUser.setLoginopt(true);

        // Setup librarian
        testLibrarian = new Benutzer();
        testLibrarian.setId(2L);
        testLibrarian.setEmail("librarian@test.ch");
        testLibrarian.setRechte(Benutzer.ROLE_LIBRARIAN);
        testLibrarian.setKontostatus(true);

        // Setup admin
        testAdmin = new Benutzer();
        testAdmin.setId(3L);
        testAdmin.setEmail("admin@test.ch");
        testAdmin.setRechte(Benutzer.ROLE_ADMIN);
        testAdmin.setKontostatus(true);

        // Setup test konto
        testKonto = new Konto();
        testKonto.setId(1L);
        testKonto.setBibliotheksname("Test Bibliothek");
        testKonto.setKontostatus(true);
        testKonto.setUserlogin(true);
        testKonto.setUserbestellung(true);
        testKonto.setGbvbestellung(true);
        testKonto.setGbvrequesterid("GBV123");
        testKonto.setIsil("DE-123");
    }

    @Nested
    @DisplayName("Find Operations")
    class FindOperations {

        @Test
        @DisplayName("Should find user by ID")
        void testFindById() {
            when(benutzerRepository.findById(1L)).thenReturn(Optional.of(testUser));

            Optional<Benutzer> result = userService.findById(1L);

            assertTrue(result.isPresent());
            assertEquals("user@test.ch", result.get().getEmail());
        }

        @Test
        @DisplayName("Should find user by email")
        void testFindByEmail() {
            when(benutzerRepository.findByEmail("user@test.ch")).thenReturn(Optional.of(testUser));

            Optional<Benutzer> result = userService.findByEmail("user@test.ch");

            assertTrue(result.isPresent());
            assertEquals(1L, result.get().getId());
        }

        @Test
        @DisplayName("Should find users by konto ID")
        void testFindByKontoId() {
            when(benutzerRepository.findByKontoId(1L)).thenReturn(Arrays.asList(testUser, testLibrarian));

            List<Benutzer> result = userService.findByKontoId(1L);

            assertEquals(2, result.size());
        }

        @Test
        @DisplayName("Should find all users")
        void testFindAll() {
            when(benutzerRepository.findAll()).thenReturn(Arrays.asList(testUser, testLibrarian, testAdmin));

            List<Benutzer> result = userService.findAll();

            assertEquals(3, result.size());
        }
    }

    @Nested
    @DisplayName("Save and Delete Operations")
    class SaveDeleteOperations {

        @Test
        @DisplayName("Should save user with datum set")
        void testSaveNewUser() {
            Benutzer newUser = new Benutzer();
            newUser.setEmail("new@test.ch");

            when(benutzerRepository.save(any(Benutzer.class))).thenAnswer(invocation -> {
                Benutzer saved = invocation.getArgument(0);
                saved.setId(1L);
                return saved;
            });

            Benutzer result = userService.save(newUser);

            assertNotNull(result.getDatum());
            verify(benutzerRepository).save(newUser);
        }

        @Test
        @DisplayName("Should not override existing datum")
        void testSaveExistingUser() {
            LocalDateTime existingDate = LocalDateTime.of(2020, 1, 1, 12, 0);
            testUser.setDatum(existingDate);

            when(benutzerRepository.save(testUser)).thenReturn(testUser);

            Benutzer result = userService.save(testUser);

            assertEquals(existingDate, result.getDatum());
        }

        @Test
        @DisplayName("Should delete user by ID")
        void testDelete() {
            userService.delete(1L);

            verify(benutzerRepository).deleteById(1L);
        }

        @Test
        @DisplayName("Should update last use timestamp")
        void testUpdateLastUse() {
            when(benutzerRepository.save(testUser)).thenReturn(testUser);

            userService.updateLastUse(testUser);

            assertNotNull(testUser.getLastuse());
            verify(benutzerRepository).save(testUser);
        }
    }

    @Nested
    @DisplayName("Get Allowed Kontos")
    class GetAllowedKontos {

        @Test
        @DisplayName("Admin should get all kontos")
        void testGetAllowedKontosForAdmin() {
            Konto konto2 = new Konto();
            konto2.setId(2L);
            konto2.setBibliotheksname("Another Bibliothek");

            when(kontoRepository.findAllByOrderByKontotypDescBibliotheksnameAsc())
                    .thenReturn(Arrays.asList(testKonto, konto2));

            List<Konto> result = userService.getAllowedKontos(testAdmin);

            assertEquals(2, result.size());
        }

        @Test
        @DisplayName("Librarian should get active kontos from their set")
        void testGetAllowedKontosForLibrarian() {
            Set<Konto> kontos = new HashSet<>();
            kontos.add(testKonto);
            testLibrarian.setKontos(kontos);

            List<Konto> result = userService.getAllowedKontos(testLibrarian);

            assertEquals(1, result.size());
            assertEquals("Test Bibliothek", result.get(0).getBibliotheksname());
        }

        @Test
        @DisplayName("User should only get kontos with userlogin enabled")
        void testGetAllowedKontosForUser() {
            Set<Konto> kontos = new HashSet<>();
            kontos.add(testKonto);
            testUser.setKontos(kontos);

            List<Konto> result = userService.getAllowedKontos(testUser);

            assertEquals(1, result.size());
        }

        @Test
        @DisplayName("Should filter out inactive kontos")
        void testGetAllowedKontosFiltersInactive() {
            testKonto.setKontostatus(false);
            Set<Konto> kontos = new HashSet<>();
            kontos.add(testKonto);
            testLibrarian.setKontos(kontos);

            List<Konto> result = userService.getAllowedKontos(testLibrarian);

            assertEquals(0, result.size());
        }
    }

    @Nested
    @DisplayName("Order Permission Checks")
    class OrderPermissionChecks {

        @Test
        @DisplayName("Admin can always order Subito")
        void testCanOrderSubitoAdmin() {
            assertTrue(userService.canOrderSubito(testAdmin, testKonto));
        }

        @Test
        @DisplayName("Librarian can always order Subito")
        void testCanOrderSubitoLibrarian() {
            assertTrue(userService.canOrderSubito(testLibrarian, testKonto));
        }

        @Test
        @DisplayName("User can order Subito when both flags are set")
        void testCanOrderSubitoUser() {
            testUser.setUserbestellung(true);
            testKonto.setUserbestellung(true);

            assertTrue(userService.canOrderSubito(testUser, testKonto));
        }

        @Test
        @DisplayName("User cannot order Subito when user flag is false")
        void testCanOrderSubitoUserFlagFalse() {
            testUser.setUserbestellung(false);
            testKonto.setUserbestellung(true);

            assertFalse(userService.canOrderSubito(testUser, testKonto));
        }

        @Test
        @DisplayName("Admin can always order GBV")
        void testCanOrderGBVAdmin() {
            assertTrue(userService.canOrderGBV(testAdmin, testKonto));
        }

        @Test
        @DisplayName("User can order GBV when all conditions met")
        void testCanOrderGBVUser() {
            testUser.setGbvbestellung(true);

            assertTrue(userService.canOrderGBV(testUser, testKonto));
        }

        @Test
        @DisplayName("User cannot order GBV when requester ID missing")
        void testCanOrderGBVMissingRequesterId() {
            testUser.setGbvbestellung(true);
            testKonto.setGbvrequesterid(null);

            assertFalse(userService.canOrderGBV(testUser, testKonto));
        }

        @Test
        @DisplayName("User cannot order GBV when ISIL missing")
        void testCanOrderGBVMissingIsil() {
            testUser.setGbvbestellung(true);
            testKonto.setIsil(null);

            assertFalse(userService.canOrderGBV(testUser, testKonto));
        }
    }

    @Nested
    @DisplayName("Account Status Checks")
    class AccountStatusChecks {

        @Test
        @DisplayName("Admin account is always active")
        void testIsUserAccountActiveAdmin() {
            assertTrue(userService.isUserAccountActive(testAdmin, testKonto));
        }

        @Test
        @DisplayName("Librarian account is always active")
        void testIsUserAccountActiveLibrarian() {
            assertTrue(userService.isUserAccountActive(testLibrarian, testKonto));
        }

        @Test
        @DisplayName("User account active when both statuses are true")
        void testIsUserAccountActiveUser() {
            testUser.setKontostatus(true);
            testKonto.setKontostatus(true);

            assertTrue(userService.isUserAccountActive(testUser, testKonto));
        }

        @Test
        @DisplayName("User account inactive when user status is false")
        void testIsUserAccountInactiveUser() {
            testUser.setKontostatus(false);
            testKonto.setKontostatus(true);

            assertFalse(userService.isUserAccountActive(testUser, testKonto));
        }

        @Test
        @DisplayName("User account inactive when konto status is false")
        void testIsUserAccountInactiveKonto() {
            testUser.setKontostatus(true);
            testKonto.setKontostatus(false);

            assertFalse(userService.isUserAccountActive(testUser, testKonto));
        }
    }

    @Nested
    @DisplayName("Category Reset")
    class CategoryReset {

        @Test
        @DisplayName("Should reset categories for matching users")
        void testResetCategories() {
            testUser.setCategoryId(5L);
            Benutzer user2 = new Benutzer();
            user2.setId(10L);
            user2.setCategoryId(5L);
            Benutzer user3 = new Benutzer();
            user3.setId(11L);
            user3.setCategoryId(3L);

            when(benutzerRepository.findAll()).thenReturn(Arrays.asList(testUser, user2, user3));

            userService.resetCategories(5L);

            assertEquals(0L, testUser.getCategoryId());
            assertEquals(0L, user2.getCategoryId());
            assertEquals(3L, user3.getCategoryId());
            verify(benutzerRepository, times(2)).save(any(Benutzer.class));
        }
    }
}
