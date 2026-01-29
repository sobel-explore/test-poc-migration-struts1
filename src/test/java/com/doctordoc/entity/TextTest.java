//  Copyright (C) 2005 - 2010  Markus Fischer, Pascal Steiner
//
//  This program is free software; you can redistribute it and/or
//  modify it under the terms of the GNU General Public License
//  as published by the Free Software Foundation; version 2 of the License.

package com.doctordoc.entity;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for Text entity.
 */
class TextTest {

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Default constructor creates empty Text")
        void testDefaultConstructor() {
            Text text = new Text();
            assertNull(text.getId());
            assertNull(text.getKontoId());
            assertNull(text.getTexttyp());
            assertNull(text.getInhalt());
        }

        @Test
        @DisplayName("Constructor with id sets id")
        void testIdConstructor() {
            Text text = new Text(1L);
            assertEquals(1L, text.getId());
        }
    }

    @Nested
    @DisplayName("Getter/Setter Tests")
    class GetterSetterTests {

        @Test
        @DisplayName("Should get and set id")
        void testId() {
            Text text = new Text();
            text.setId(1L);
            assertEquals(1L, text.getId());
        }

        @Test
        @DisplayName("Should get and set kontoId")
        void testKontoId() {
            Text text = new Text();
            text.setKontoId(100L);
            assertEquals(100L, text.getKontoId());
        }

        @Test
        @DisplayName("Should get and set texttyp")
        void testTexttyp() {
            Text text = new Text();
            text.setTexttyp("category");
            assertEquals("category", text.getTexttyp());
        }

        @Test
        @DisplayName("Should get and set inhalt")
        void testInhalt() {
            Text text = new Text();
            text.setInhalt("Test content");
            assertEquals("Test content", text.getInhalt());
        }
    }

    @Nested
    @DisplayName("toString Tests")
    class ToStringTests {

        @Test
        @DisplayName("toString returns inhalt when not null")
        void testToStringWithInhalt() {
            Text text = new Text();
            text.setInhalt("Test content");
            assertEquals("Test content", text.toString());
        }

        @Test
        @DisplayName("toString returns empty string when inhalt is null")
        void testToStringWithNullInhalt() {
            Text text = new Text();
            text.setInhalt(null);
            assertEquals("", text.toString());
        }
    }
}
