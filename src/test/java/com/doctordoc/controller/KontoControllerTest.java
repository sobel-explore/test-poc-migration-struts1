//  Copyright (C) 2005 - 2010  Markus Fischer, Pascal Steiner
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
//
//  Contact: info@doctor-doc.com

package com.doctordoc.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.doctordoc.config.DoctorDocProperties;
import com.doctordoc.config.TestSecurityConfig;
import com.doctordoc.entity.Benutzer;
import com.doctordoc.entity.Konto;
import com.doctordoc.security.DoctorDocUserDetails;
import com.doctordoc.service.KontoService;

/**
 * Tests for KontoController.
 * Migrated from TestKontoAction (JUnit 4 Struts MockStrutsTestCase) to Spring Boot MVC tests.
 *
 * Note: These tests focus on controller behavior (redirects, status codes, view names)
 * without requiring full Thymeleaf template rendering.
 */
@WebMvcTest(KontoController.class)
@Import(TestSecurityConfig.class)
@ActiveProfiles("test")
class KontoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private KontoService kontoService;

    @MockBean
    private DoctorDocProperties properties;

    private Benutzer testBenutzer;
    private Benutzer testAdmin;
    private Konto testKonto;
    private DoctorDocUserDetails librarianUserDetails;
    private DoctorDocUserDetails adminUserDetails;

    @BeforeEach
    void setUp() {
        // Setup librarian user
        testBenutzer = new Benutzer();
        testBenutzer.setId(1L);
        testBenutzer.setEmail("librarian@test.ch");
        testBenutzer.setPassword("testpassword");
        testBenutzer.setVorname("Test");
        testBenutzer.setName("Librarian");
        testBenutzer.setRechte(Benutzer.ROLE_LIBRARIAN);
        testBenutzer.setKontostatus(true);

        // Setup admin user
        testAdmin = new Benutzer();
        testAdmin.setId(2L);
        testAdmin.setEmail("admin@test.ch");
        testAdmin.setPassword("adminpassword");
        testAdmin.setVorname("Test");
        testAdmin.setName("Admin");
        testAdmin.setRechte(Benutzer.ROLE_ADMIN);
        testAdmin.setKontostatus(true);

        // Setup test konto
        testKonto = new Konto();
        testKonto.setId(1L);
        testKonto.setBibliotheksname("Test Bibliothek");
        testKonto.setAdresse("Teststrasse 1");
        testKonto.setPlz("12345");
        testKonto.setOrt("Teststadt");
        testKonto.setLand("CH");
        testKonto.setTelefon("0123456789");
        testKonto.setBibliotheksmail("test@bibliothek.ch");
        testKonto.setKontostatus(true);

        List<Konto> kontos = new ArrayList<>();
        kontos.add(testKonto);

        librarianUserDetails = new DoctorDocUserDetails(testBenutzer);
        librarianUserDetails.setAvailableKontos(kontos);
        librarianUserDetails.setActiveKonto(testKonto);

        adminUserDetails = new DoctorDocUserDetails(testAdmin);
        adminUserDetails.setAvailableKontos(kontos);
        adminUserDetails.setActiveKonto(testKonto);
    }

    @Nested
    @DisplayName("List Kontos Tests")
    class ListKontosTests {

        @Test
        @DisplayName("Should redirect unauthorized user to login")
        void testListKontosUnauthorized() throws Exception {
            mockMvc.perform(get("/konto/list"))
                    .andExpect(status().is3xxRedirection());
        }

        @Test
        @Disabled("Requires konto/list.html template - enable when templates are migrated")
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should list kontos for admin")
        void testListKontosAsAdmin() throws Exception {
            when(kontoService.findAll()).thenReturn(Arrays.asList(testKonto));

            MvcResult result = mockMvc.perform(get("/konto/list")
                    .with(user(adminUserDetails)))
                    .andExpect(status().isOk())
                    .andReturn();

            assertEquals("konto/list", result.getModelAndView().getViewName());
            assertNotNull(result.getModelAndView().getModel().get("kontos"));
        }

        @Test
        @WithMockUser(roles = "LIBRARIAN")
        @DisplayName("Should deny list kontos for librarian")
        void testListKontosAsLibrarian() throws Exception {
            mockMvc.perform(get("/konto/list")
                    .with(user(librarianUserDetails)))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("View Konto Tests")
    class ViewKontoTests {

        @Test
        @Disabled("Requires konto/view.html template - enable when templates are migrated")
        @WithMockUser
        @DisplayName("Should view konto details")
        void testViewKonto() throws Exception {
            MvcResult result = mockMvc.perform(get("/konto/view")
                    .with(user(librarianUserDetails)))
                    .andExpect(status().isOk())
                    .andReturn();

            assertEquals("konto/view", result.getModelAndView().getViewName());
            assertNotNull(result.getModelAndView().getModel().get("konto"));
            assertNotNull(result.getModelAndView().getModel().get("user"));
        }

        @Test
        @WithMockUser
        @DisplayName("Should redirect to selectkonto when no active konto")
        void testViewKontoNoActiveKonto() throws Exception {
            DoctorDocUserDetails userWithoutKonto = new DoctorDocUserDetails(testBenutzer);
            userWithoutKonto.setAvailableKontos(new ArrayList<>());

            mockMvc.perform(get("/konto/view")
                    .with(user(userWithoutKonto)))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/selectkonto"));
        }
    }

    @Nested
    @DisplayName("Modify Konto Tests")
    class ModifyKontoTests {

        @Test
        @Disabled("Requires konto/form.html template - enable when templates are migrated")
        @WithMockUser(roles = "LIBRARIAN")
        @DisplayName("Should show modify form for librarian")
        void testModifyKontoFormAsLibrarian() throws Exception {
            MvcResult result = mockMvc.perform(get("/konto/modify")
                    .with(user(librarianUserDetails)))
                    .andExpect(status().isOk())
                    .andReturn();

            assertEquals("konto/form", result.getModelAndView().getViewName());
            assertNotNull(result.getModelAndView().getModel().get("kontoDto"));
            assertNotNull(result.getModelAndView().getModel().get("konto"));
        }

        @Test
        @WithMockUser(roles = "LIBRARIAN")
        @DisplayName("Should process konto modification")
        void testModifyKonto() throws Exception {
            when(kontoService.save(any(Konto.class))).thenReturn(testKonto);

            mockMvc.perform(post("/konto/modify")
                    .with(user(librarianUserDetails))
                    .with(csrf())
                    .param("bibliotheksname", "Updated Bibliothek")
                    .param("adresse", "Neue Adresse 1")
                    .param("plz", "54321")
                    .param("ort", "Neue Stadt")
                    .param("land", "CH")
                    .param("bibliotheksmail", "updated@bibliothek.ch"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/konto/view"))
                    .andExpect(flash().attributeExists("success"));

            verify(kontoService).save(any(Konto.class));
        }

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("Should deny modify for regular user")
        void testModifyKontoAsUser() throws Exception {
            Benutzer regularUser = new Benutzer();
            regularUser.setId(3L);
            regularUser.setRechte(Benutzer.ROLE_USER);
            regularUser.setEmail("user@test.ch");

            List<Konto> kontos = new ArrayList<>();
            kontos.add(testKonto);
            DoctorDocUserDetails userDetails = new DoctorDocUserDetails(regularUser);
            userDetails.setAvailableKontos(kontos);
            userDetails.setActiveKonto(testKonto);

            mockMvc.perform(get("/konto/modify")
                    .with(user(userDetails)))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("Add Konto Tests")
    class AddKontoTests {

        @Test
        @Disabled("Requires konto/form.html template - enable when templates are migrated")
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should show add form for admin")
        void testAddKontoForm() throws Exception {
            MvcResult result = mockMvc.perform(get("/konto/add")
                    .with(user(adminUserDetails)))
                    .andExpect(status().isOk())
                    .andReturn();

            assertEquals("konto/form", result.getModelAndView().getViewName());
            assertNotNull(result.getModelAndView().getModel().get("kontoDto"));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should process new konto creation")
        void testAddKonto() throws Exception {
            when(kontoService.save(any(Konto.class))).thenReturn(testKonto);

            mockMvc.perform(post("/konto/add")
                    .with(user(adminUserDetails))
                    .with(csrf())
                    .param("bibliotheksname", "New Bibliothek")
                    .param("adresse", "Neue Adresse 1")
                    .param("plz", "12345")
                    .param("ort", "Test Stadt")
                    .param("land", "CH")
                    .param("bibliotheksmail", "new@bibliothek.ch"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/konto/list"))
                    .andExpect(flash().attributeExists("success"));

            verify(kontoService).save(any(Konto.class));
        }

        @Test
        @WithMockUser(roles = "LIBRARIAN")
        @DisplayName("Should deny add for librarian")
        void testAddKontoAsLibrarian() throws Exception {
            mockMvc.perform(get("/konto/add")
                    .with(user(librarianUserDetails)))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("Delete Konto Tests")
    class DeleteKontoTests {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should delete konto successfully")
        void testDeleteKonto() throws Exception {
            mockMvc.perform(post("/konto/delete/2")
                    .with(user(adminUserDetails))
                    .with(csrf()))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/konto/list"))
                    .andExpect(flash().attributeExists("success"));

            verify(kontoService).delete(2L);
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should prevent deleting current konto")
        void testDeleteCurrentKonto() throws Exception {
            mockMvc.perform(post("/konto/delete/1") // ID of active konto
                    .with(user(adminUserDetails))
                    .with(csrf()))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/konto/list"))
                    .andExpect(flash().attributeExists("error"));
        }

        @Test
        @WithMockUser(roles = "LIBRARIAN")
        @DisplayName("Should deny delete for librarian")
        void testDeleteKontoAsLibrarian() throws Exception {
            mockMvc.perform(post("/konto/delete/2")
                    .with(user(librarianUserDetails))
                    .with(csrf()))
                    .andExpect(status().isForbidden());
        }
    }
}
