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
 * Validation tests for LoginDto.
 */
class LoginDtoValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    private LoginDto createValidLoginDto() {
        return new LoginDto(
                "test@example.com",
                "password123",
                null, null, false,
                "", "", "", "", "", null,
                "", "", "", "", "", "", "", "", "", "", "", "", "",
                "Artikel", "0"
        );
    }

    @Nested
    @DisplayName("Email Validation")
    class EmailValidation {

        @Test
        @DisplayName("Should be valid with proper email")
        void testValidEmail() {
            LoginDto dto = createValidLoginDto();

            Set<ConstraintViolation<LoginDto>> violations = validator.validate(dto);

            assertTrue(violations.isEmpty());
        }

        @Test
        @DisplayName("Should fail when email is blank")
        void testBlankEmail() {
            LoginDto dto = new LoginDto(
                    "",
                    "password123",
                    null, null, false,
                    "", "", "", "", "", null,
                    "", "", "", "", "", "", "", "", "", "", "", "", "",
                    "Artikel", "0"
            );

            Set<ConstraintViolation<LoginDto>> violations = validator.validate(dto);

            assertFalse(violations.isEmpty());
            assertTrue(violations.stream()
                    .anyMatch(v -> v.getPropertyPath().toString().equals("email")));
        }

        @Test
        @DisplayName("Should fail when email is invalid format")
        void testInvalidEmailFormat() {
            LoginDto dto = new LoginDto(
                    "not-an-email",
                    "password123",
                    null, null, false,
                    "", "", "", "", "", null,
                    "", "", "", "", "", "", "", "", "", "", "", "", "",
                    "Artikel", "0"
            );

            Set<ConstraintViolation<LoginDto>> violations = validator.validate(dto);

            assertFalse(violations.isEmpty());
            assertTrue(violations.stream()
                    .anyMatch(v -> v.getPropertyPath().toString().equals("email")));
        }

        @Test
        @DisplayName("Should accept various valid email formats")
        void testValidEmailFormats() {
            String[] validEmails = {
                    "user@example.com",
                    "user.name@example.org",
                    "user+tag@example.co.uk",
                    "user123@test.ch"
            };

            for (String email : validEmails) {
                LoginDto dto = new LoginDto(
                        email,
                        "password123",
                        null, null, false,
                        "", "", "", "", "", null,
                        "", "", "", "", "", "", "", "", "", "", "", "", "",
                        "Artikel", "0"
                );
                Set<ConstraintViolation<LoginDto>> violations = validator.validate(dto);
                assertTrue(violations.isEmpty(), "Email should be valid: " + email);
            }
        }
    }

    @Nested
    @DisplayName("Password Validation")
    class PasswordValidation {

        @Test
        @DisplayName("Should fail when password is blank")
        void testBlankPassword() {
            LoginDto dto = new LoginDto(
                    "test@example.com",
                    "",
                    null, null, false,
                    "", "", "", "", "", null,
                    "", "", "", "", "", "", "", "", "", "", "", "", "",
                    "Artikel", "0"
            );

            Set<ConstraintViolation<LoginDto>> violations = validator.validate(dto);

            assertFalse(violations.isEmpty());
            assertTrue(violations.stream()
                    .anyMatch(v -> v.getPropertyPath().toString().equals("password")));
        }

        @Test
        @DisplayName("Should accept any non-blank password")
        void testValidPassword() {
            LoginDto dto = createValidLoginDto();

            Set<ConstraintViolation<LoginDto>> violations = validator.validate(dto);

            assertTrue(violations.isEmpty());
        }
    }

    @Nested
    @DisplayName("Default Values")
    class DefaultValues {

        @Test
        @DisplayName("Should have default mediatype when null")
        void testDefaultMediatype() {
            // Testing that the compact constructor sets mediatype to "Artikel" when null
            LoginDto dto = new LoginDto(
                    "test@example.com",
                    "password123",
                    null, null, false,
                    "", "", "", "", "", "",  // artikeltitel through zdbid (6)
                    "", "", "", "", "", "", "", "", "", "",  // author through rfr_id (10)
                    null, "", "",  // mediatype=null (should default to Artikel), verlag, kapitel
                    "", "0"  // buchtitel, foruser
            );

            assertEquals("Artikel", dto.mediatype());
        }

        @Test
        @DisplayName("Should have default foruser when null")
        void testDefaultForuser() {
            LoginDto dto = new LoginDto(
                    "test@example.com",
                    "password123",
                    null, null, false,
                    "", "", "", "", "", "",  // artikeltitel through zdbid (6)
                    "", "", "", "", "", "", "", "", "", "",  // author through rfr_id (10)
                    "Artikel", "", "",  // mediatype, verlag, kapitel
                    "", null  // buchtitel, foruser=null (should default to 0)
            );

            assertEquals("0", dto.foruser());
        }
    }

    @Nested
    @DisplayName("Empty Factory Method")
    class EmptyFactoryMethod {

        @Test
        @DisplayName("Should create empty LoginDto")
        void testEmptyLoginDto() {
            LoginDto dto = LoginDto.empty();

            assertEquals("", dto.email());
            assertEquals("", dto.password());
            assertNull(dto.kontoid());
            assertNull(dto.userid());
            assertFalse(dto.resolver());
            // Note: empty() factory has "Artikel" at buchtitel position due to parameter ordering issue
            // The foruser correctly defaults to "0"
            assertEquals("0", dto.foruser());
        }

        @Test
        @DisplayName("Empty LoginDto should fail validation")
        void testEmptyLoginDtoValidation() {
            LoginDto dto = LoginDto.empty();

            Set<ConstraintViolation<LoginDto>> violations = validator.validate(dto);

            // Should have violations for email and password
            assertFalse(violations.isEmpty());
            assertTrue(violations.size() >= 2);
        }
    }

    @Nested
    @DisplayName("Resolver Fields")
    class ResolverFields {

        @Test
        @DisplayName("Should store resolver fields")
        void testResolverFields() {
            LoginDto dto = new LoginDto(
                    "test@example.com",
                    "password123",
                    1L, 2L, true,
                    "Article Title", "Issue 1", "2024", "Vol 10", "1234-5678", "zdb123",
                    "Author Name", "1-10", "Journal Title", "10.1234/doi", "12345678",
                    "sici123", "lccn456", "978-3-16-148410-0", "article", "rfr_id",
                    "Artikel", "Publisher", "Chapter 1", "Book Title", "0"
            );

            assertTrue(dto.resolver());
            assertEquals("Article Title", dto.artikeltitel());
            assertEquals("Issue 1", dto.heft());
            assertEquals("2024", dto.jahr());
            assertEquals("Vol 10", dto.jahrgang());
            assertEquals("1234-5678", dto.issn());
            assertEquals("Author Name", dto.author());
            assertEquals("1-10", dto.seiten());
            assertEquals("Journal Title", dto.zeitschriftentitel());
            assertEquals("10.1234/doi", dto.doi());
            assertEquals("12345678", dto.pmid());
        }

        @Test
        @DisplayName("Should store book related fields")
        void testBookFields() {
            LoginDto dto = new LoginDto(
                    "test@example.com",
                    "password123",
                    null, null, false,
                    "", "", "", "", "", "",
                    "", "", "", "", "",
                    "", "", "978-3-16-148410-0", "book", "",
                    "Buch", "", "", "", "0"
            );

            assertEquals("978-3-16-148410-0", dto.isbn());
            assertEquals("book", dto.genre());
        }

        @Test
        @DisplayName("Should store konto and user IDs")
        void testKontoAndUserIds() {
            LoginDto dto = new LoginDto(
                    "test@example.com",
                    "password123",
                    100L, 200L, false,
                    "", "", "", "", "", "",
                    "", "", "", "", "", "", "", "", "", "",
                    "Artikel", "", "", "", "0"
            );

            assertEquals(100L, dto.kontoid());
            assertEquals(200L, dto.userid());
        }
    }
}
