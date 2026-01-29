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
import com.doctordoc.entity.Konto;
import com.doctordoc.repository.BenutzerRepository;
import com.doctordoc.repository.KontoRepository;
import com.doctordoc.security.DoctorDocUserDetails;

/**
 * Full integration tests for UserController with @SpringBootTest.
 * Tests the complete request/response cycle including template rendering.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class UserControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private KontoRepository kontoRepository;

    @Autowired
    private BenutzerRepository benutzerRepository;

    private Konto testKonto;
    private Benutzer testLibrarian;
    private Benutzer testUser;
    private DoctorDocUserDetails librarianDetails;
    private DoctorDocUserDetails userDetails;

    @BeforeEach
    void setUp() {
        // Create and save test konto
        testKonto = new Konto();
        testKonto.setBibliotheksname("Test Bibliothek");
        testKonto.setKontostatus(true);
        testKonto.setDefaultDeloptions("post");
        testKonto.setTimezone("Europe/Berlin");
        testKonto = kontoRepository.save(testKonto);

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

        // Setup librarian details
        librarianDetails = new DoctorDocUserDetails(testLibrarian);
        librarianDetails.setActiveKonto(testKonto);
        librarianDetails.setAvailableKontos(Arrays.asList(testKonto));

        // Setup user details
        userDetails = new DoctorDocUserDetails(testUser);
        userDetails.setActiveKonto(testKonto);
        userDetails.setAvailableKontos(Arrays.asList(testKonto));
    }

    @Nested
    @DisplayName("List Users Tests")
    class ListUsersTests {

        @Test
        @DisplayName("Should display user list for librarian")
        void testListUsersDisplayed() throws Exception {
            mockMvc.perform(get("/user/list")
                            .with(user(librarianDetails)))
                    .andExpect(status().isOk())
                    .andExpect(view().name("user/list"))
                    .andExpect(model().attributeExists("users"))
                    .andExpect(model().attributeExists("konto"));
        }
    }

    @Nested
    @DisplayName("Add User Tests")
    class AddUserTests {

        @Test
        @DisplayName("Should display add user form")
        void testAddUserFormDisplayed() throws Exception {
            mockMvc.perform(get("/user/add")
                            .with(user(librarianDetails)))
                    .andExpect(status().isOk())
                    .andExpect(view().name("user/form"))
                    .andExpect(model().attributeExists("userDto"));
        }

        @Test
        @DisplayName("Should create new user and redirect")
        void testCreateUserSuccess() throws Exception {
            mockMvc.perform(post("/user/add")
                            .with(user(librarianDetails))
                            .with(csrf())
                            .param("email", "newuser@test.ch")
                            .param("vorname", "New")
                            .param("name", "User")
                            .param("password", "password123")
                            .param("passwordConfirm", "password123"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/user/list"));
        }
    }

    @Nested
    @DisplayName("Edit User Tests")
    class EditUserTests {

        @Test
        @DisplayName("Should display edit user form")
        void testEditUserFormDisplayed() throws Exception {
            mockMvc.perform(get("/user/edit/" + testUser.getId())
                            .with(user(librarianDetails)))
                    .andExpect(status().isOk())
                    .andExpect(view().name("user/form"))
                    .andExpect(model().attributeExists("userDto"));
        }

        @Test
        @DisplayName("Should update user and redirect")
        void testUpdateUserSuccess() throws Exception {
            mockMvc.perform(post("/user/edit/" + testUser.getId())
                            .with(user(librarianDetails))
                            .with(csrf())
                            .param("email", "updated@test.ch")
                            .param("vorname", "Updated")
                            .param("name", "User"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/user/list"));
        }

        @Test
        @DisplayName("Should redirect when user not found")
        void testEditUserNotFound() throws Exception {
            mockMvc.perform(get("/user/edit/99999")
                            .with(user(librarianDetails)))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/user/list"));
        }
    }

    @Nested
    @DisplayName("Delete User Tests")
    class DeleteUserTests {

        @Test
        @DisplayName("Should delete user and redirect")
        void testDeleteUserSuccess() throws Exception {
            mockMvc.perform(post("/user/delete/" + testUser.getId())
                            .with(user(librarianDetails))
                            .with(csrf()))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/user/list"));
        }

        @Test
        @DisplayName("Should prevent librarian from deleting self")
        void testDeleteSelfPrevented() throws Exception {
            mockMvc.perform(post("/user/delete/" + testLibrarian.getId())
                            .with(user(librarianDetails))
                            .with(csrf()))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/user/list"));
        }
    }
}
