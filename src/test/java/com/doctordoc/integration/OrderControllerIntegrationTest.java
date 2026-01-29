//  Copyright (C) 2005 - 2010  Markus Fischer, Pascal Steiner
//
//  This program is free software; you can redistribute it and/or
//  modify it under the terms of the GNU General Public License
//  as published by the Free Software Foundation; version 2 of the License.

package com.doctordoc.integration;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.Arrays;
import java.util.HashSet;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.doctordoc.entity.Benutzer;
import com.doctordoc.entity.Bestellungen;
import com.doctordoc.entity.Konto;
import com.doctordoc.repository.BenutzerRepository;
import com.doctordoc.repository.BestellungenRepository;
import com.doctordoc.repository.KontoRepository;
import com.doctordoc.security.DoctorDocUserDetails;

/**
 * Full integration tests for OrderController with @SpringBootTest.
 * Tests the complete request/response cycle including template rendering.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class OrderControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private KontoRepository kontoRepository;

    @Autowired
    private BenutzerRepository benutzerRepository;

    @Autowired
    private BestellungenRepository bestellungenRepository;

    private Konto testKonto;
    private Benutzer testUser;
    private Benutzer testLibrarian;
    private Bestellungen testOrder;
    private DoctorDocUserDetails userDetails;
    private DoctorDocUserDetails librarianDetails;

    @BeforeEach
    void setUp() {
        // Create and save test konto
        testKonto = new Konto();
        testKonto.setBibliotheksname("Test Bibliothek");
        testKonto.setKontostatus(true);
        testKonto.setDefaultDeloptions("post");
        testKonto.setTimezone("Europe/Berlin");
        testKonto = kontoRepository.save(testKonto);

        // Create and save test user
        testUser = new Benutzer();
        testUser.setEmail("user@test.ch");
        testUser.setPassword("$2a$10$hashedpassword");
        testUser.setVorname("Test");
        testUser.setName("User");
        testUser.setRechte(Benutzer.ROLE_USER);
        testUser.setKontostatus(true);
        testUser.setLoginopt(true);
        testUser.setKontos(new HashSet<>(Arrays.asList(testKonto)));
        testUser = benutzerRepository.save(testUser);

        // Create and save test librarian
        testLibrarian = new Benutzer();
        testLibrarian.setEmail("librarian@test.ch");
        testLibrarian.setPassword("$2a$10$hashedpassword");
        testLibrarian.setVorname("Test");
        testLibrarian.setName("Librarian");
        testLibrarian.setRechte(Benutzer.ROLE_LIBRARIAN);
        testLibrarian.setKontostatus(true);
        testLibrarian.setLoginopt(true);
        testLibrarian.setKontos(new HashSet<>(Arrays.asList(testKonto)));
        testLibrarian = benutzerRepository.save(testLibrarian);

        // Create and save test order
        testOrder = new Bestellungen();
        testOrder.setKontoId(testKonto.getId());
        testOrder.setBenutzerId(testUser.getId());
        testOrder.setArtikeltitel("Test Article");
        testOrder.setMediatype("Artikel");
        testOrder.setState("pending");
        testOrder.setZeitschrift("");
        testOrder.setAutor("");
        testOrder.setJahr("");
        testOrder.setJahrgang("");
        testOrder.setHeft("");
        testOrder.setSeiten("");
        testOrder.setIsbn("");
        testOrder.setDoi("");
        testOrder.setPmid("");
        testOrder.setBuchtitel("");
        testOrder.setBuchkapitel("");
        testOrder.setVerlag("");
        testOrder.setFileformat("");
        testOrder.setDeloptions("post");
        testOrder.setSignatur("");
        testOrder.setTrackingnr("");
        testOrder.setInternenr("");
        testOrder.setSystembemerkung("");
        testOrder.setNotizen("");
        testOrder.setOrderpriority("normal");
        testOrder.setLieferantId(0L);
        testOrder = bestellungenRepository.save(testOrder);

        // Setup user details
        userDetails = new DoctorDocUserDetails(testUser);
        userDetails.setActiveKonto(testKonto);
        userDetails.setAvailableKontos(Arrays.asList(testKonto));

        // Setup librarian details
        librarianDetails = new DoctorDocUserDetails(testLibrarian);
        librarianDetails.setActiveKonto(testKonto);
        librarianDetails.setAvailableKontos(Arrays.asList(testKonto));
    }

    @Nested
    @DisplayName("New Order Form Tests")
    class NewOrderFormTests {

        @Test
        @DisplayName("Should display new order form")
        void testNewOrderFormDisplayed() throws Exception {
            mockMvc.perform(get("/order/new")
                            .with(user(userDetails)))
                    .andExpect(status().isOk())
                    .andExpect(view().name("order/form"))
                    .andExpect(model().attributeExists("orderDto"))
                    .andExpect(model().attributeExists("konto"));
        }

        @Test
        @DisplayName("Should pre-fill OpenURL parameters")
        void testNewOrderWithOpenUrlParams() throws Exception {
            mockMvc.perform(get("/order/new")
                            .with(user(userDetails))
                            .param("issn", "1234-5678")
                            .param("doi", "10.1000/xyz"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("order/form"));
        }
    }

    @Nested
    @DisplayName("Create Order Tests")
    class CreateOrderTests {

        @Test
        @DisplayName("Should create new order and redirect")
        void testCreateOrderSuccess() throws Exception {
            mockMvc.perform(post("/order/new")
                            .with(user(userDetails))
                            .with(csrf())
                            .param("artikeltitel", "New Test Article")
                            .param("mediatype", "Artikel")
                            .param("zeitschriftentitel", "Test Journal"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/uebersicht"));
        }
    }

    @Nested
    @DisplayName("Edit Order Tests")
    class EditOrderTests {

        @Test
        @DisplayName("Should display edit form for librarian")
        void testEditOrderFormDisplayed() throws Exception {
            mockMvc.perform(get("/order/edit/" + testOrder.getId())
                            .with(user(librarianDetails)))
                    .andExpect(status().isOk())
                    .andExpect(view().name("order/form"))
                    .andExpect(model().attributeExists("orderDto"))
                    .andExpect(model().attributeExists("order"));
        }

        @Test
        @DisplayName("Should update order and redirect")
        void testUpdateOrderSuccess() throws Exception {
            mockMvc.perform(post("/order/edit/" + testOrder.getId())
                            .with(user(librarianDetails))
                            .with(csrf())
                            .param("artikeltitel", "Updated Article Title")
                            .param("mediatype", "Artikel"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/uebersicht/detail/" + testOrder.getId()));
        }
    }

    @Nested
    @DisplayName("Update Status Tests")
    class UpdateStatusTests {

        @Test
        @DisplayName("Should update order status")
        void testUpdateStatusSuccess() throws Exception {
            mockMvc.perform(post("/order/status/" + testOrder.getId())
                            .with(user(librarianDetails))
                            .with(csrf())
                            .param("status", "processing"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/uebersicht/detail/" + testOrder.getId()));
        }
    }

    @Nested
    @DisplayName("Delete Order Tests")
    class DeleteOrderTests {

        @Test
        @DisplayName("Should delete order and redirect")
        void testDeleteOrderSuccess() throws Exception {
            mockMvc.perform(post("/order/delete/" + testOrder.getId())
                            .with(user(librarianDetails))
                            .with(csrf()))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/uebersicht"));
        }
    }
}
