//  Copyright (C) 2005 - 2010  Markus Fischer, Pascal Steiner
//
//  This program is free software; you can redistribute it and/or
//  modify it under the terms of the GNU General Public License
//  as published by the Free Software Foundation; version 2 of the License.

package com.doctordoc.controller;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import com.doctordoc.config.DoctorDocProperties;
import com.doctordoc.config.TestSecurityConfig;
import com.doctordoc.entity.Benutzer;
import com.doctordoc.entity.Konto;
import com.doctordoc.security.DoctorDocUserDetails;
import com.doctordoc.service.UserService;

/**
 * Integration tests for UserController.
 */
@WebMvcTest(UserController.class)
@Import(TestSecurityConfig.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @MockBean
    private DoctorDocProperties doctorDocProperties;

    private Benutzer testLibrarian;
    private Benutzer testUser;
    private Konto testKonto;
    private DoctorDocUserDetails librarianDetails;
    private DoctorDocUserDetails librarianDetailsNoKonto;
    private DoctorDocUserDetails userDetails;

    @BeforeEach
    void setUp() {
        // Setup test konto
        testKonto = new Konto();
        testKonto.setId(1L);
        testKonto.setBibliotheksname("Test Bibliothek");
        testKonto.setKontostatus(true);

        // Setup test librarian
        testLibrarian = new Benutzer();
        testLibrarian.setId(1L);
        testLibrarian.setEmail("librarian@test.ch");
        testLibrarian.setPassword("hashedPassword");
        testLibrarian.setVorname("Test");
        testLibrarian.setName("Librarian");
        testLibrarian.setRechte(Benutzer.ROLE_LIBRARIAN);
        testLibrarian.setKontostatus(true);
        testLibrarian.setLoginopt(true);
        testLibrarian.setKontos(new HashSet<>(Arrays.asList(testKonto)));

        // Setup test user
        testUser = new Benutzer();
        testUser.setId(2L);
        testUser.setEmail("user@test.ch");
        testUser.setVorname("Regular");
        testUser.setName("User");
        testUser.setRechte(Benutzer.ROLE_USER);
        testUser.setKontostatus(true);
        testUser.setKontos(new HashSet<>(Arrays.asList(testKonto)));

        // Setup librarian details with active konto
        librarianDetails = new DoctorDocUserDetails(testLibrarian);
        librarianDetails.setActiveKonto(testKonto);
        librarianDetails.setAvailableKontos(Arrays.asList(testKonto));

        // Setup librarian details without active konto
        librarianDetailsNoKonto = new DoctorDocUserDetails(testLibrarian);
        librarianDetailsNoKonto.setActiveKonto(null);
        librarianDetailsNoKonto.setAvailableKontos(Collections.emptyList());

        // Setup user details
        userDetails = new DoctorDocUserDetails(testUser);
        userDetails.setActiveKonto(testKonto);
        userDetails.setAvailableKontos(Arrays.asList(testKonto));
    }

    @Nested
    @DisplayName("List Users Tests")
    class ListUsersTests {

        @Test
        @DisplayName("Should redirect to selectkonto when no active konto")
        void testListUsersNoKonto() throws Exception {
            mockMvc.perform(get("/user/list")
                            .with(user(librarianDetailsNoKonto)))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/selectkonto"));
        }

        @Test
        @Disabled("Requires full Thymeleaf templates - run with @SpringBootTest for integration")
        @DisplayName("Should show user list when konto is active")
        void testListUsersWithKonto() throws Exception {
            when(userService.findByKontoId(1L)).thenReturn(Arrays.asList(testUser));

            mockMvc.perform(get("/user/list")
                            .with(user(librarianDetails)))
                    .andExpect(status().isOk())
                    .andExpect(view().name("user/list"))
                    .andExpect(model().attributeExists("users"))
                    .andExpect(model().attributeExists("konto"));
        }

        @Test
        @DisplayName("Should redirect to login for unauthenticated user")
        void testListUsersUnauthenticated() throws Exception {
            mockMvc.perform(get("/user/list"))
                    .andExpect(status().is3xxRedirection());
        }
    }

    @Nested
    @DisplayName("Add User Tests")
    class AddUserTests {

        @Test
        @DisplayName("Should redirect to selectkonto when no active konto")
        void testAddUserFormNoKonto() throws Exception {
            mockMvc.perform(get("/user/add")
                            .with(user(librarianDetailsNoKonto)))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/selectkonto"));
        }

        @Test
        @Disabled("Requires full Thymeleaf templates - run with @SpringBootTest for integration")
        @DisplayName("Should show add form when konto is active")
        void testAddUserFormWithKonto() throws Exception {
            mockMvc.perform(get("/user/add")
                            .with(user(librarianDetails)))
                    .andExpect(status().isOk())
                    .andExpect(view().name("user/form"))
                    .andExpect(model().attributeExists("userDto"));
        }

        @Test
        @DisplayName("Should deny access for regular user")
        void testAddUserFormDeniedForUser() throws Exception {
            mockMvc.perform(get("/user/add")
                            .with(user(userDetails)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Should redirect on POST without konto")
        void testAddUserPostNoKonto() throws Exception {
            mockMvc.perform(post("/user/add")
                            .with(user(librarianDetailsNoKonto))
                            .with(csrf())
                            .param("vorname", "Test")
                            .param("name", "User")
                            .param("email", "test@example.com")
                            .param("password", "password123"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/selectkonto"));
        }
    }

    @Nested
    @DisplayName("Edit User Tests")
    class EditUserTests {

        @Test
        @DisplayName("Should redirect to list when user not found")
        void testEditUserNotFound() throws Exception {
            when(userService.findById(999L)).thenReturn(Optional.empty());

            mockMvc.perform(get("/user/edit/999")
                            .with(user(librarianDetails)))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/user/list"));
        }

        @Test
        @Disabled("Requires full Thymeleaf templates - run with @SpringBootTest for integration")
        @DisplayName("Should show edit form for user in same konto")
        void testEditUserFound() throws Exception {
            when(userService.findById(2L)).thenReturn(Optional.of(testUser));

            mockMvc.perform(get("/user/edit/2")
                            .with(user(librarianDetails)))
                    .andExpect(status().isOk())
                    .andExpect(view().name("user/form"))
                    .andExpect(model().attributeExists("userDto"));
        }

        @Test
        @DisplayName("Should deny access for regular user")
        void testEditUserDeniedForUser() throws Exception {
            mockMvc.perform(get("/user/edit/1")
                            .with(user(userDetails)))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("Delete User Tests")
    class DeleteUserTests {

        @Test
        @DisplayName("Should redirect when no konto")
        void testDeleteUserNoKonto() throws Exception {
            mockMvc.perform(post("/user/delete/2")
                            .with(user(librarianDetailsNoKonto))
                            .with(csrf()))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/selectkonto"));
        }

        @Test
        @DisplayName("Should deny access for regular user")
        void testDeleteUserDeniedForUser() throws Exception {
            mockMvc.perform(post("/user/delete/1")
                            .with(user(userDetails))
                            .with(csrf()))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Should redirect to login for unauthenticated user")
        void testDeleteUserUnauthenticated() throws Exception {
            mockMvc.perform(post("/user/delete/1")
                            .with(csrf()))
                    .andExpect(status().is3xxRedirection());
        }

        @Test
        @DisplayName("Should prevent deleting self")
        void testDeleteUserSelf() throws Exception {
            mockMvc.perform(post("/user/delete/1")  // Librarian's own ID
                            .with(user(librarianDetails))
                            .with(csrf()))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/user/list"));
        }

        @Test
        @DisplayName("Should delete user in same konto")
        void testDeleteUserSuccess() throws Exception {
            when(userService.findById(2L)).thenReturn(Optional.of(testUser));

            mockMvc.perform(post("/user/delete/2")
                            .with(user(librarianDetails))
                            .with(csrf()))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/user/list"));

            verify(userService).delete(2L);
        }
    }

    @Nested
    @DisplayName("Profile Tests")
    class ProfileTests {

        @Test
        @Disabled("Requires full Thymeleaf templates - run with @SpringBootTest for integration")
        @DisplayName("Should allow any authenticated user to access profile")
        void testProfileAccessible() throws Exception {
            mockMvc.perform(get("/user/MENU_PROFILEe")
                            .with(user(userDetails)))
                    .andExpect(status().isOk())
                    .andExpect(view().name("user/MENU_PROFILEe"))
                    .andExpect(model().attributeExists("userDto"));
        }

        @Test
        @DisplayName("Should redirect unauthenticated user to login")
        void testProfileUnauthenticated() throws Exception {
            mockMvc.perform(get("/user/MENU_PROFILEe"))
                    .andExpect(status().is3xxRedirection());
        }
    }

    @Nested
    @DisplayName("Security Tests")
    class SecurityTests {

        @Test
        @DisplayName("All user management endpoints require authentication")
        void testEndpointsRequireAuth() throws Exception {
            mockMvc.perform(get("/user/list")).andExpect(status().is3xxRedirection());
            mockMvc.perform(get("/user/add")).andExpect(status().is3xxRedirection());
            mockMvc.perform(get("/user/edit/1")).andExpect(status().is3xxRedirection());
        }

        @Test
        @Disabled("Requires full Thymeleaf templates - run with @SpringBootTest for integration")
        @DisplayName("Admin should have access to user management")
        void testAdminAccess() throws Exception {
            // Create admin user
            Benutzer adminUser = new Benutzer();
            adminUser.setId(99L);
            adminUser.setEmail("admin@test.ch");
            adminUser.setRechte(Benutzer.ROLE_ADMIN);
            adminUser.setKontostatus(true);
            adminUser.setKontos(new HashSet<>(Arrays.asList(testKonto)));

            DoctorDocUserDetails adminDetails = new DoctorDocUserDetails(adminUser);
            adminDetails.setActiveKonto(testKonto);

            when(userService.findByKontoId(1L)).thenReturn(Arrays.asList(testUser));

            mockMvc.perform(get("/user/list")
                            .with(user(adminDetails)))
                    .andExpect(status().isOk());
        }
    }
}
