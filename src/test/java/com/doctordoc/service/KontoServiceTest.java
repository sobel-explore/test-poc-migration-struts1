//  Copyright (C) 2005 - 2010  Markus Fischer, Pascal Steiner
//
//  This program is free software; you can redistribute it and/or
//  modify it under the terms of the GNU General Public License
//  as published by the Free Software Foundation; version 2 of the License.

package com.doctordoc.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.doctordoc.entity.Konto;
import com.doctordoc.repository.KontoRepository;

/**
 * Unit tests for KontoService.
 */
@ExtendWith(MockitoExtension.class)
class KontoServiceTest {

    @Mock
    private KontoRepository kontoRepository;

    @InjectMocks
    private KontoService kontoService;

    private Konto testKonto;

    @BeforeEach
    void setUp() {
        testKonto = new Konto();
        testKonto.setId(1L);
        testKonto.setBibliotheksname("Test Bibliothek");
        testKonto.setKontostatus(true);
        testKonto.setKontotyp(0);
    }

    @Nested
    @DisplayName("Find Operations")
    class FindOperations {

        @Test
        @DisplayName("Should find konto by ID")
        void testFindById() {
            when(kontoRepository.findById(1L)).thenReturn(Optional.of(testKonto));

            Optional<Konto> result = kontoService.findById(1L);

            assertTrue(result.isPresent());
            assertEquals("Test Bibliothek", result.get().getBibliotheksname());
            verify(kontoRepository).findById(1L);
        }

        @Test
        @DisplayName("Should return empty when konto not found")
        void testFindByIdNotFound() {
            when(kontoRepository.findById(999L)).thenReturn(Optional.empty());

            Optional<Konto> result = kontoService.findById(999L);

            assertFalse(result.isPresent());
        }

        @Test
        @DisplayName("Should find all kontos ordered")
        void testFindAll() {
            Konto konto2 = new Konto();
            konto2.setId(2L);
            konto2.setBibliotheksname("Another Bibliothek");

            when(kontoRepository.findAllByOrderByKontotypDescBibliotheksnameAsc())
                    .thenReturn(Arrays.asList(testKonto, konto2));

            List<Konto> result = kontoService.findAll();

            assertEquals(2, result.size());
            verify(kontoRepository).findAllByOrderByKontotypDescBibliotheksnameAsc();
        }

        @Test
        @DisplayName("Should find advanced kontos")
        void testFindAdvancedKontos() {
            testKonto.setKontotyp(1);
            when(kontoRepository.findByKontotypGreaterThanOrderByKontotypDescBibliotheksnameAsc(0))
                    .thenReturn(Arrays.asList(testKonto));

            List<Konto> result = kontoService.findAdvancedKontos();

            assertEquals(1, result.size());
            verify(kontoRepository).findByKontotypGreaterThanOrderByKontotypDescBibliotheksnameAsc(0);
        }

        @Test
        @DisplayName("Should find faxserver kontos")
        void testFindFaxserverKontos() {
            testKonto.setFaxno("123456");
            when(kontoRepository.findByFaxnoIsNotNull()).thenReturn(Arrays.asList(testKonto));

            List<Konto> result = kontoService.findFaxserverKontos();

            assertEquals(1, result.size());
            verify(kontoRepository).findByFaxnoIsNotNull();
        }
    }

    @Nested
    @DisplayName("Save and Delete Operations")
    class SaveDeleteOperations {

        @Test
        @DisplayName("Should save konto with edatum set")
        void testSaveNewKonto() {
            Konto newKonto = new Konto();
            newKonto.setBibliotheksname("New Bibliothek");

            when(kontoRepository.save(any(Konto.class))).thenAnswer(invocation -> {
                Konto saved = invocation.getArgument(0);
                saved.setId(1L);
                return saved;
            });

            Konto result = kontoService.save(newKonto);

            assertNotNull(result.getEdatum());
            verify(kontoRepository).save(newKonto);
        }

        @Test
        @DisplayName("Should not override existing edatum")
        void testSaveExistingKonto() {
            LocalDate existingDate = LocalDate.of(2020, 1, 1);
            testKonto.setEdatum(existingDate);

            when(kontoRepository.save(testKonto)).thenReturn(testKonto);

            Konto result = kontoService.save(testKonto);

            assertEquals(existingDate, result.getEdatum());
        }

        @Test
        @DisplayName("Should delete konto by ID")
        void testDelete() {
            kontoService.delete(1L);

            verify(kontoRepository).deleteById(1L);
        }
    }

    @Nested
    @DisplayName("Business Logic Methods")
    class BusinessLogicMethods {

        @Test
        @DisplayName("Should return true for paid account (kontotyp > 0)")
        void testIsPaidAccountWithKontotyp() {
            testKonto.setKontotyp(1);

            assertTrue(kontoService.isPaidAccount(testKonto));
        }

        @Test
        @DisplayName("Should return true for trial account within 30 days")
        void testIsPaidAccountWithinTrial() {
            testKonto.setKontotyp(0);
            testKonto.setEdatum(LocalDate.now().minusDays(15));

            assertTrue(kontoService.isPaidAccount(testKonto));
        }

        @Test
        @DisplayName("Should return false for expired trial account")
        void testIsPaidAccountExpiredTrial() {
            testKonto.setKontotyp(0);
            testKonto.setEdatum(LocalDate.now().minusDays(31));

            assertFalse(kontoService.isPaidAccount(testKonto));
        }

        @Test
        @DisplayName("Should return false when no edatum set")
        void testIsPaidAccountNoEdatum() {
            testKonto.setKontotyp(0);
            testKonto.setEdatum(null);

            assertFalse(kontoService.isPaidAccount(testKonto));
        }

        @Test
        @DisplayName("Should return true when has fax server")
        void testHasFaxServerTrue() {
            testKonto.setFaxno("123456");

            assertTrue(kontoService.hasFaxServer(testKonto));
        }

        @Test
        @DisplayName("Should return false when no fax server")
        void testHasFaxServerFalse() {
            testKonto.setFaxno(null);
            assertFalse(kontoService.hasFaxServer(testKonto));

            testKonto.setFaxno("");
            assertFalse(kontoService.hasFaxServer(testKonto));
        }

        @Test
        @DisplayName("Should return correct kontotyp descriptions")
        void testGetKontotypDescription() {
            testKonto.setKontotyp(0);
            assertEquals("Free", kontoService.getKontotypDescription(testKonto));

            testKonto.setKontotyp(1);
            assertEquals("Premium", kontoService.getKontotypDescription(testKonto));

            testKonto.setKontotyp(2);
            assertEquals("Fax 1 Year", kontoService.getKontotypDescription(testKonto));

            testKonto.setKontotyp(3);
            assertEquals("Fax 3 Months", kontoService.getKontotypDescription(testKonto));

            testKonto.setKontotyp(99);
            assertEquals("Unknown", kontoService.getKontotypDescription(testKonto));
        }
    }
}
