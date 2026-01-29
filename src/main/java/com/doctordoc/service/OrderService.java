package com.doctordoc.service;

import com.doctordoc.config.DoctorDocProperties;
import com.doctordoc.entity.Benutzer;
import com.doctordoc.entity.Bestellungen;
import com.doctordoc.entity.Konto;
import com.doctordoc.repository.BestellungenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Service for order (Bestellungen) operations.
 * Extracts business logic from OrderAction, BestellformAction.
 */
@Service
@Transactional
public class OrderService {

    private static final Logger LOG = LoggerFactory.getLogger(OrderService.class);

    private final BestellungenRepository bestellungenRepository;
    private final DoctorDocProperties properties;

    public OrderService(BestellungenRepository bestellungenRepository,
                        DoctorDocProperties properties) {
        this.bestellungenRepository = bestellungenRepository;
        this.properties = properties;
    }

    public Optional<Bestellungen> findById(Long id) {
        return bestellungenRepository.findById(id);
    }

    public List<Bestellungen> findByKontoId(Long kontoId) {
        return bestellungenRepository.findByKontoId(kontoId);
    }

    public Page<Bestellungen> findByKontoIdPaged(Long kontoId, Pageable pageable) {
        return bestellungenRepository.findByKontoIdOrderByOrderdateDesc(kontoId, pageable);
    }

    public List<Bestellungen> findByBenutzerId(Long benutzerId) {
        return bestellungenRepository.findByBenutzerId(benutzerId);
    }

    public Page<Bestellungen> findByKontoIdAndState(Long kontoId, String state, Pageable pageable) {
        return bestellungenRepository.findByKontoIdAndState(kontoId, state, pageable);
    }

    public Page<Bestellungen> searchOrders(Long kontoId, String search, Pageable pageable) {
        return bestellungenRepository.searchOrders(kontoId, search, pageable);
    }

    public Bestellungen save(Bestellungen order) {
        if (order.getOrderdate() == null) {
            order.setOrderdate(LocalDateTime.now());
        }
        if (order.getStatedate() == null) {
            order.setStatedate(LocalDateTime.now());
        }
        return bestellungenRepository.save(order);
    }

    public void delete(Long id) {
        bestellungenRepository.deleteById(id);
    }

    public void updateStatus(Long orderId, String state) {
        bestellungenRepository.findById(orderId).ifPresent(order -> {
            order.setState(state);
            order.setStatedate(LocalDateTime.now());
            if ("closed".equalsIgnoreCase(state)) {
                order.setErledigt(true);
            }
            bestellungenRepository.save(order);
        });
    }

    /**
     * Check if user is allowed to view/modify this order.
     */
    public boolean isLegitimateOrder(Benutzer benutzer, Konto konto, Bestellungen order) {
        if (order == null || order.getId() == null) {
            return false;
        }

        // Librarians and admins can see orders of their account
        if (benutzer.isAdmin() || benutzer.isLibrarian()) {
            return order.getKontoId().equals(konto.getId());
        }

        // Regular users can only see their own orders
        return order.getBenutzerId().equals(benutzer.getId());
    }

    /**
     * Check order limits for account.
     */
    public boolean checkMaxOrdersKonto(Konto konto) {
        if (konto.getOrderlimits() == 0 || konto.getMaxordersj() == null || konto.getMaxordersj() == 0) {
            return true;
        }
        long orderCount = bestellungenRepository.countOrdersThisYearForKonto(konto.getId());
        return orderCount < konto.getMaxordersj();
    }

    /**
     * Check order limits per user per year.
     */
    public boolean checkMaxOrdersUser(Benutzer benutzer, Konto konto) {
        if (konto.getOrderlimits() == 0 || konto.getMaxordersutotal() == null || konto.getMaxordersutotal() == 0) {
            return true;
        }
        long orderCount = bestellungenRepository.countOrdersPerUserThisYear(benutzer.getId(), konto.getId());
        return orderCount < konto.getMaxordersutotal();
    }

    /**
     * Check if order should be anonymized based on settings.
     */
    public boolean shouldAnonymize(Bestellungen order) {
        if (!properties.getAnonymization().isEnabled()) {
            return false;
        }
        if (order.getOrderdate() == null) {
            return false;
        }

        LocalDate orderDate = order.getOrderdate().toLocalDate();
        LocalDate anonymizeDate = LocalDate.now().minusMonths(properties.getAnonymization().getAfterMonths());
        return orderDate.isBefore(anonymizeDate);
    }
}
