//  Copyright (C) 2012  Markus Fischer
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

package com.doctordoc.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for the Check utility class.
 * Migrated from JUnit 4 to JUnit 5 for Spring Boot compatibility.
 */
class CheckTest {

    private Check check;

    @BeforeEach
    void setUp() {
        check = new Check();
    }

    @Nested
    @DisplayName("Email Validation Tests")
    class EmailValidationTests {

        @Test
        @DisplayName("Should validate correct email addresses")
        void testValidEmails() {
            assertTrue(check.isEmail("test@test.ch"));
            assertTrue(check.isEmail("C.O`Su@test.test.ch"));
            assertTrue(check.isEmail("Test.TEST@test.ch"));
        }

        @Test
        @DisplayName("Should reject invalid email addresses")
        void testInvalidEmails() {
            assertFalse(check.isEmail("test@testch"));
            assertFalse(check.isEmail("test@test.c"));
            assertFalse(check.isEmail("'test@test.ch'"));
            assertFalse(check.isEmail("test@test.kom"));
            assertFalse(check.isEmail("test@test"));
            assertFalse(check.isEmail("test@"));
            assertFalse(check.isEmail("test"));
            assertFalse(check.isEmail("@"));
            assertFalse(check.isEmail(""));
            assertFalse(check.isEmail(null));
        }
    }

    @Nested
    @DisplayName("String Length Tests")
    class StringLengthTests {

        @Test
        @DisplayName("Should validate minimum length correctly")
        void testIsMinLength() {
            assertTrue(check.isMinLength("", 0));
            assertTrue(check.isMinLength("Z", 0));
            assertTrue(check.isMinLength("Z", 1));
            assertFalse(check.isMinLength("Z", 2));
            assertTrue(check.isMinLength("ABC", 3));
            assertFalse(check.isMinLength(null, 0));
        }

        @Test
        @DisplayName("Should validate exact length correctly")
        void testIsExactLength() {
            assertTrue(check.isExactLength("", 0));
            assertFalse(check.isExactLength("Z", 0));
            assertTrue(check.isExactLength("Z", 1));
            assertFalse(check.isExactLength("Z", 2));
            assertTrue(check.isExactLength("ABC", 3));
            assertFalse(check.isExactLength(null, 0));
        }

        @Test
        @DisplayName("Should validate length in min/max range correctly")
        void testIsLengthInMinMax() {
            assertTrue(check.isLengthInMinMax("", 0, 1));
            assertFalse(check.isLengthInMinMax("", 1, 2));
            assertTrue(check.isLengthInMinMax("Z", 0, 1));
            assertTrue(check.isLengthInMinMax("Z", 1, 1));
            assertTrue(check.isLengthInMinMax("Z", 1, 2));
            assertFalse(check.isLengthInMinMax("Z", 2, 3));
            assertFalse(check.isLengthInMinMax("Z", 2, 0));
            assertFalse(check.isLengthInMinMax("Z", 2, 1));
            assertTrue(check.isLengthInMinMax("ABC", 0, 20));
            assertFalse(check.isLengthInMinMax(null, 30, 106));
        }
    }

    @Nested
    @DisplayName("URL Validation Tests")
    class UrlValidationTests {

        @Test
        @DisplayName("Should validate correct URLs")
        void testValidUrls() {
            assertTrue(check.isUrl("http://test.ch"));
            assertTrue(check.isUrl("https://test.ch"));
            assertTrue(check.isUrl("ftp://test.ch"));
        }

        @Test
        @DisplayName("Should reject invalid URLs")
        void testInvalidUrls() {
            assertFalse(check.isUrl(""));
            assertFalse(check.isUrl("www.test.ch"));
            assertFalse(check.isUrl("fttp://test.ch"));
            assertFalse(check.isUrl(null));
        }
    }

    @Nested
    @DisplayName("Filetype Extension Tests")
    class FiletypeExtensionTests {

