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

package com.doctordoc.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import com.doctordoc.entity.Benutzer;

/**
 * Tests for BenutzerRepository.
 * Migrated from TestBenutzer (JUnit 4) to Spring Boot JPA tests.
 * Uses H2 in-memory database for testing.
 */
@DataJpaTest
@ActiveProfiles("test")
class BenutzerRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private BenutzerRepository benutzerRepository;

    private static final String TEST_EMAIL = "test@test.ch";
    private static final String TEST_PASSWORD = "testpassword";
    private static final String TEST_VORNAME = "Testvorname";
    private static final String TEST_NAME = "Testname";
    private static final String TEST_ANREDE = "Testanrede";

    private Benutzer createTestBenutzer() {
        Benutzer benutzer = new Benutzer();
        benutzer.setEmail(TEST_EMAIL);
        benutzer.setPassword(TEST_PASSWORD);
        benutzer.setVorname(TEST_VORNAME);
        benutzer.setName(TEST_NAME);
        benutzer.setAnrede(TEST_ANREDE);
        benutzer.setInstitut("Testinstitut");
        benutzer.setAbteilung("Testabteilung");
        benutzer.setAdresse("Testadresse");
        benutzer.setPlz("12345");
        benutzer.setOrt("Testort");
        benutzer.setLand("CH");
        benutzer.setRechte(Benutzer.ROLE_USER);
        benutzer.setKontostatus(true);
        benutzer.setDatum(LocalDateTime.now());
        return benutzer;
    }

    @Nested
    @DisplayName("Save User Tests")
    class SaveUserTests {

        @Test
        @DisplayName("Should save a new user successfully")
        void testSaveBenutzer() {
            Benutzer benutzer = createTestBenutzer();

            Benutzer saved = benutzerRepository.save(benutzer);

            assertNotNull(saved.getId(), "User ID should be generated after save");
            assertEquals(TEST_EMAIL, saved.getEmail());
            assertEquals(TEST_VORNAME, saved.getVorname());
            assertEquals(TEST_NAME, saved.getName());
        }

        @Test
        @DisplayName("Should save two users with same email")
        void testSave2BenutzerWithSameEmail() {
            Benutzer benutzer1 = createTestBenutzer();
            Benutzer benutzer2 = createTestBenutzer();

            benutzerRepository.save(benutzer1);
            benutzerRepository.save(benutzer2);

            List<Benutzer> users = benutzerRepository.findAllByEmail(TEST_EMAIL);
            assertEquals(2, users.size(), "Should have exactly 2 test users in the DB");
        }
    }

    @Nested
    @DisplayName("Load User Tests")
    class LoadUserTests {

        @Test
        @DisplayName("Should load user by email")
        void testLoadBenutzer() {
            Benutzer benutzer = createTestBenutzer();
            entityManager.persistAndFlush(benutzer);

            Optional<Benutzer> found = benutzerRepository.findByEmail(TEST_EMAIL);

            assertTrue(found.isPresent(), "User should be found by email");
            Benutzer loadedUser = found.get();
            assertEquals(TEST_ANREDE, loadedUser.getAnrede());
            assertEquals(TEST_VORNAME, loadedUser.getVorname());
            assertEquals(TEST_NAME, loadedUser.getName());
            assertEquals(TEST_EMAIL, loadedUser.getEmail());
            assertEquals(TEST_PASSWORD, loadedUser.getPassword());
            assertEquals("Testinstitut", loadedUser.getInstitut());
            assertEquals("Testabteilung", loadedUser.getAbteilung());
            assertEquals("Testadresse", loadedUser.getAdresse());
            assertEquals("12345", loadedUser.getPlz());
            assertEquals("Testort", loadedUser.getOrt());
            assertEquals("CH", loadedUser.getLand());
            assertEquals(Benutzer.ROLE_USER, loadedUser.getRechte());
        }

        @Test
        @DisplayName("Should load user by email and password")
        void testLoadBenutzerByEmailAndPassword() {
            Benutzer benutzer = createTestBenutzer();
            entityManager.persistAndFlush(benutzer);

            Optional<Benutzer> found = benutzerRepository.findByEmailAndPassword(TEST_EMAIL, TEST_PASSWORD);

            assertTrue(found.isPresent(), "User should be found by email and password");
            assertEquals(TEST_EMAIL, found.get().getEmail());
        }
    }

    @Nested
    @DisplayName("Update User Tests")
    class UpdateUserTests {

        @Test
        @DisplayName("Should update user name and vorname")
        void testChangeBenutzer() {
            Benutzer benutzer = createTestBenutzer();
            entityManager.persistAndFlush(benutzer);

            // Load and modify
            Optional<Benutzer> found = benutzerRepository.findByEmail(TEST_EMAIL);
            assertTrue(found.isPresent());

            Benutzer toUpdate = found.get();
            toUpdate.setName(TEST_NAME + "1");
            toUpdate.setVorname(TEST_VORNAME + "1");
            benutzerRepository.save(toUpdate);
            entityManager.flush();
            entityManager.clear();

            // Verify changes
            Optional<Benutzer> updated = benutzerRepository.findByEmail(TEST_EMAIL);
            assertTrue(updated.isPresent());
            assertEquals(TEST_VORNAME + "1", updated.get().getVorname());
            assertEquals(TEST_NAME + "1", updated.get().getName());
        }
    }

    @Nested
    @DisplayName("Delete User Tests")
    class DeleteUserTests {

        @Test
        @DisplayName("Should delete user successfully")
        void testDeleteBenutzer() {
            Benutzer benutzer = createTestBenutzer();
            entityManager.persistAndFlush(benutzer);

            Optional<Benutzer> found = benutzerRepository.findByEmail(TEST_EMAIL);
            assertTrue(found.isPresent());

            benutzerRepository.delete(found.get());
            entityManager.flush();

            Optional<Benutzer> deleted = benutzerRepository.findByEmail(TEST_EMAIL);
            assertFalse(deleted.isPresent(), "User should be deleted");
        }
    }

    @Nested
    @DisplayName("Role Tests")
    class RoleTests {

        @Test
        @DisplayName("Should correctly identify user roles")
        void testUserRoles() {
            Benutzer user = createTestBenutzer();
            user.setRechte(Benutzer.ROLE_USER);
            assertTrue(user.isUser());
            assertFalse(user.isLibrarian());
            assertFalse(user.isAdmin());
            assertEquals("USER", user.getRoleName());

            user.setRechte(Benutzer.ROLE_LIBRARIAN);
            assertFalse(user.isUser());
            assertTrue(user.isLibrarian());
            assertFalse(user.isAdmin());
            assertEquals("LIBRARIAN", user.getRoleName());

            user.setRechte(Benutzer.ROLE_ADMIN);
            assertFalse(user.isUser());
            assertFalse(user.isLibrarian());
            assertTrue(user.isAdmin());
            assertEquals("ADMIN", user.getRoleName());
        }
    }

    @Nested
    @DisplayName("Full Name Tests")
    class FullNameTests {

        @Test
        @DisplayName("Should return correct full name")
        void testGetFullName() {
            Benutzer benutzer = new Benutzer();
            benutzer.setVorname("Max");
            benutzer.setName("Mustermann");

            assertEquals("Max Mustermann", benutzer.getFullName());

            benutzer.setVorname(null);
            assertEquals("Mustermann", benutzer.getFullName());

            benutzer.setVorname("Max");
            benutzer.setName(null);
            assertEquals("Max", benutzer.getFullName());

            benutzer.setVorname(null);
            benutzer.setName(null);
            assertEquals("", benutzer.getFullName());
        }
    }
}
