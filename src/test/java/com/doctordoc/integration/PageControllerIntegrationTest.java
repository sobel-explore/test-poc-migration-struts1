//  Copyright (C) 2005 - 2010  Markus Fischer, Pascal Steiner
//
//  This program is free software; you can redistribute it and/or
//  modify it under the terms of the GNU General Public License
//  as published by the Free Software Foundation; version 2 of the License.

package com.doctordoc.integration;

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
import com.doctordoc.entity.Konto;
import com.doctordoc.repository.BenutzerRepository;
import com.doctordoc.repository.KontoRepository;
import com.doctordoc.security.DoctorDocUserDetails;

/**
 * Integration tests for PageController.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class PageControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private KontoRepository kontoRepository;

    @Autowired
    private BenutzerRepository benutzerRepository;

    private Konto testKonto;
    private Benutzer testUser;
    private Benutzer testAdmin;
    private DoctorDocUserDetails userDetails;
    private DoctorDocUserDetails adminDetails;

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

        // Create and save test admin
        testAdmin = new Benutzer();
        testAdmin.setEmail("admin@test.ch");
        testAdmin.setPassword("$2a$10$hashedpassword");
        testAdmin.setVorname("Test");
        testAdmin.setName("Admin");
        testAdmin.setRechte(Benutzer.ROLE_ADMIN);
        testAdmin.setKontostatus(true);
        testAdmin.setLoginopt(true);
        testAdmin.setKontos(new HashSet<>(Arrays.asList(testKonto)));
        testAdmin = benutzerRepository.save(testAdmin);

        // Setup user details
        userDetails = new DoctorDocUserDetails(testUser);
        userDetails.setActiveKonto(testKonto);
        userDetails.setAvailableKontos(Arrays.asList(testKonto));

        // Setup admin details
        adminDetails = new DoctorDocUserDetails(testAdmin);
        adminDetails.setActiveKonto(testKonto);
        adminDetails.setAvailableKontos(Arrays.asList(testKonto));
    }

    @Nested
    @DisplayName("Search Free Page Tests")
    class SearchFreeTests {

        @Test
        @DisplayName("Should display search free page")
        void testSearchFreeDisplayed() throws Exception {
            mockMvc.perform(get("/searchfree")
                            .with(user(userDetails)))
                    .andExpect(status().isOk())
                    .andExpect(view().name("search/free"))
                    .andExpect(model().attributeExists("konto"));
        }

        @Test
        @DisplayName("Should require authentication")
        void testSearchFreeRequiresAuth() throws Exception {
            mockMvc.perform(get("/searchfree"))
                    .andExpect(status().is3xxRedirection());
        }
    }

    @Nested
    @DisplayName("Search Order Page Tests")
    class SearchOrderTests {

        @Test
        @DisplayName("Should display search order page with years")
        void testSearchOrderDisplayed() throws Exception {
            mockMvc.perform(get("/searchorder")
                            .with(user(userDetails)))
                    .andExpect(status().isOk())
                    .andExpect(view().name("search/order"))
                    .andExpect(model().attributeExists("years"));
        }
    }

    @Nested
    @DisplayName("Public Pages Tests")
    class PublicPagesTests {

        @Test
        @DisplayName("Should display services page without authentication")
        void testServicesPage() throws Exception {
            mockMvc.perform(get("/services"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("pages/services"));
        }

        @Test
        @DisplayName("Should display impressum page without authentication")
        void testImpressumPage() throws Exception {
            mockMvc.perform(get("/impressum"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("pages/impressum"));
        }

        @Test
        @DisplayName("Should display howto page without authentication")
        void testHowtoPage() throws Exception {
            mockMvc.perform(get("/howto"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("pages/howto"))
                    .andExpect(model().attributeExists("activesubmenu"));
        }

        @Test
        @DisplayName("Should display howto page with submenu parameter")
        void testHowtoPageWithSubmenu() throws Exception {
            mockMvc.perform(get("/howto")
                            .param("activesubmenu", "API"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("pages/howto"))
                    .andExpect(model().attribute("activesubmenu", "API"));
        }
    }

    @Nested
    @DisplayName("Statistics Page Tests")
    class StatisticsTests {

        @Test
        @DisplayName("Should display statistics page with default dates")
        void testStatisticsPage() throws Exception {
            mockMvc.perform(get("/statistics")
                            .with(user(userDetails)))
                    .andExpect(status().isOk())
                    .andExpect(view().name("pages/statistics"))
                    .andExpect(model().attributeExists("years"))
                    .andExpect(model().attributeExists("dfrom"))
                    .andExpect(model().attributeExists("yfrom"));
        }

        @Test
        @DisplayName("Should accept date parameters")
        void testStatisticsWithParams() throws Exception {
            mockMvc.perform(get("/statistics")
                            .with(user(userDetails))
                            .param("dfrom", "1")
                            .param("mfrom", "6")
                            .param("yfrom", "2024")
                            .param("dto", "30")
                            .param("mto", "12")
                            .param("yto", "2024"))
                    .andExpect(status().isOk())
                    .andExpect(model().attribute("yfrom", 2024));
        }
    }

    @Nested
    @DisplayName("Stock Page Tests")
    class StockTests {

        @Test
        @DisplayName("Should display stock page")
        void testStockPage() throws Exception {
            mockMvc.perform(get("/stock")
                            .with(user(userDetails)))
                    .andExpect(status().isOk())
                    .andExpect(view().name("pages/stock"))
                    .andExpect(model().attributeExists("stocks"));
        }

        @Test
        @DisplayName("Should accept search query")
        void testStockWithQuery() throws Exception {
            mockMvc.perform(get("/stock")
                            .with(user(userDetails))
                            .param("q", "test search"))
                    .andExpect(status().isOk())
                    .andExpect(model().attribute("query", "test search"));
        }
    }

    @Nested
    @DisplayName("Admin Page Tests")
    class AdminTests {

        @Test
        @DisplayName("Should display admin page for admin user")
        void testAdminPage() throws Exception {
            mockMvc.perform(get("/admin")
                            .with(user(adminDetails)))
                    .andExpect(status().isOk())
                    .andExpect(view().name("pages/admin"))
                    .andExpect(model().attributeExists("appVersion"))
                    .andExpect(model().attributeExists("javaVersion"))
                    .andExpect(model().attributeExists("freeMemory"));
        }

        @Test
        @DisplayName("Should deny access for regular user")
        void testAdminDeniedForUser() throws Exception {
            mockMvc.perform(get("/admin")
                            .with(user(userDetails)))
                    .andExpect(status().isForbidden());
        }
    }
}
