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
import java.util.Collections;
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
 * Integration tests for LoginController.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class LoginControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private KontoRepository kontoRepository;

    @Autowired
    private BenutzerRepository benutzerRepository;

    private Konto testKonto;
    private Konto testKonto2;
    private Benutzer testUser;
    private DoctorDocUserDetails userDetailsWithKonto;
    private DoctorDocUserDetails userDetailsMultipleKontos;
    private DoctorDocUserDetails userDetailsNoKontos;

    @BeforeEach
    void setUp() {
        // Create and save test konto
        testKonto = new Konto();
        testKonto.setBibliotheksname("Test Bibliothek");
        testKonto.setKontostatus(true);
        testKonto.setDefaultDeloptions("post");
        testKonto.setTimezone("Europe/Berlin");
        testKonto = kontoRepository.save(testKonto);

        // Create second konto
        testKonto2 = new Konto();
        testKonto2.setBibliotheksname("Second Bibliothek");
        testKonto2.setKontostatus(true);
        testKonto2.setDefaultDeloptions("post");
        testKonto2.setTimezone("Europe/Berlin");
        testKonto2 = kontoRepository.save(testKonto2);

        // Create and save test user
        testUser = new Benutzer();
        testUser.setEmail("user@test.ch");
        testUser.setPassword("$2a$10$hashedpassword");
        testUser.setVorname("Test");
        testUser.setName("User");
        testUser.setRechte(Benutzer.ROLE_USER);
        testUser.setKontostatus(true);
        testUser.setLoginopt(true);
        testUser.setKontos(new HashSet<>(Arrays.asList(testKonto, testKonto2)));
        testUser = benutzerRepository.save(testUser);

        // Setup user with active konto
        userDetailsWithKonto = new DoctorDocUserDetails(testUser);
        userDetailsWithKonto.setActiveKonto(testKonto);
        userDetailsWithKonto.setAvailableKontos(Arrays.asList(testKonto));

        // Setup user with multiple kontos (no active)
        userDetailsMultipleKontos = new DoctorDocUserDetails(testUser);
        userDetailsMultipleKontos.setActiveKonto(null);
        userDetailsMultipleKontos.setAvailableKontos(Arrays.asList(testKonto, testKonto2));

        // Setup user with no kontos
        userDetailsNoKontos = new DoctorDocUserDetails(testUser);
        userDetailsNoKontos.setActiveKonto(null);
        userDetailsNoKontos.setAvailableKontos(Collections.emptyList());
    }

    @Nested
    @DisplayName("Home Redirect Tests")
    class HomeTests {

        @Test
        @DisplayName("Should redirect to login from home")
        void testHomeRedirectsToLogin() throws Exception {
            mockMvc.perform(get("/"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/login"));
        }
    }

    @Nested
    @DisplayName("Login Page Tests")
    class LoginPageTests {

        @Test
        @DisplayName("Should display login page")
        void testLoginPage() throws Exception {
            mockMvc.perform(get("/login"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("login"));
        }

        @Test
        @DisplayName("Should show error message when error param present")
        void testLoginWithError() throws Exception {
            mockMvc.perform(get("/login")
                            .param("error", "true"))
                    .andExpect(status().isOk())
                    .andExpect(model().attributeExists("error"));
        }

        @Test
        @DisplayName("Should show logout message when logout param present")
        void testLoginAfterLogout() throws Exception {
            mockMvc.perform(get("/login")
                            .param("logout", "true"))
                    .andExpect(status().isOk())
                    .andExpect(model().attributeExists("message"));
        }

        @Test
        @DisplayName("Should show expired message when expired param present")
        void testLoginSessionExpired() throws Exception {
            mockMvc.perform(get("/login")
                            .param("expired", "true"))
                    .andExpect(status().isOk())
                    .andExpect(model().attributeExists("message"));
        }
    }

    @Nested
    @DisplayName("Select Konto Tests")
    class SelectKontoTests {

        @Test
        @DisplayName("Should display konto selection for user with multiple kontos")
        void testSelectKontoPage() throws Exception {
            mockMvc.perform(get("/selectkonto")
                            .with(user(userDetailsMultipleKontos)))
                    .andExpect(status().isOk())
                    .andExpect(view().name("selectkonto"))
                    .andExpect(model().attributeExists("kontos"));
        }

        @Test
        @DisplayName("Should redirect to uebersicht for user with single konto")
        void testSelectKontoSingleKonto() throws Exception {
            DoctorDocUserDetails singleKontoUser = new DoctorDocUserDetails(testUser);
            singleKontoUser.setActiveKonto(null);
            singleKontoUser.setAvailableKontos(Arrays.asList(testKonto));

            mockMvc.perform(get("/selectkonto")
                            .with(user(singleKontoUser)))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/uebersicht"));
        }

        @Test
        @DisplayName("Should redirect to login when no kontos available")
        void testSelectKontoNoKontos() throws Exception {
            mockMvc.perform(get("/selectkonto")
                            .with(user(userDetailsNoKontos)))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/login?error=true"));
        }

        @Test
        @DisplayName("Should redirect to login when not authenticated")
        void testSelectKontoUnauthenticated() throws Exception {
            mockMvc.perform(get("/selectkonto"))
                    .andExpect(status().is3xxRedirection());
        }

        @Test
        @DisplayName("Should select konto and redirect to uebersicht")
        void testSelectKontoPost() throws Exception {
            mockMvc.perform(post("/selectkonto")
                            .with(user(userDetailsMultipleKontos))
                            .with(csrf())
                            .param("kontoId", testKonto.getId().toString()))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/uebersicht"));
        }

        @Test
        @DisplayName("Should redirect when invalid konto selected")
        void testSelectKontoInvalid() throws Exception {
            mockMvc.perform(post("/selectkonto")
                            .with(user(userDetailsMultipleKontos))
                            .with(csrf())
                            .param("kontoId", "99999"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/selectkonto"));
        }
    }

    @Nested
    @DisplayName("GTC (Terms) Tests")
    class GtcTests {

        @Test
        @DisplayName("Should redirect when GTC disabled")
        void testGtcDisabled() throws Exception {
            mockMvc.perform(get("/gtc")
                            .with(user(userDetailsWithKonto)))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/uebersicht"));
        }

        @Test
        @DisplayName("Should accept GTC and redirect")
        void testAcceptGtc() throws Exception {
            mockMvc.perform(post("/gtc/accept")
                            .with(user(userDetailsWithKonto))
                            .with(csrf()))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/uebersicht"));
        }

        @Test
        @DisplayName("Should redirect to login when accepting GTC unauthenticated")
        void testAcceptGtcUnauthenticated() throws Exception {
            mockMvc.perform(post("/gtc/accept")
                            .with(csrf()))
                    .andExpect(status().is3xxRedirection());
        }
    }
}
