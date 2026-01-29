//  Copyright (C) 2005 - 2010  Markus Fischer, Pascal Steiner
//
//  This program is free software; you can redistribute it and/or
//  modify it under the terms of the GNU General Public License
//  as published by the Free Software Foundation; version 2 of the License.

package com.doctordoc.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.web.servlet.MockMvc;

import com.doctordoc.config.DoctorDocProperties;
import com.doctordoc.config.TestSecurityConfig;
import com.doctordoc.entity.Benutzer;
import com.doctordoc.entity.Bestellungen;
import com.doctordoc.entity.Konto;
import com.doctordoc.security.DoctorDocUserDetails;
import com.doctordoc.service.OrderService;

/**
 * Integration tests for OverviewController.
 */
@WebMvcTest(OverviewController.class)
@Import(TestSecurityConfig.class)
class OverviewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OrderService orderService;

    @MockBean
    private DoctorDocProperties properties;

    private Benutzer testUser;
    private Benutzer testLibrarian;
    private Konto testKonto;
    private Konto testKonto2;
    private Bestellungen testOrder;
    private DoctorDocUserDetails userDetails;
    private DoctorDocUserDetails userDetailsNoKonto;
    private DoctorDocUserDetails userDetailsMultipleKontos;
    private DoctorDocUserDetails librarianDetails;
    private Page<Bestellungen> emptyPage;
    private Page<Bestellungen> ordersPage;

    @BeforeEach
    void setUp() {
        // Setup test konto
        testKonto = new Konto();
        testKonto.setId(1L);
        testKonto.setBibliotheksname("Test Bibliothek");
        testKonto.setKontostatus(true);

        testKonto2 = new Konto();
        testKonto2.setId(2L);
        testKonto2.setBibliotheksname("Second Bibliothek");
        testKonto2.setKontostatus(true);

        // Setup test user
        testUser = new Benutzer();
        testUser.setId(1L);
        testUser.setEmail("user@test.ch");
        testUser.setRechte(Benutzer.ROLE_USER);
        testUser.setKontostatus(true);
        testUser.setKontos(new HashSet<>(Arrays.asList(testKonto)));

        // Setup test librarian
        testLibrarian = new Benutzer();
        testLibrarian.setId(2L);
        testLibrarian.setEmail("librarian@test.ch");
        testLibrarian.setRechte(Benutzer.ROLE_LIBRARIAN);
        testLibrarian.setKontostatus(true);
        testLibrarian.setKontos(new HashSet<>(Arrays.asList(testKonto)));

        // Setup test order
        testOrder = new Bestellungen();
        testOrder.setId(1L);
        testOrder.setKontoId(1L);
        testOrder.setBenutzerId(1L);
        testOrder.setArtikeltitel("Test Article");
        testOrder.setMediatype("Artikel");
        testOrder.setState("pending");

        // Setup user details with active konto
        userDetails = new DoctorDocUserDetails(testUser);
        userDetails.setActiveKonto(testKonto);
        userDetails.setAvailableKontos(Arrays.asList(testKonto));

        // Setup user details without active konto (single konto)
        userDetailsNoKonto = new DoctorDocUserDetails(testUser);
        userDetailsNoKonto.setActiveKonto(null);
        userDetailsNoKonto.setAvailableKontos(Arrays.asList(testKonto));

        // Setup user with multiple kontos but none selected
        Benutzer multiKontoUser = new Benutzer();
        multiKontoUser.setId(3L);
        multiKontoUser.setEmail("multi@test.ch");
        multiKontoUser.setRechte(Benutzer.ROLE_USER);
        multiKontoUser.setKontostatus(true);
        multiKontoUser.setKontos(new HashSet<>(Arrays.asList(testKonto, testKonto2)));

        userDetailsMultipleKontos = new DoctorDocUserDetails(multiKontoUser);
        userDetailsMultipleKontos.setActiveKonto(null);
        userDetailsMultipleKontos.setAvailableKontos(Arrays.asList(testKonto, testKonto2));

        // Setup librarian details
        librarianDetails = new DoctorDocUserDetails(testLibrarian);
        librarianDetails.setActiveKonto(testKonto);
        librarianDetails.setAvailableKontos(Arrays.asList(testKonto));

        // Setup page data
        emptyPage = new PageImpl<>(Collections.emptyList());
        ordersPage = new PageImpl<>(Arrays.asList(testOrder));

        // Setup default properties
        when(properties.getMaxResultsDisplay()).thenReturn(100);
    }

    @Nested
    @DisplayName("Overview Page Tests")
    class OverviewPageTests {

        @Test
        @DisplayName("Should redirect to login when no konto and single konto available")
        void testOverviewNoKontoSingleKonto() throws Exception {
            mockMvc.perform(get("/uebersicht")
                            .with(user(userDetailsNoKonto)))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/login?error=true"));
        }

        @Test
        @DisplayName("Should redirect to selectkonto when multiple kontos available")
        void testOverviewNoKontoMultipleKontos() throws Exception {
            mockMvc.perform(get("/uebersicht")
                            .with(user(userDetailsMultipleKontos)))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/selectkonto"));
        }

        @Test
        @DisplayName("Should redirect to login for unauthenticated user")
        void testOverviewUnauthenticated() throws Exception {
            mockMvc.perform(get("/uebersicht"))
                    .andExpect(status().is3xxRedirection());
        }
    }

    @Nested
    @DisplayName("Order Detail Tests")
    class OrderDetailTests {

        @Test
        @DisplayName("Should redirect to selectkonto when no active konto")
        void testDetailNoKonto() throws Exception {
            mockMvc.perform(get("/uebersicht/detail/1")
                            .with(user(userDetailsNoKonto)))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/selectkonto"));
        }

        @Test
        @DisplayName("Should redirect to uebersicht when order not found")
        void testDetailOrderNotFound() throws Exception {
            when(orderService.findById(999L)).thenReturn(Optional.empty());

            mockMvc.perform(get("/uebersicht/detail/999")
                            .with(user(userDetails)))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/uebersicht"));
        }

        @Test
        @DisplayName("Should redirect to uebersicht when unauthorized")
        void testDetailUnauthorized() throws Exception {
            when(orderService.findById(1L)).thenReturn(Optional.of(testOrder));
            when(orderService.isLegitimateOrder(any(), any(), any())).thenReturn(false);

            mockMvc.perform(get("/uebersicht/detail/1")
                            .with(user(userDetails)))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/uebersicht"));
        }

        @Test
        @DisplayName("Should redirect to login for unauthenticated user")
        void testDetailUnauthenticated() throws Exception {
            mockMvc.perform(get("/uebersicht/detail/1"))
                    .andExpect(status().is3xxRedirection());
        }
    }

    @Nested
    @DisplayName("Security Tests")
    class SecurityTests {

        @Test
        @DisplayName("All overview endpoints require authentication")
        void testEndpointsRequireAuth() throws Exception {
            mockMvc.perform(get("/uebersicht")).andExpect(status().is3xxRedirection());
            mockMvc.perform(get("/uebersicht/detail/1")).andExpect(status().is3xxRedirection());
        }
    }
}
