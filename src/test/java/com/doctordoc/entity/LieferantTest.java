//  Copyright (C) 2005 - 2010  Markus Fischer, Pascal Steiner
//
//  This program is free software; you can redistribute it and/or
//  modify it under the terms of the GNU General Public License
//  as published by the Free Software Foundation; version 2 of the License.

package com.doctordoc.entity;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for Lieferant entity.
 */
class LieferantTest {

    @Test
    @DisplayName("New Lieferant should have default allgemein false")
    void testDefaultAllgemein() {
        Lieferant lieferant = new Lieferant();
        assertFalse(lieferant.getAllgemein());
    }

    @Test
    @DisplayName("Should get and set id")
    void testId() {
        Lieferant lieferant = new Lieferant();
        lieferant.setId(1L);
        assertEquals(1L, lieferant.getId());
    }

    @Test
    @DisplayName("Should get and set siegel")
    void testSiegel() {
        Lieferant lieferant = new Lieferant();
        lieferant.setSiegel("DE-123");
        assertEquals("DE-123", lieferant.getSiegel());
    }

    @Test
    @DisplayName("Should get and set lieferant name")
    void testLieferant() {
        Lieferant lieferant = new Lieferant();
        lieferant.setLieferant("Test Supplier");
        assertEquals("Test Supplier", lieferant.getLieferant());
    }

    @Test
    @DisplayName("Should get and set emailILL")
    void testEmailILL() {
        Lieferant lieferant = new Lieferant();
        lieferant.setEmailILL("ill@example.com");
        assertEquals("ill@example.com", lieferant.getEmailILL());
    }

    @Test
    @DisplayName("Should get and set countryCode")
    void testCountryCode() {
        Lieferant lieferant = new Lieferant();
        lieferant.setCountryCode("DE");
        assertEquals("DE", lieferant.getCountryCode());
    }

    @Test
    @DisplayName("Should get and set allgemein")
    void testAllgemein() {
        Lieferant lieferant = new Lieferant();
        lieferant.setAllgemein(true);
        assertTrue(lieferant.getAllgemein());
    }

    @Test
    @DisplayName("Should get and set kontoId")
    void testKontoId() {
        Lieferant lieferant = new Lieferant();
        lieferant.setKontoId(100L);
        assertEquals(100L, lieferant.getKontoId());
    }
}
