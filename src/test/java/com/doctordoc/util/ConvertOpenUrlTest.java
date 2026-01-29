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

import java.lang.reflect.Method;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for the ConvertOpenUrl utility class.
 * Migrated from JUnit 4 to JUnit 5 for Spring Boot compatibility.
 */
class ConvertOpenUrlTest {

    private ConvertOpenUrl converter;

    @BeforeEach
    void setUp() {
        converter = new ConvertOpenUrl();
    }

    @Nested
    @DisplayName("Extract Start Page Tests")
    class ExtractStartPageTests {

        @Test
        @DisplayName("Should extract start page from page range")
        void testExtractStartPage() {
            assertEquals("60", converter.extractStartPage("60-4"));
            assertEquals("S60", converter.extractStartPage("S60-4"));
            assertEquals("60", converter.extractStartPage(" 60 - 4"));
            assertEquals("e1254", converter.extractStartPage(" e1254-56"));
            assertEquals("", converter.extractStartPage(""));
        }
    }

    @Nested
    @DisplayName("Normalize ISSN Tests")
    class NormalizeIssnTests {

        @Test
        @DisplayName("Should normalize ISSN format")
        void testNormalizeIssn() {
            assertEquals("1234-1234", converter.normalizeIssn("12341234"));
            assertEquals("", converter.extractStartPage(""));
        }
    }

    @Nested
    @DisplayName("Extract From Several ISSNs Tests")
    class ExtractFromSeveralIssnsTests {

        @Test
        @DisplayName("Should extract first ISSN from multiple ISSNs")
        void testExtractFromSeveralIssns() throws Exception {
            // Using reflection to test private method
            final Method method = ConvertOpenUrl.class.getDeclaredMethod("extractFromSeveralIssns", String.class);
            method.setAccessible(true);

            assertEquals("1234-1234", method.invoke(converter, "1234-1234; 2345-2345"));
            assertEquals("1234-12342345-2345", method.invoke(converter, "1234-12342345-2345")); // returns input
            assertEquals("1234-1234", method.invoke(converter, "1234-1234; gtz66 2345-2345"));
            assertEquals("1234-1234", method.invoke(converter, "1234-1234; gtz662345-2345"));
            assertEquals("", method.invoke(converter, ""));
        }
    }

    @Nested
    @DisplayName("Check Lowercase Tests")
    class CheckLowercaseTests {

        @Test
        @DisplayName("Should detect lowercase characters")
        void testCheckLowercase() throws Exception {
            // Using reflection to test private method
            final Method method = ConvertOpenUrl.class.getDeclaredMethod("checkLowercase", String.class);
            method.setAccessible(true);

            assertTrue((Boolean) method.invoke(converter, "aHHH"));
            assertTrue((Boolean) method.invoke(converter, "HHHa"));
            assertFalse((Boolean) method.invoke(converter, "HHH"));
            assertFalse((Boolean) method.invoke(converter, " "));
            assertFalse((Boolean) method.invoke(converter, ""));
        }
    }

    @Nested
    @DisplayName("To Normal Letters Tests")
    class ToNormalLettersTests {

        @Test
        @DisplayName("Should convert uppercase to normal case")
        void testToNormalLetters() throws Exception {
            // Using reflection to test private method
            final Method method = ConvertOpenUrl.class.getDeclaredMethod("toNormalLetters", String.class);
            method.setAccessible(true);

            assertEquals("Methods in enzymology", method.invoke(converter, "METHODS IN ENZYMOLOGY"));
            assertEquals("Methods in enzymology ", method.invoke(converter, "METHODS IN ENZYMOLOGY "));
            assertEquals("", method.invoke(converter, ""));
        }
    }
}
