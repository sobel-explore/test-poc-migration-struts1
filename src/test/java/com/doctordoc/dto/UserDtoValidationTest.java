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
 * Validation tests for UserDto.
 */
class UserDtoValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    private UserDto createValidUserDto() {
        UserDto dto = new UserDto();
        dto.setVorname("Test");
        dto.setName("User");
        dto.setEmail("test@example.com");
        dto.setPassword("password123");
        return dto;
    }

    @Nested
    @DisplayName("Name Validation")
    class NameValidation {

        @Test
        @DisplayName("Should be valid with proper names")
        void testValidNames() {
            UserDto dto = createValidUserDto();

            Set<ConstraintViolation<UserDto>> violations = validator.validate(dto);

            assertTrue(violations.isEmpty());
        }

        @Test
        @DisplayName("Should fail when vorname is blank")
        void testBlankVorname() {
            UserDto dto = createValidUserDto();
            dto.setVorname("");

            Set<ConstraintViolation<UserDto>> violations = validator.validate(dto);

            assertFalse(violations.isEmpty());
            assertTrue(violations.stream()
                    .anyMatch(v -> v.getPropertyPath().toString().equals("vorname")));
        }

        @Test
        @DisplayName("Should fail when name is blank")
        void testBlankName() {
            UserDto dto = createValidUserDto();
            dto.setName("");

            Set<ConstraintViolation<UserDto>> violations = validator.validate(dto);

            assertFalse(violations.isEmpty());
            assertTrue(violations.stream()
                    .anyMatch(v -> v.getPropertyPath().toString().equals("name")));
        }

        @Test
        @DisplayName("Should fail when vorname exceeds max length")
        void testVornameTooLong() {
            UserDto dto = createValidUserDto();
            dto.setVorname("A".repeat(101));

            Set<ConstraintViolation<UserDto>> violations = validator.validate(dto);

            assertFalse(violations.isEmpty());
        }

        @Test
        @DisplayName("Should fail when name exceeds max length")
        void testNameTooLong() {
            UserDto dto = createValidUserDto();
            dto.setName("A".repeat(101));

            Set<ConstraintViolation<UserDto>> violations = validator.validate(dto);

            assertFalse(violations.isEmpty());
        }
    }

    @Nested
    @DisplayName("Email Validation")
    class EmailValidation {

        @Test
        @DisplayName("Should fail when email is blank")
        void testBlankEmail() {
            UserDto dto = createValidUserDto();
            dto.setEmail("");

            Set<ConstraintViolation<UserDto>> violations = validator.validate(dto);

            assertFalse(violations.isEmpty());
            assertTrue(violations.stream()
                    .anyMatch(v -> v.getPropertyPath().toString().equals("email")));
        }

        @Test
        @DisplayName("Should fail when email is invalid format")
        void testInvalidEmailFormat() {
            UserDto dto = createValidUserDto();
            dto.setEmail("not-an-email");

            Set<ConstraintViolation<UserDto>> violations = validator.validate(dto);

            assertFalse(violations.isEmpty());
            assertTrue(violations.stream()
                    .anyMatch(v -> v.getPropertyPath().toString().equals("email")));
        }

        @Test
        @DisplayName("Should accept valid email formats")
        void testValidEmailFormats() {
            UserDto dto = createValidUserDto();

            dto.setEmail("user@example.com");
            assertTrue(validator.validate(dto).isEmpty());

            dto.setEmail("user.name@example.org");
            assertTrue(validator.validate(dto).isEmpty());

            dto.setEmail("user+tag@example.co.uk");
            assertTrue(validator.validate(dto).isEmpty());
        }
    }

    @Nested
    @DisplayName("Password Validation")
    class PasswordValidation {

        @Test
        @DisplayName("Should fail when password is too short")
        void testPasswordTooShort() {
            UserDto dto = createValidUserDto();
            dto.setPassword("12345");

            Set<ConstraintViolation<UserDto>> violations = validator.validate(dto);

            assertFalse(violations.isEmpty());
            assertTrue(violations.stream()
                    .anyMatch(v -> v.getPropertyPath().toString().equals("password")));
        }

        @Test
        @DisplayName("Should be valid with password of 6 characters")
        void testPasswordMinLength() {
            UserDto dto = createValidUserDto();
            dto.setPassword("123456");

            Set<ConstraintViolation<UserDto>> violations = validator.validate(dto);

            assertTrue(violations.isEmpty());
        }

        @Test
        @DisplayName("Should be valid with null password (for updates)")
        void testNullPassword() {
            UserDto dto = createValidUserDto();
            dto.setPassword(null);

            Set<ConstraintViolation<UserDto>> violations = validator.validate(dto);

            assertTrue(violations.isEmpty());
        }
    }

    @Nested
    @DisplayName("Size Validation")
    class SizeValidation {

        @Test
        @DisplayName("Should fail when adresse exceeds max length")
        void testAdresseTooLong() {
            UserDto dto = createValidUserDto();
            dto.setAdresse("A".repeat(256));

            Set<ConstraintViolation<UserDto>> violations = validator.validate(dto);

            assertFalse(violations.isEmpty());
        }

        @Test
        @DisplayName("Should fail when PLZ exceeds max length")
        void testPlzTooLong() {
            UserDto dto = createValidUserDto();
            dto.setPlz("1".repeat(21));

            Set<ConstraintViolation<UserDto>> violations = validator.validate(dto);

            assertFalse(violations.isEmpty());
        }

        @Test
        @DisplayName("Should fail when librarycard exceeds max length")
        void testLibrarycardTooLong() {
            UserDto dto = createValidUserDto();
            dto.setLibrarycard("X".repeat(51));

            Set<ConstraintViolation<UserDto>> violations = validator.validate(dto);

            assertFalse(violations.isEmpty());
        }
    }

    @Nested
    @DisplayName("Setter Trimming")
    class SetterTrimming {

        @Test
        @DisplayName("Should trim vorname on set")
        void testVornameTrimming() {
            UserDto dto = new UserDto();
            dto.setVorname("  Test  ");

            assertEquals("Test", dto.getVorname());
        }

        @Test
        @DisplayName("Should trim name on set")
        void testNameTrimming() {
            UserDto dto = new UserDto();
            dto.setName("  User  ");

            assertEquals("User", dto.getName());
        }

        @Test
        @DisplayName("Should trim email on set")
        void testEmailTrimming() {
            UserDto dto = new UserDto();
            dto.setEmail("  test@example.com  ");

            assertEquals("test@example.com", dto.getEmail());
        }

        @Test
        @DisplayName("Should handle null values in setters")
        void testNullValues() {
            UserDto dto = new UserDto();
            dto.setVorname(null);
            dto.setName(null);
            dto.setEmail(null);

            assertNull(dto.getVorname());
            assertNull(dto.getName());
            assertNull(dto.getEmail());
        }
    }

    @Nested
    @DisplayName("Full Name Method")
    class FullNameMethod {

        @Test
        @DisplayName("Should return full name with both parts")
        void testFullNameBothParts() {
            UserDto dto = new UserDto();
            dto.setVorname("Test");
            dto.setName("User");

            assertEquals("Test User", dto.getFullName());
        }

        @Test
        @DisplayName("Should return only vorname when name is null")
        void testFullNameOnlyVorname() {
            UserDto dto = new UserDto();
            dto.setVorname("Test");

            assertEquals("Test", dto.getFullName());
        }

        @Test
        @DisplayName("Should return only name when vorname is null")
        void testFullNameOnlyName() {
            UserDto dto = new UserDto();
            dto.setName("User");

            assertEquals("User", dto.getFullName());
        }

        @Test
        @DisplayName("Should return empty string when both are null")
        void testFullNameBothNull() {
            UserDto dto = new UserDto();

            assertEquals("", dto.getFullName());
        }
    }

    @Nested
    @DisplayName("Default Values")
    class DefaultValues {

        @Test
        @DisplayName("Should have default anrede")
        void testDefaultAnrede() {
            UserDto dto = new UserDto();

            assertEquals("", dto.getAnrede());
        }

        @Test
        @DisplayName("Should have default kontostatus")
        void testDefaultKontostatus() {
            UserDto dto = new UserDto();

            assertTrue(dto.isKontostatus());
        }

        @Test
        @DisplayName("Should have default rechte as USER")
        void testDefaultRechte() {
            UserDto dto = new UserDto();

            assertEquals(1, dto.getRechte());
        }

        @Test
        @DisplayName("Should have default librarycard")
        void testDefaultLibrarycard() {
            UserDto dto = new UserDto();

            assertEquals("", dto.getLibrarycard());
        }
    }
}
