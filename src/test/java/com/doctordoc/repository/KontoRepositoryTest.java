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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import com.doctordoc.entity.Konto;

/**
 * Tests for KontoRepository.
 * Migrated from TestKonto (JUnit 4) to Spring Boot JPA tests.
 * Uses H2 in-memory database for testing.
 */
@DataJpaTest
@ActiveProfiles("test")
class KontoRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private KontoRepository kontoRepository;

    private static final String TEST_BIBLIONAME = "Test Bibliothek";
    private static final String TEST_PLZ = "12345";
    private static final String TEST_ORT = "Testort";

    private Konto createTestKonto() {
        Konto konto = new Konto();
        konto.setBibliotheksname(TEST_BIBLIONAME);
        konto.setAdresse("Testadresse 1");
        konto.setPlz(TEST_PLZ);
        konto.setOrt(TEST_ORT);
        konto.setLand("CH");
        konto.setTelefon("0123456789");
        konto.setBibliotheksmail("biblio@test.ch");
        konto.setDbsmail("dbs@test.ch");
        konto.setKontostatus(true);
        konto.setUserlogin(false);
        return konto;
    }

    @Nested
    @DisplayName("Save Konto Tests")
    class SaveKontoTests {

        @Test
        @DisplayName("Should save a new konto successfully")
        void testSaveKonto() {
            Konto konto = createTestKonto();

            Konto saved = kontoRepository.save(konto);

            assertNotNull(saved.getId(), "Konto ID should be generated after save");
            assertEquals(TEST_BIBLIONAME, saved.getBibliotheksname());
            assertEquals(TEST_PLZ, saved.getPlz());
            assertEquals(TEST_ORT, saved.getOrt());
        }
    }

    @Nested
    @DisplayName("Load Konto Tests")
    class LoadKontoTests {

        @Test
        @DisplayName("Should load konto by ID")
        void testLoadKonto() {
            Konto konto = createTestKonto();
            entityManager.persistAndFlush(konto);

            Optional<Konto> found = kontoRepository.findById(konto.getId());

            assertTrue(found.isPresent(), "Konto should be found by ID");
            Konto loadedKonto = found.get();
            assertEquals(TEST_BIBLIONAME, loadedKonto.getBibliotheksname());
            assertEquals(TEST_PLZ, loadedKonto.getPlz());
            assertEquals(TEST_ORT, loadedKonto.getOrt());
            assertEquals("CH", loadedKonto.getLand());
        }
    }

    @Nested
    @DisplayName("Update Konto Tests")
    class UpdateKontoTests {

        @Test
        @DisplayName("Should update konto PLZ")
        void testUpdateKontoPlz() {
            Konto konto = createTestKonto();
            entityManager.persistAndFlush(konto);

            // Store original PLZ
            String originalPlz = konto.getPlz();

            // Update PLZ
            konto.setPlz("4603");
            kontoRepository.save(konto);
            entityManager.flush();
            entityManager.clear();

            // Verify change
            Optional<Konto> updated = kontoRepository.findById(konto.getId());
            assertTrue(updated.isPresent());
            assertEquals("4603", updated.get().getPlz(), "PLZ should be updated to 4603");

            // Restore original (as in the original test)
            Konto toRestore = updated.get();
            toRestore.setPlz(originalPlz);
            kontoRepository.save(toRestore);
            entityManager.flush();
            entityManager.clear();

            // Verify restoration
            Optional<Konto> restored = kontoRepository.findById(konto.getId());
            assertTrue(restored.isPresent());
            assertEquals(originalPlz, restored.get().getPlz());
        }

        @Test
        @DisplayName("Should update konto bibliotheksname")
        void testUpdateKontoBibliotheksname() {
            Konto konto = createTestKonto();
            entityManager.persistAndFlush(konto);

            konto.setBibliotheksname("Updated Bibliothek Name");
            kontoRepository.save(konto);
            entityManager.flush();
            entityManager.clear();

            Optional<Konto> updated = kontoRepository.findById(konto.getId());
            assertTrue(updated.isPresent());
            assertEquals("Updated Bibliothek Name", updated.get().getBibliotheksname());
        }
    }

    @Nested
    @DisplayName("Delete Konto Tests")
    class DeleteKontoTests {

        @Test
        @DisplayName("Should delete konto successfully")
        void testDeleteKonto() {
            Konto konto = createTestKonto();
            entityManager.persistAndFlush(konto);
            Long kontoId = konto.getId();

            kontoRepository.delete(konto);
            entityManager.flush();

            Optional<Konto> deleted = kontoRepository.findById(kontoId);
            assertTrue(deleted.isEmpty(), "Konto should be deleted");
        }
    }

    @Nested
    @DisplayName("Query Tests")
    class QueryTests {

        @Test
        @DisplayName("Should find kontos by status")
        void testFindByKontostatus() {
            Konto activeKonto = createTestKonto();
            activeKonto.setKontostatus(true);
            entityManager.persistAndFlush(activeKonto);

            Konto inactiveKonto = createTestKonto();
            inactiveKonto.setBibliotheksname("Inactive Bibliothek");
            inactiveKonto.setKontostatus(false);
            entityManager.persistAndFlush(inactiveKonto);

            var activeKontos = kontoRepository.findByKontostatus(true);
            assertEquals(1, activeKontos.size());
            assertEquals(TEST_BIBLIONAME, activeKontos.get(0).getBibliotheksname());

            var inactiveKontos = kontoRepository.findByKontostatus(false);
            assertEquals(1, inactiveKontos.size());
            assertEquals("Inactive Bibliothek", inactiveKontos.get(0).getBibliotheksname());
        }

        @Test
        @DisplayName("Should find all kontos ordered by kontotyp and name")
        void testFindAllOrdered() {
            Konto konto1 = createTestKonto();
            konto1.setBibliotheksname("Alpha Bibliothek");
            konto1.setKontotyp(1);
            entityManager.persistAndFlush(konto1);

            Konto konto2 = createTestKonto();
            konto2.setBibliotheksname("Beta Bibliothek");
            konto2.setKontotyp(2);
            entityManager.persistAndFlush(konto2);

            var allKontos = kontoRepository.findAllByOrderByKontotypDescBibliotheksnameAsc();
            assertEquals(2, allKontos.size());
            // Higher kontotyp should come first
            assertEquals("Beta Bibliothek", allKontos.get(0).getBibliotheksname());
            assertEquals("Alpha Bibliothek", allKontos.get(1).getBibliotheksname());
        }
    }
}
