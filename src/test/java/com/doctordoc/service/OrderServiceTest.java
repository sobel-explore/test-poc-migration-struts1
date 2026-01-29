//  Copyright (C) 2005 - 2010  Markus Fischer, Pascal Steiner
//
//  This program is free software; you can redistribute it and/or
//  modify it under the terms of the GNU General Public License
//  as published by the Free Software Foundation; version 2 of the License.

package com.doctordoc.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.doctordoc.config.DoctorDocProperties;
import com.doctordoc.entity.Benutzer;
import com.doctordoc.entity.Bestellungen;
import com.doctordoc.entity.Konto;
import com.doctordoc.repository.BestellungenRepository;

/**
 * Unit tests for OrderService.
 */
@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private BestellungenRepository bestellungenRepository;

    @Mock
    private DoctorDocProperties properties;

    @InjectMocks
    private OrderService orderService;

    private Bestellungen testOrder;
    private Benutzer testUser;
    private Benutzer testLibrarian;
    private Benutzer testAdmin;
    private Konto testKonto;

    @BeforeEach
    void setUp() {
        // Setup test order
        testOrder = new Bestellungen();
        testOrder.setId(1L);
        testOrder.setKontoId(100L);
        testOrder.setBenutzerId(1L);
        testOrder.setLieferantId(1L);
        testOrder.setMediatype("Artikel");
        testOrder.setArtikeltitel("Test Article");
        testOrder.setState("open");

        // Setup test user
        testUser = new Benutzer();
        testUser.setId(1L);
        testUser.setRechte(Benutzer.ROLE_USER);

        // Setup test librarian
        testLibrarian = new Benutzer();
        testLibrarian.setId(2L);
        testLibrarian.setRechte(Benutzer.ROLE_LIBRARIAN);

        // Setup test admin
        testAdmin = new Benutzer();
        testAdmin.setId(3L);
        testAdmin.setRechte(Benutzer.ROLE_ADMIN);

        // Setup test konto
        testKonto = new Konto();
        testKonto.setId(100L);
        testKonto.setOrderlimits(1);
        testKonto.setMaxordersj(50L);
        testKonto.setMaxordersutotal(10L);
    }

    @Nested
    @DisplayName("Find Operations")
    class FindOperations {

        @Test
        @DisplayName("Should find order by ID")
        void testFindById() {
            when(bestellungenRepository.findById(1L)).thenReturn(Optional.of(testOrder));

            Optional<Bestellungen> result = orderService.findById(1L);

            assertTrue(result.isPresent());
            assertEquals("Test Article", result.get().getArtikeltitel());
        }

        @Test
        @DisplayName("Should find orders by konto ID")
        void testFindByKontoId() {
            when(bestellungenRepository.findByKontoId(100L)).thenReturn(Arrays.asList(testOrder));

            List<Bestellungen> result = orderService.findByKontoId(100L);

            assertEquals(1, result.size());
        }

        @Test
        @DisplayName("Should find orders by konto ID with paging")
        void testFindByKontoIdPaged() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Bestellungen> page = new PageImpl<>(Arrays.asList(testOrder));
            when(bestellungenRepository.findByKontoIdOrderByOrderdateDesc(100L, pageable)).thenReturn(page);

            Page<Bestellungen> result = orderService.findByKontoIdPaged(100L, pageable);

            assertEquals(1, result.getTotalElements());
        }

        @Test
        @DisplayName("Should find orders by benutzer ID")
        void testFindByBenutzerId() {
            when(bestellungenRepository.findByBenutzerId(1L)).thenReturn(Arrays.asList(testOrder));

            List<Bestellungen> result = orderService.findByBenutzerId(1L);

            assertEquals(1, result.size());
        }

        @Test
        @DisplayName("Should find orders by konto ID and state")
        void testFindByKontoIdAndState() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Bestellungen> page = new PageImpl<>(Arrays.asList(testOrder));
            when(bestellungenRepository.findByKontoIdAndState(100L, "open", pageable)).thenReturn(page);

            Page<Bestellungen> result = orderService.findByKontoIdAndState(100L, "open", pageable);

            assertEquals(1, result.getTotalElements());
        }

        @Test
        @DisplayName("Should search orders")
        void testSearchOrders() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Bestellungen> page = new PageImpl<>(Arrays.asList(testOrder));
            when(bestellungenRepository.searchOrders(100L, "Test", pageable)).thenReturn(page);

            Page<Bestellungen> result = orderService.searchOrders(100L, "Test", pageable);

            assertEquals(1, result.getTotalElements());
        }
    }

    @Nested
    @DisplayName("Save and Delete Operations")
    class SaveDeleteOperations {

        @Test
        @DisplayName("Should save order with dates set")
        void testSaveNewOrder() {
            Bestellungen newOrder = new Bestellungen();
            newOrder.setArtikeltitel("New Article");

            when(bestellungenRepository.save(any(Bestellungen.class))).thenAnswer(invocation -> {
                Bestellungen saved = invocation.getArgument(0);
                saved.setId(1L);
                return saved;
            });

            Bestellungen result = orderService.save(newOrder);

            assertNotNull(result.getOrderdate());
            assertNotNull(result.getStatedate());
            verify(bestellungenRepository).save(newOrder);
        }

        @Test
        @DisplayName("Should not override existing orderdate")
        void testSaveExistingOrderWithDate() {
            LocalDateTime existingDate = LocalDateTime.of(2020, 1, 1, 12, 0);
            testOrder.setOrderdate(existingDate);

            when(bestellungenRepository.save(testOrder)).thenReturn(testOrder);

            Bestellungen result = orderService.save(testOrder);

            assertEquals(existingDate, result.getOrderdate());
        }

        @Test
        @DisplayName("Should delete order by ID")
        void testDelete() {
            orderService.delete(1L);

            verify(bestellungenRepository).deleteById(1L);
        }
    }

    @Nested
    @DisplayName("Update Status")
    class UpdateStatus {

        @Test
        @DisplayName("Should update order status")
        void testUpdateStatus() {
            when(bestellungenRepository.findById(1L)).thenReturn(Optional.of(testOrder));
            when(bestellungenRepository.save(any(Bestellungen.class))).thenReturn(testOrder);

            orderService.updateStatus(1L, "processing");

            assertEquals("processing", testOrder.getState());
            assertNotNull(testOrder.getStatedate());
            verify(bestellungenRepository).save(testOrder);
        }

        @Test
        @DisplayName("Should set erledigt when status is closed")
        void testUpdateStatusClosed() {
            when(bestellungenRepository.findById(1L)).thenReturn(Optional.of(testOrder));
            when(bestellungenRepository.save(any(Bestellungen.class))).thenReturn(testOrder);

            orderService.updateStatus(1L, "closed");

            assertEquals("closed", testOrder.getState());
            assertTrue(testOrder.getErledigt());
        }

        @Test
        @DisplayName("Should do nothing if order not found")
        void testUpdateStatusOrderNotFound() {
            when(bestellungenRepository.findById(1L)).thenReturn(Optional.empty());

            orderService.updateStatus(1L, "processing");

            verify(bestellungenRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Legitimate Order Checks")
    class LegitimateOrderChecks {

        @Test
        @DisplayName("Should return false for null order")
        void testIsLegitimateOrderNullOrder() {
            assertFalse(orderService.isLegitimateOrder(testUser, testKonto, null));
        }

        @Test
        @DisplayName("Should return false for order without ID")
        void testIsLegitimateOrderNoId() {
            testOrder.setId(null);
            assertFalse(orderService.isLegitimateOrder(testUser, testKonto, testOrder));
        }

        @Test
        @DisplayName("Admin can view orders of their konto")
        void testIsLegitimateOrderAdmin() {
            assertTrue(orderService.isLegitimateOrder(testAdmin, testKonto, testOrder));
        }

        @Test
        @DisplayName("Librarian can view orders of their konto")
        void testIsLegitimateOrderLibrarian() {
            assertTrue(orderService.isLegitimateOrder(testLibrarian, testKonto, testOrder));
        }

        @Test
        @DisplayName("User can only view their own orders")
        void testIsLegitimateOrderUser() {
            assertTrue(orderService.isLegitimateOrder(testUser, testKonto, testOrder));
        }

        @Test
        @DisplayName("User cannot view other users' orders")
        void testIsLegitimateOrderOtherUser() {
            Benutzer otherUser = new Benutzer();
            otherUser.setId(999L);
            otherUser.setRechte(Benutzer.ROLE_USER);

            assertFalse(orderService.isLegitimateOrder(otherUser, testKonto, testOrder));
        }

        @Test
        @DisplayName("Librarian cannot view orders of different konto")
        void testIsLegitimateOrderLibrarianDifferentKonto() {
            Konto differentKonto = new Konto();
            differentKonto.setId(200L);

            assertFalse(orderService.isLegitimateOrder(testLibrarian, differentKonto, testOrder));
        }
    }

    @Nested
    @DisplayName("Order Limit Checks")
    class OrderLimitChecks {

        @Test
        @DisplayName("Should allow if no order limits set")
        void testCheckMaxOrdersKontoNoLimits() {
            testKonto.setOrderlimits(0);

            assertTrue(orderService.checkMaxOrdersKonto(testKonto));
        }

        @Test
        @DisplayName("Should allow if max orders is null")
        void testCheckMaxOrdersKontoMaxNull() {
            testKonto.setMaxordersj(null);

            assertTrue(orderService.checkMaxOrdersKonto(testKonto));
        }

        @Test
        @DisplayName("Should allow if under limit")
        void testCheckMaxOrdersKontoUnderLimit() {
            when(bestellungenRepository.countOrdersThisYearForKonto(100L)).thenReturn(25L);

            assertTrue(orderService.checkMaxOrdersKonto(testKonto));
        }

        @Test
        @DisplayName("Should reject if at limit")
        void testCheckMaxOrdersKontoAtLimit() {
            when(bestellungenRepository.countOrdersThisYearForKonto(100L)).thenReturn(50L);

            assertFalse(orderService.checkMaxOrdersKonto(testKonto));
        }

        @Test
        @DisplayName("Should allow user if no limits set")
        void testCheckMaxOrdersUserNoLimits() {
            testKonto.setOrderlimits(0);

            assertTrue(orderService.checkMaxOrdersUser(testUser, testKonto));
        }

        @Test
        @DisplayName("Should allow user if under limit")
        void testCheckMaxOrdersUserUnderLimit() {
            when(bestellungenRepository.countOrdersPerUserThisYear(1L, 100L)).thenReturn(5L);

            assertTrue(orderService.checkMaxOrdersUser(testUser, testKonto));
        }

        @Test
        @DisplayName("Should reject user if at limit")
        void testCheckMaxOrdersUserAtLimit() {
            when(bestellungenRepository.countOrdersPerUserThisYear(1L, 100L)).thenReturn(10L);

            assertFalse(orderService.checkMaxOrdersUser(testUser, testKonto));
        }
    }

    @Nested
    @DisplayName("Anonymization Checks")
    class AnonymizationChecks {

        @Test
        @DisplayName("Should not anonymize if disabled")
        void testShouldAnonymizeDisabled() {
            DoctorDocProperties.Anonymization anonymization = new DoctorDocProperties.Anonymization();
            anonymization.setEnabled(false);
            when(properties.getAnonymization()).thenReturn(anonymization);

            assertFalse(orderService.shouldAnonymize(testOrder));
        }

        @Test
        @DisplayName("Should not anonymize if no order date")
        void testShouldAnonymizeNoDate() {
            DoctorDocProperties.Anonymization anonymization = new DoctorDocProperties.Anonymization();
            anonymization.setEnabled(true);
            when(properties.getAnonymization()).thenReturn(anonymization);
            testOrder.setOrderdate(null);

            assertFalse(orderService.shouldAnonymize(testOrder));
        }

        @Test
        @DisplayName("Should anonymize old orders")
        void testShouldAnonymizeOldOrder() {
            DoctorDocProperties.Anonymization anonymization = new DoctorDocProperties.Anonymization();
            anonymization.setEnabled(true);
            anonymization.setAfterMonths(12);
            when(properties.getAnonymization()).thenReturn(anonymization);
            testOrder.setOrderdate(LocalDateTime.now().minusMonths(15));

            assertTrue(orderService.shouldAnonymize(testOrder));
        }

        @Test
        @DisplayName("Should not anonymize recent orders")
        void testShouldNotAnonymizeRecentOrder() {
            DoctorDocProperties.Anonymization anonymization = new DoctorDocProperties.Anonymization();
            anonymization.setEnabled(true);
            anonymization.setAfterMonths(12);
            when(properties.getAnonymization()).thenReturn(anonymization);
            testOrder.setOrderdate(LocalDateTime.now().minusMonths(6));

            assertFalse(orderService.shouldAnonymize(testOrder));
        }
    }
}
