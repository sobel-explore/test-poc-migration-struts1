//  Copyright (C) 2005 - 2010  Markus Fischer, Pascal Steiner
//
//  This program is free software; you can redistribute it and/or
//  modify it under the terms of the GNU General Public License
//  as published by the Free Software Foundation; version 2 of the License.

package com.doctordoc.dto;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Set;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

/**
 * Validation tests for KontoDto.
 */
class KontoDtoValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    private KontoDto createValidKontoDto() {
        KontoDto dto = new KontoDto();
        dto.setBibliotheksname("Test Bibliothek");
        dto.setAdresse("Teststrasse 1");
        dto.setPlz("12345");
        dto.setOrt("Teststadt");
        dto.setLand("CH");
        dto.setBibliotheksmail("test@bibliothek.ch");
        return dto;
    }

    @Nested
    @DisplayName("Bibliotheksname Validation")
    class BibliotheksnameValidation {

        @Test
        @DisplayName("Should be valid with proper name")
        void testValidBibliotheksname() {
            KontoDto dto = createValidKontoDto();

            Set<ConstraintViolation<KontoDto>> violations = validator.validate(dto);

            assertTrue(violations.isEmpty());
        }

        @Test
        @DisplayName("Should fail when bibliotheksname is blank")
        void testBlankBibliotheksname() {
            KontoDto dto = createValidKontoDto();
            dto.setBibliotheksname("");

            Set<ConstraintViolation<KontoDto>> violations = validator.validate(dto);

            assertFalse(violations.isEmpty());
            assertTrue(violations.stream()
                    .anyMatch(v -> v.getPropertyPath().toString().equals("bibliotheksname")));
        }

        @Test
        @DisplayName("Should fail when bibliotheksname is null")
        void testNullBibliotheksname() {
            KontoDto dto = createValidKontoDto();
            dto.setBibliotheksname(null);

            Set<ConstraintViolation<KontoDto>> violations = validator.validate(dto);

            assertFalse(violations.isEmpty());
        }

        @Test
        @DisplayName("Should fail when bibliotheksname exceeds max length")
        void testBibliotheksnameTooLong() {
            KontoDto dto = createValidKontoDto();
            dto.setBibliotheksname("A".repeat(256));

            Set<ConstraintViolation<KontoDto>> violations = validator.validate(dto);

            assertFalse(violations.isEmpty());
        }
    }

    @Nested
    @DisplayName("Email Validation")
    class EmailValidation {

        @Test
        @DisplayName("Should be valid with proper email")
        void testValidEmail() {
            KontoDto dto = createValidKontoDto();
            dto.setBibliotheksmail("test@example.com");

            Set<ConstraintViolation<KontoDto>> violations = validator.validate(dto);

            assertTrue(violations.isEmpty());
        }

        @Test
        @DisplayName("Should fail with invalid email format")
        void testInvalidEmail() {
            KontoDto dto = createValidKontoDto();
            dto.setBibliotheksmail("invalid-email");

            Set<ConstraintViolation<KontoDto>> violations = validator.validate(dto);

            assertFalse(violations.isEmpty());
            assertTrue(violations.stream()
                    .anyMatch(v -> v.getPropertyPath().toString().equals("bibliotheksmail")));
        }

        @Test
        @DisplayName("Should fail with invalid DBS email format")
        void testInvalidDbsEmail() {
            KontoDto dto = createValidKontoDto();
            dto.setDbsmail("not-an-email");

            Set<ConstraintViolation<KontoDto>> violations = validator.validate(dto);

            assertFalse(violations.isEmpty());
            assertTrue(violations.stream()
                    .anyMatch(v -> v.getPropertyPath().toString().equals("dbsmail")));
        }
    }

    @Nested
    @DisplayName("Size Validation")
    class SizeValidation {

        @Test
        @DisplayName("Should fail when ISIL exceeds max length")
        void testIsilTooLong() {
            KontoDto dto = createValidKontoDto();
            dto.setIsil("X".repeat(51));

            Set<ConstraintViolation<KontoDto>> violations = validator.validate(dto);

            assertFalse(violations.isEmpty());
        }

        @Test
        @DisplayName("Should fail when PLZ exceeds max length")
        void testPlzTooLong() {
            KontoDto dto = createValidKontoDto();
            dto.setPlz("1".repeat(21));

            Set<ConstraintViolation<KontoDto>> violations = validator.validate(dto);

            assertFalse(violations.isEmpty());
        }

        @Test
        @DisplayName("Should fail when Ort exceeds max length")
        void testOrtTooLong() {
            KontoDto dto = createValidKontoDto();
            dto.setOrt("X".repeat(101));

            Set<ConstraintViolation<KontoDto>> violations = validator.validate(dto);

            assertFalse(violations.isEmpty());
        }

        @Test
        @DisplayName("Should fail when Faxno exceeds max length")
        void testFaxnoTooLong() {
            KontoDto dto = createValidKontoDto();
            dto.setFaxno("1".repeat(51));

            Set<ConstraintViolation<KontoDto>> violations = validator.validate(dto);

            assertFalse(violations.isEmpty());
        }
    }

    @Nested
    @DisplayName("Default Values")
    class DefaultValues {

        @Test
        @DisplayName("Should have default timezone")
        void testDefaultTimezone() {
            KontoDto dto = new KontoDto();

            assertEquals("Europe/Berlin", dto.getTimezone());
        }

        @Test
        @DisplayName("Should have default kontostatus")
        void testDefaultKontostatus() {
            KontoDto dto = new KontoDto();

            assertTrue(dto.isKontostatus());
        }

        @Test
        @DisplayName("Should have default delivery options")
        void testDefaultDeloptions() {
            KontoDto dto = new KontoDto();

            assertEquals("post", dto.getDefaultDeloptions());
        }

        @Test
        @DisplayName("Should have default showpubsuppliers")
        void testDefaultShowpubsuppliers() {
            KontoDto dto = new KontoDto();

            assertTrue(dto.isShowpubsuppliers());
        }
    }
}
