//  Copyright (C) 2005 - 2010  Markus Fischer, Pascal Steiner
//
//  This program is free software; you can redistribute it and/or
//  modify it under the terms of the GNU General Public License
//  as published by the Free Software Foundation; version 2 of the License.

package com.doctordoc.dto;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
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
 * Validation tests for OrderDto.
 */
class OrderDtoValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    private OrderDto createValidOrderDto() {
        OrderDto dto = new OrderDto();
        dto.setArtikeltitel("Test Article");
        dto.setZeitschriftentitel("Test Journal");
        dto.setAuthor("Test Author");
        return dto;
    }

    @Nested
    @DisplayName("Size Validation")
    class SizeValidation {

        @Test
        @DisplayName("Should be valid with proper values")
        void testValidOrder() {
            OrderDto dto = createValidOrderDto();

            Set<ConstraintViolation<OrderDto>> violations = validator.validate(dto);

            assertTrue(violations.isEmpty());
        }

        @Test
        @DisplayName("Should fail when artikeltitel exceeds max length")
        void testArtikeltitelTooLong() {
            OrderDto dto = createValidOrderDto();
            dto.setArtikeltitel("A".repeat(501));

            Set<ConstraintViolation<OrderDto>> violations = validator.validate(dto);

            assertFalse(violations.isEmpty());
            assertTrue(violations.stream()
                    .anyMatch(v -> v.getPropertyPath().toString().equals("artikeltitel")));
        }

        @Test
        @DisplayName("Should fail when zeitschriftentitel exceeds max length")
        void testZeitschriftentitelTooLong() {
            OrderDto dto = createValidOrderDto();
            dto.setZeitschriftentitel("A".repeat(256));

            Set<ConstraintViolation<OrderDto>> violations = validator.validate(dto);

            assertFalse(violations.isEmpty());
            assertTrue(violations.stream()
                    .anyMatch(v -> v.getPropertyPath().toString().equals("zeitschriftentitel")));
        }

        @Test
        @DisplayName("Should fail when author exceeds max length")
        void testAuthorTooLong() {
            OrderDto dto = createValidOrderDto();
            dto.setAuthor("A".repeat(256));

            Set<ConstraintViolation<OrderDto>> violations = validator.validate(dto);

            assertFalse(violations.isEmpty());
            assertTrue(violations.stream()
                    .anyMatch(v -> v.getPropertyPath().toString().equals("author")));
        }

        @Test
        @DisplayName("Should fail when issn exceeds max length")
        void testIssnTooLong() {
            OrderDto dto = createValidOrderDto();
            dto.setIssn("X".repeat(21));

            Set<ConstraintViolation<OrderDto>> violations = validator.validate(dto);

            assertFalse(violations.isEmpty());
        }

        @Test
        @DisplayName("Should fail when isbn exceeds max length")
        void testIsbnTooLong() {
            OrderDto dto = createValidOrderDto();
            dto.setIsbn("X".repeat(21));

            Set<ConstraintViolation<OrderDto>> violations = validator.validate(dto);

            assertFalse(violations.isEmpty());
        }

        @Test
        @DisplayName("Should fail when doi exceeds max length")
        void testDoiTooLong() {
            OrderDto dto = createValidOrderDto();
            dto.setDoi("X".repeat(101));

            Set<ConstraintViolation<OrderDto>> violations = validator.validate(dto);

            assertFalse(violations.isEmpty());
        }

        @Test
        @DisplayName("Should fail when systembemerkung exceeds max length")
        void testSystembemerkungTooLong() {
            OrderDto dto = createValidOrderDto();
            dto.setSystembemerkung("X".repeat(2001));

            Set<ConstraintViolation<OrderDto>> violations = validator.validate(dto);

            assertFalse(violations.isEmpty());
        }

        @Test
        @DisplayName("Should fail when notizen exceeds max length")
        void testNotizenTooLong() {
            OrderDto dto = createValidOrderDto();
            dto.setNotizen("X".repeat(2001));

            Set<ConstraintViolation<OrderDto>> violations = validator.validate(dto);

            assertFalse(violations.isEmpty());
        }
    }

    @Nested
    @DisplayName("Default Values")
    class DefaultValues {

        @Test
        @DisplayName("Should have default mediatype")
        void testDefaultMediatype() {
            OrderDto dto = new OrderDto();

            assertEquals("Artikel", dto.getMediatype());
        }

        @Test
        @DisplayName("Should have default priority")
        void testDefaultPriority() {
            OrderDto dto = new OrderDto();

            assertEquals("normal", dto.getPriority());
        }

        @Test
        @DisplayName("Should have default waehrung")
        void testDefaultWaehrung() {
            OrderDto dto = new OrderDto();

            assertEquals("CHF", dto.getWaehrung());
        }

        @Test
        @DisplayName("Should have default erledigt false")
        void testDefaultErledigt() {
            OrderDto dto = new OrderDto();

            assertFalse(dto.isErledigt());
        }
    }

    @Nested
    @DisplayName("Media Type Methods")
    class MediaTypeMethods {

        @Test
        @DisplayName("Should identify journal article")
        void testIsJournalArticle() {
            OrderDto dto = new OrderDto();
            dto.setMediatype("Artikel");

            assertTrue(dto.isJournalArticle());
            assertFalse(dto.isBook());
        }

        @Test
        @DisplayName("Should identify book")
        void testIsBook() {
            OrderDto dto = new OrderDto();
            dto.setMediatype("Buch");

            assertFalse(dto.isJournalArticle());
            assertTrue(dto.isBook());
        }

        @Test
        @DisplayName("Should identify partial book copy")
        void testIsPartialBook() {
            OrderDto dto = new OrderDto();
            dto.setMediatype("Teilkopie Buch");

            assertFalse(dto.isJournalArticle());
            assertTrue(dto.isBook());
        }

        @Test
        @DisplayName("Should handle unknown mediatype")
        void testUnknownMediatype() {
            OrderDto dto = new OrderDto();
            dto.setMediatype("Other");

            assertFalse(dto.isJournalArticle());
            assertFalse(dto.isBook());
        }
    }

    @Nested
    @DisplayName("Getters and Setters")
    class GettersSetters {

        @Test
        @DisplayName("Should set and get ID")
        void testIdGetterSetter() {
            OrderDto dto = new OrderDto();
            dto.setId(123L);

            assertEquals(123L, dto.getId());
        }

        @Test
        @DisplayName("Should set and get lieferantId")
        void testLieferantIdGetterSetter() {
            OrderDto dto = new OrderDto();
            dto.setLieferantId(456L);

            assertEquals(456L, dto.getLieferantId());
        }

        @Test
        @DisplayName("Should set and get kaufpreis")
        void testKaufpreisGetterSetter() {
            OrderDto dto = new OrderDto();
            dto.setKaufpreis(new BigDecimal("29.99"));

            assertEquals(new BigDecimal("29.99"), dto.getKaufpreis());
        }

        @Test
        @DisplayName("Should set and get forUserId")
        void testForUserIdGetterSetter() {
            OrderDto dto = new OrderDto();
            dto.setForUserId(789L);

            assertEquals(789L, dto.getForUserId());
        }

        @Test
        @DisplayName("Should set and get all book fields")
        void testBookFields() {
            OrderDto dto = new OrderDto();
            dto.setBuchtitel("Test Book");
            dto.setKapitel("Chapter 1");
            dto.setVerlag("Test Publisher");

            assertEquals("Test Book", dto.getBuchtitel());
            assertEquals("Chapter 1", dto.getKapitel());
            assertEquals("Test Publisher", dto.getVerlag());
        }

        @Test
        @DisplayName("Should set and get tracking numbers")
        void testTrackingNumbers() {
            OrderDto dto = new OrderDto();
            dto.setSubitonr("SUB123");
            dto.setGbvnr("GBV456");
            dto.setTrackingnr("TRK789");
            dto.setInterneBestellnr("INT001");

            assertEquals("SUB123", dto.getSubitonr());
            assertEquals("GBV456", dto.getGbvnr());
            assertEquals("TRK789", dto.getTrackingnr());
            assertEquals("INT001", dto.getInterneBestellnr());
        }

        @Test
        @DisplayName("Should set and get library info")
        void testLibraryInfo() {
            OrderDto dto = new OrderDto();
            dto.setSigel("DE-123");
            dto.setBibliothek("Test Library");
            dto.setSignatur("ABC/123");

            assertEquals("DE-123", dto.getSigel());
            assertEquals("Test Library", dto.getBibliothek());
            assertEquals("ABC/123", dto.getSignatur());
        }
    }
}
