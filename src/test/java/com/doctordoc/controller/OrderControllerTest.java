//  Copyright (C) 2005 - 2010  Markus Fischer, Pascal Steiner
//
//  This program is free software; you can redistribute it and/or
//  modify it under the terms of the GNU General Public License
//  as published by the Free Software Foundation; version 2 of the License.

package com.doctordoc.controller;

import static org.mockito.ArgumentMatchers.any;
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
import com.doctordoc.entity.Bestellungen;
import com.doctordoc.entity.Konto;
import com.doctordoc.repository.LieferantRepository;
import com.doctordoc.security.DoctorDocUserDetails;
import com.doctordoc.service.OrderService;
import com.doctordoc.service.UserService;

/**
 * Integration tests for OrderController.
 */
@WebMvcTest(OrderController.class)
@Import(TestSecurityConfig.class)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OrderService orderService;

    @MockBean
    private UserService userService;

    @MockBean
    private LieferantRepository lieferantRepository;

    @MockBean
    private DoctorDocProperties doctorDocProperties;

    private Benutzer testUser;
    private Benutzer testLibrarian;
    private Konto testKonto;
    private Bestellungen testOrder;
    private DoctorDocUserDetails userDetails;
    private DoctorDocUserDetails userDetailsNoKonto;
    private DoctorDocUserDetails librarianDetails;
    private DoctorDocUserDetails librarianDetailsNoKonto;

    @BeforeEach
    void setUp() {
        // Setup test konto
        testKonto = new Konto();
        testKonto.setId(1L);
        testKonto.setBibliotheksname("Test Bibliothek");
        testKonto.setKontostatus(true);
        testKonto.setDefaultDeloptions("post");

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
        testOrder.setLieferantId(1L);
        testOrder.setArtikeltitel("Test Article");
        testOrder.setMediatype("Artikel");
        testOrder.setState("pending");

        // Setup user details with active konto
        userDetails = new DoctorDocUserDetails(testUser);
        userDetails.setActiveKonto(testKonto);
        userDetails.setAvailableKontos(Arrays.asList(testKonto));

        // Setup user details without active konto
        userDetailsNoKonto = new DoctorDocUserDetails(testUser);
        userDetailsNoKonto.setActiveKonto(null);
        userDetailsNoKonto.setAvailableKontos(Collections.emptyList());

        // Setup librarian details with active konto
        librarianDetails = new DoctorDocUserDetails(testLibrarian);
        librarianDetails.setActiveKonto(testKonto);
        librarianDetails.setAvailableKontos(Arrays.asList(testKonto));

        // Setup librarian details without active konto
        librarianDetailsNoKonto = new DoctorDocUserDetails(testLibrarian);
        librarianDetailsNoKonto.setActiveKonto(null);
        librarianDetailsNoKonto.setAvailableKontos(Collections.emptyList());
    }

    @Nested
    @DisplayName("New Order Tests")
    class NewOrderTests {

        @Test
        @DisplayName("Should redirect to selectkonto when no active konto")
        void testNewOrderNoKonto() throws Exception {
            mockMvc.perform(get("/order/new")
                            .with(user(userDetailsNoKonto)))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/selectkonto"));
        }

        @Test
        @DisplayName("Should redirect to login for unauthenticated user")
        void testNewOrderUnauthenticated() throws Exception {
            mockMvc.perform(get("/order/new"))
                    .andExpect(status().is3xxRedirection());
        }

        @Test
        @Disabled("Requires full Thymeleaf templates - run with @SpringBootTest for integration")
        @DisplayName("Should accept URL parameters for OpenURL integration")
        void testNewOrderWithParameters() throws Exception {
            when(orderService.checkMaxOrdersKonto(any())).thenReturn(true);
            when(orderService.checkMaxOrdersUser(any(), any())).thenReturn(true);
            when(lieferantRepository.findByAllgemeinTrueOrKontoId(anyLong()))
                    .thenReturn(Collections.emptyList());

            mockMvc.perform(get("/order/new")
                            .with(user(userDetails))
                            .param("issn", "1234-5678")
                            .param("doi", "10.1000/test"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("order/form"))
                    .andExpect(model().attributeExists("orderDto"));
        }

        @Test
        @Disabled("Requires full Thymeleaf templates - run with @SpringBootTest for integration")
        @DisplayName("Should show order form when konto is active and limits OK")
        void testNewOrderFormSuccess() throws Exception {
            when(orderService.checkMaxOrdersKonto(any())).thenReturn(true);
            when(orderService.checkMaxOrdersUser(any(), any())).thenReturn(true);
            when(lieferantRepository.findByAllgemeinTrueOrKontoId(anyLong()))
                    .thenReturn(Collections.emptyList());

            mockMvc.perform(get("/order/new")
                            .with(user(userDetails)))
                    .andExpect(status().isOk())
                    .andExpect(view().name("order/form"))
                    .andExpect(model().attributeExists("orderDto"));
        }

        @Test
        @Disabled("Requires full Thymeleaf templates - run with @SpringBootTest for integration")
        @DisplayName("Should show error when konto order limit exceeded")
        void testNewOrderKontoLimitExceeded() throws Exception {
            when(orderService.checkMaxOrdersKonto(any())).thenReturn(false);

            mockMvc.perform(get("/order/new")
                            .with(user(userDetails)))
                    .andExpect(status().isOk())
                    .andExpect(view().name("error/orderlimit"));
        }

        @Test
        @Disabled("Requires full Thymeleaf templates - run with @SpringBootTest for integration")
        @DisplayName("Should show error when user order limit exceeded")
        void testNewOrderUserLimitExceeded() throws Exception {
            when(orderService.checkMaxOrdersKonto(any())).thenReturn(true);
            when(orderService.checkMaxOrdersUser(any(), any())).thenReturn(false);

            mockMvc.perform(get("/order/new")
                            .with(user(userDetails)))
                    .andExpect(status().isOk())
                    .andExpect(view().name("error/orderlimit"));
        }
    }

    @Nested
    @DisplayName("Create Order Tests")
    class CreateOrderTests {

        @Test
        @DisplayName("Should redirect to selectkonto when no active konto")
        void testCreateOrderNoKonto() throws Exception {
            mockMvc.perform(post("/order/new")
                            .with(user(userDetailsNoKonto))
                            .with(csrf())
                            .param("artikeltitel", "Test Article")
                            .param("mediatype", "Artikel"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/selectkonto"));
        }

        @Test
        @DisplayName("Should redirect to login for unauthenticated user")
        void testCreateOrderUnauthenticated() throws Exception {
            mockMvc.perform(post("/order/new")
                            .with(csrf())
                            .param("artikeltitel", "Test"))
                    .andExpect(status().is3xxRedirection());
        }

        @Test
        @DisplayName("Should create order successfully")
        void testCreateOrderSuccess() throws Exception {
            when(lieferantRepository.findByAllgemeinTrueOrKontoId(anyLong()))
                    .thenReturn(Collections.emptyList());

            mockMvc.perform(post("/order/new")
                            .with(user(userDetails))
                            .with(csrf())
                            .param("artikeltitel", "Test Article")
                            .param("mediatype", "Artikel"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/uebersicht"));

            verify(orderService).save(any(Bestellungen.class));
        }
    }

    @Nested
    @DisplayName("Edit Order Tests")
    class EditOrderTests {

        @Test
        @DisplayName("Should redirect to uebersicht when order not found")
        void testEditOrderNotFound() throws Exception {
            when(orderService.findById(999L)).thenReturn(Optional.empty());

            mockMvc.perform(get("/order/edit/999")
                            .with(user(librarianDetails)))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/uebersicht"));
        }

        @Test
        @DisplayName("Should deny access for regular user")
        void testEditOrderDeniedForUser() throws Exception {
            mockMvc.perform(get("/order/edit/1")
                            .with(user(userDetails)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Should redirect when no konto selected")
        void testEditOrderNoKonto() throws Exception {
            mockMvc.perform(get("/order/edit/1")
                            .with(user(librarianDetailsNoKonto)))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/selectkonto"));
        }

        @Test
        @Disabled("Requires full Thymeleaf templates - run with @SpringBootTest for integration")
        @DisplayName("Should show edit form for valid order")
        void testEditOrderSuccess() throws Exception {
            when(orderService.findById(1L)).thenReturn(Optional.of(testOrder));
            when(orderService.isLegitimateOrder(any(), any(), any())).thenReturn(true);
            when(lieferantRepository.findByAllgemeinTrueOrKontoId(anyLong()))
                    .thenReturn(Collections.emptyList());
            when(userService.findByKontoId(anyLong())).thenReturn(Arrays.asList(testUser));

            mockMvc.perform(get("/order/edit/1")
                            .with(user(librarianDetails)))
                    .andExpect(status().isOk())
                    .andExpect(view().name("order/form"))
                    .andExpect(model().attributeExists("orderDto"));
        }
    }

    @Nested
    @DisplayName("Update Order Tests")
    class UpdateOrderTests {

        @Test
        @DisplayName("Should redirect to selectkonto when no active konto")
        void testUpdateOrderNoKonto() throws Exception {
            mockMvc.perform(post("/order/edit/1")
                            .with(user(librarianDetailsNoKonto))
                            .with(csrf())
                            .param("artikeltitel", "Updated Article"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/selectkonto"));
        }

        @Test
        @DisplayName("Should deny access for regular user")
        void testUpdateOrderDeniedForUser() throws Exception {
            mockMvc.perform(post("/order/edit/1")
                            .with(user(userDetails))
                            .with(csrf())
                            .param("artikeltitel", "Test"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Should update order successfully")
        void testUpdateOrderSuccess() throws Exception {
            when(orderService.findById(1L)).thenReturn(Optional.of(testOrder));
            when(orderService.isLegitimateOrder(any(), any(), any())).thenReturn(true);
            when(lieferantRepository.findByAllgemeinTrueOrKontoId(anyLong()))
                    .thenReturn(Collections.emptyList());
            when(userService.findByKontoId(anyLong())).thenReturn(Arrays.asList(testUser));

            mockMvc.perform(post("/order/edit/1")
                            .with(user(librarianDetails))
                            .with(csrf())
                            .param("artikeltitel", "Updated Article"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/uebersicht/detail/1"));

            verify(orderService).save(any(Bestellungen.class));
        }
    }

    @Nested
    @DisplayName("Update Status Tests")
    class UpdateStatusTests {

        @Test
        @DisplayName("Should redirect to selectkonto when no active konto")
        void testUpdateStatusNoKonto() throws Exception {
            mockMvc.perform(post("/order/status/1")
                            .with(user(librarianDetailsNoKonto))
                            .with(csrf())
                            .param("status", "processing"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/selectkonto"));
        }

        @Test
        @DisplayName("Should deny access for regular user")
        void testUpdateStatusDeniedForUser() throws Exception {
            mockMvc.perform(post("/order/status/1")
                            .with(user(userDetails))
                            .with(csrf())
                            .param("status", "processing"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Should redirect to login for unauthenticated user")
        void testUpdateStatusUnauthenticated() throws Exception {
            mockMvc.perform(post("/order/status/1")
                            .with(csrf())
                            .param("status", "processing"))
                    .andExpect(status().is3xxRedirection());
        }

        @Test
        @DisplayName("Should update status successfully")
        void testUpdateStatusSuccess() throws Exception {
            when(orderService.findById(1L)).thenReturn(Optional.of(testOrder));
            when(orderService.isLegitimateOrder(any(), any(), any())).thenReturn(true);

            mockMvc.perform(post("/order/status/1")
                            .with(user(librarianDetails))
                            .with(csrf())
                            .param("status", "processing"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/uebersicht/detail/1"));

            verify(orderService).updateStatus(1L, "processing");
        }
    }

    @Nested
    @DisplayName("Delete Order Tests")
    class DeleteOrderTests {

        @Test
        @DisplayName("Should redirect to selectkonto when no active konto")
        void testDeleteOrderNoKonto() throws Exception {
            mockMvc.perform(post("/order/delete/1")
                            .with(user(librarianDetailsNoKonto))
                            .with(csrf()))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/selectkonto"));
        }

        @Test
        @DisplayName("Should deny access for regular user")
        void testDeleteOrderDeniedForUser() throws Exception {
            mockMvc.perform(post("/order/delete/1")
                            .with(user(userDetails))
                            .with(csrf()))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Should redirect to login for unauthenticated user")
        void testDeleteOrderUnauthenticated() throws Exception {
            mockMvc.perform(post("/order/delete/1")
                            .with(csrf()))
                    .andExpect(status().is3xxRedirection());
        }

        @Test
        @DisplayName("Should delete order successfully")
        void testDeleteOrderSuccess() throws Exception {
            when(orderService.findById(1L)).thenReturn(Optional.of(testOrder));
            when(orderService.isLegitimateOrder(any(), any(), any())).thenReturn(true);

            mockMvc.perform(post("/order/delete/1")
                            .with(user(librarianDetails))
                            .with(csrf()))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/uebersicht"));

            verify(orderService).delete(1L);
        }
    }

    @Nested
    @DisplayName("Security Tests")
    class SecurityTests {

        @Test
        @DisplayName("All order endpoints require authentication")
        void testEndpointsRequireAuth() throws Exception {
            mockMvc.perform(get("/order/new")).andExpect(status().is3xxRedirection());
            mockMvc.perform(get("/order/edit/1")).andExpect(status().is3xxRedirection());
        }

        @Test
        @Disabled("Requires full Thymeleaf templates - run with @SpringBootTest for integration")
        @DisplayName("Admin should have access to order management")
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

            when(orderService.findById(1L)).thenReturn(Optional.of(testOrder));
            when(orderService.isLegitimateOrder(any(), any(), any())).thenReturn(true);
            when(lieferantRepository.findByAllgemeinTrueOrKontoId(anyLong()))
                    .thenReturn(Collections.emptyList());
            when(userService.findByKontoId(anyLong())).thenReturn(Arrays.asList(testUser));

            mockMvc.perform(get("/order/edit/1")
                            .with(user(adminDetails)))
                    .andExpect(status().isOk());
        }

        @Test
        @Disabled("Requires full Thymeleaf templates - run with @SpringBootTest for integration")
        @DisplayName("Librarian should have elevated privileges")
        void testLibrarianAccess() throws Exception {
            when(orderService.findById(1L)).thenReturn(Optional.of(testOrder));
            when(orderService.isLegitimateOrder(any(), any(), any())).thenReturn(true);
            when(lieferantRepository.findByAllgemeinTrueOrKontoId(anyLong()))
                    .thenReturn(Collections.emptyList());
            when(userService.findByKontoId(anyLong())).thenReturn(Arrays.asList(testUser));

            mockMvc.perform(get("/order/edit/1")
                            .with(user(librarianDetails)))
                    .andExpect(status().isOk());
        }
    }
}
