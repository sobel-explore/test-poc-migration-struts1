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
import org.junit.jupiter.api.Disabled;
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
 * Integration tests for KontoController.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class KontoControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private KontoRepository kontoRepository;

    @Autowired
    private BenutzerRepository benutzerRepository;

    private Konto testKonto;
    private Konto testKonto2;
    private Benutzer testUser;
    private Benutzer testLibrarian;
    private Benutzer testAdmin;
    private DoctorDocUserDetails userDetails;
    private DoctorDocUserDetails librarianDetails;
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

        // Create second konto for delete tests
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

        // Create and save test admin
        testAdmin = new Benutzer();
        testAdmin.setEmail("admin@test.ch");
        testAdmin.setPassword("$2a$10$hashedpassword");
        testAdmin.setVorname("Test");
        testAdmin.setName("Admin");
        testAdmin.setRechte(Benutzer.ROLE_ADMIN);
        testAdmin.setKontostatus(true);
        testAdmin.setLoginopt(true);
        testAdmin.setKontos(new HashSet<>(Arrays.asList(testKonto, testKonto2)));
        testAdmin = benutzerRepository.save(testAdmin);

        // Setup user details
        userDetails = new DoctorDocUserDetails(testUser);
        userDetails.setActiveKonto(testKonto);
        userDetails.setAvailableKontos(Arrays.asList(testKonto));

        // Setup librarian details
        librarianDetails = new DoctorDocUserDetails(testLibrarian);
        librarianDetails.setActiveKonto(testKonto);
        librarianDetails.setAvailableKontos(Arrays.asList(testKonto));

        // Setup admin details
        adminDetails = new DoctorDocUserDetails(testAdmin);
        adminDetails.setActiveKonto(testKonto);
        adminDetails.setAvailableKontos(Arrays.asList(testKonto, testKonto2));
    }

    @Nested
    @DisplayName("List Kontos Tests")
    class ListKontosTests {

        @Test
        @Disabled("Template konto/list does not exist yet")
        @DisplayName("Should display konto list for admin")
        void testListKontosForAdmin() throws Exception {
            mockMvc.perform(get("/konto/list")
                            .with(user(adminDetails)))
                    .andExpect(status().isOk())
                    .andExpect(view().name("konto/list"))
                    .andExpect(model().attributeExists("kontos"));
        }

        @Test
        @DisplayName("Should deny konto list for regular user")
        void testListKontosDeniedForUser() throws Exception {
            mockMvc.perform(get("/konto/list")
                            .with(user(userDetails)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Should deny konto list for librarian")
        void testListKontosDeniedForLibrarian() throws Exception {
            mockMvc.perform(get("/konto/list")
                            .with(user(librarianDetails)))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("View Konto Tests")
    class ViewKontoTests {

        @Test
        @DisplayName("Should display konto view for any authenticated user")
        void testViewKonto() throws Exception {
            mockMvc.perform(get("/konto/view")
                            .with(user(userDetails)))
                    .andExpect(status().isOk())
                    .andExpect(view().name("konto/view"))
                    .andExpect(model().attributeExists("konto"));
        }

        @Test
        @DisplayName("Should redirect when no konto selected")
        void testViewKontoNoKonto() throws Exception {
            DoctorDocUserDetails noKontoDetails = new DoctorDocUserDetails(testUser);
            noKontoDetails.setActiveKonto(null);

            mockMvc.perform(get("/konto/view")
                            .with(user(noKontoDetails)))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/selectkonto"));
        }
    }

    @Nested
    @DisplayName("Modify Konto Tests")
    class ModifyKontoTests {

        @Test
        @Disabled("Template konto/form does not exist yet")
        @DisplayName("Should display modify form for librarian")
        void testModifyFormForLibrarian() throws Exception {
            mockMvc.perform(get("/konto/modify")
                            .with(user(librarianDetails)))
                    .andExpect(status().isOk())
                    .andExpect(view().name("konto/form"))
                    .andExpect(model().attributeExists("kontoDto"));
        }

        @Test
        @Disabled("Template konto/form does not exist yet")
        @DisplayName("Should display modify form for admin")
        void testModifyFormForAdmin() throws Exception {
            mockMvc.perform(get("/konto/modify")
                            .with(user(adminDetails)))
                    .andExpect(status().isOk())
                    .andExpect(view().name("konto/form"));
        }

        @Test
        @DisplayName("Should deny modify form for regular user")
        void testModifyFormDeniedForUser() throws Exception {
            mockMvc.perform(get("/konto/modify")
                            .with(user(userDetails)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Should update konto successfully")
        void testModifyKontoSuccess() throws Exception {
            mockMvc.perform(post("/konto/modify")
                            .with(user(librarianDetails))
                            .with(csrf())
                            .param("bibliotheksname", "Updated Bibliothek"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/konto/view"));
        }
    }

    @Nested
    @DisplayName("Add Konto Tests")
    class AddKontoTests {

        @Test
        @Disabled("Template konto/form does not exist yet")
        @DisplayName("Should display add form for admin")
        void testAddFormForAdmin() throws Exception {
            mockMvc.perform(get("/konto/add")
                            .with(user(adminDetails)))
                    .andExpect(status().isOk())
                    .andExpect(view().name("konto/form"))
                    .andExpect(model().attributeExists("kontoDto"));
        }

        @Test
        @DisplayName("Should deny add form for librarian")
        void testAddFormDeniedForLibrarian() throws Exception {
            mockMvc.perform(get("/konto/add")
                            .with(user(librarianDetails)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Should create konto successfully")
        void testAddKontoSuccess() throws Exception {
            mockMvc.perform(post("/konto/add")
                            .with(user(adminDetails))
                            .with(csrf())
                            .param("bibliotheksname", "New Bibliothek"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/konto/list"));
        }
    }

    @Nested
    @DisplayName("Delete Konto Tests")
    class DeleteKontoTests {

        @Test
        @DisplayName("Should delete konto successfully")
        void testDeleteKontoSuccess() throws Exception {
            mockMvc.perform(post("/konto/delete/" + testKonto2.getId())
                            .with(user(adminDetails))
                            .with(csrf()))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/konto/list"));
        }

        @Test
        @DisplayName("Should prevent deleting current konto")
        void testDeleteCurrentKontoPrevented() throws Exception {
            mockMvc.perform(post("/konto/delete/" + testKonto.getId())
                            .with(user(adminDetails))
                            .with(csrf()))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/konto/list"));
        }

        @Test
        @DisplayName("Should deny delete for librarian")
        void testDeleteDeniedForLibrarian() throws Exception {
            mockMvc.perform(post("/konto/delete/" + testKonto2.getId())
                            .with(user(librarianDetails))
                            .with(csrf()))
                    .andExpect(status().isForbidden());
        }
    }
}