        @Test
        @DisplayName("Should validate filetype extensions correctly")
        void testIsFiletypeExtension() {
            assertTrue(check.isFiletypeExtension("test.xls", ".xls"));
            assertTrue(check.isFiletypeExtension("test.XLs", ".xlS"));
            assertTrue(check.isFiletypeExtension("test.pdf.xls", ".xls"));
            assertFalse(check.isFiletypeExtension("testxls", ".xls"));
            assertFalse(check.isFiletypeExtension("", ".xls"));
            assertFalse(check.isFiletypeExtension("", ""));
            assertFalse(check.isFiletypeExtension(null, null));
            assertFalse(check.isFiletypeExtension(null, ".xls"));
        }
    }

    @Nested
    @DisplayName("Alphanumeric Word Character Tests")
    class AlphanumericWordCharacterTests {

        @Test
        @DisplayName("Should extract alphanumeric word characters correctly")
        void testGetAlphanumericWordCharacters() {
            assertEquals(0, check.getAlphanumericWordCharacters("").size());
            assertEquals(0, check.getAlphanumericWordCharacters(" !?--_ * ").size());
            assertEquals(2, check.getAlphanumericWordCharacters("Aa9o= 2El ").size());
            assertEquals(4, check.getAlphanumericWordCharacters("Aa!9o= 2? El ").size());
            assertEquals(1, check.getAlphanumericWordCharacters("Altersabhängigkeit").size());
            assertEquals(2, check.getAlphanumericWordCharacters("page précédente").size());
            assertEquals(1, check.getAlphanumericWordCharacters("Sïmone").size());
            assertEquals(1, check.getAlphanumericWordCharacters("Château").size());
            assertEquals(1, check.getAlphanumericWordCharacters("niño").size());
            assertEquals(6, check.getAlphanumericWordCharacters("Die Grille zirpt immer um 10.").size());
            assertEquals(0, check.getAlphanumericWordCharacters(null).size());
        }
    }

    @Nested
    @DisplayName("Character Count Tests")
    class CharacterCountTests {

        @Test
        @DisplayName("Should count character occurrences correctly")
        void testCountCharacterInString() {
            assertEquals(0, check.countCharacterInString("", ""));
            assertEquals(0, check.countCharacterInString("", "test"));
            assertEquals(1, check.countCharacterInString("test", "test"));
            assertEquals(2, check.countCharacterInString("testAtest ", "test"));
            assertEquals(1, check.countCharacterInString("testATest ", "test"));
            assertEquals(0, check.countCharacterInString("test", null));
        }
    }

    @Nested
    @DisplayName("Number Validation Tests")
    class NumberValidationTests {

        @Test
        @DisplayName("Should validate strings containing only numbers")
        void testContainsOnlyNumbers() {
            assertFalse(check.containsOnlyNumbers(""));
            assertFalse(check.containsOnlyNumbers("1A20"));
            assertFalse(check.containsOnlyNumbers("11 "));
            assertTrue(check.containsOnlyNumbers("11"));
            assertFalse(check.containsOnlyNumbers(null));
        }
    }

    @Nested
    @DisplayName("ISSN Validation Tests")
    class IssnValidationTests {

        @Test
        @DisplayName("Should validate ISSN correctly")
        void testIsValidIssn() {
            assertFalse(check.isValidIssn(""));
            assertFalse(check.isValidIssn("123-1111"));
            assertFalse(check.isValidIssn(" "));
            assertTrue(check.isValidIssn("1420-4398"));
            assertFalse(check.isValidIssn(null));
        }
    }

    @Nested
    @DisplayName("Year Validation Tests")
    class YearValidationTests {

        @Test
        @DisplayName("Should validate year format correctly")
        void testIsYear() {
            assertFalse(check.isYear(""));
            assertFalse(check.isYear("AAA"));
            assertFalse(check.isYear("201"));
            assertTrue(check.isYear("1620"));
            assertTrue(check.isYear("2120"));
            assertFalse(check.isYear("2012 "));
            assertFalse(check.isYear(null));
        }
    }
}
