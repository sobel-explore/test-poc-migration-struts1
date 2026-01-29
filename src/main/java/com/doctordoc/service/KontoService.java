package com.doctordoc.service;

import com.doctordoc.entity.Konto;
import com.doctordoc.repository.KontoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Service for Konto (library account) operations.
 * Extracts business logic from KontoAction.
 */
@Service
@Transactional
public class KontoService {

    private static final Logger LOG = LoggerFactory.getLogger(KontoService.class);

    private final KontoRepository kontoRepository;

    public KontoService(KontoRepository kontoRepository) {
        this.kontoRepository = kontoRepository;
    }

    public Optional<Konto> findById(Long id) {
        return kontoRepository.findById(id);
    }

    public List<Konto> findAll() {
        return kontoRepository.findAllByOrderByKontotypDescBibliotheksnameAsc();
    }

    public List<Konto> findAdvancedKontos() {
        return kontoRepository.findByKontotypGreaterThanOrderByKontotypDescBibliotheksnameAsc(0);
    }

    public List<Konto> findFaxserverKontos() {
        return kontoRepository.findByFaxnoIsNotNull();
    }

    public List<Konto> findExpireKontos(int expireDays) {
        LocalDate expireDate = LocalDate.now().plusDays(expireDays);
        return kontoRepository.findExpireKontos(expireDate);
    }

    public Konto save(Konto konto) {
        if (konto.getEdatum() == null) {
            konto.setEdatum(LocalDate.now());
        }
        return kontoRepository.save(konto);
    }

    public void delete(Long id) {
        kontoRepository.deleteById(id);
    }

    /**
     * Check if the account is a paid account or within trial period.
     */
    public boolean isPaidAccount(Konto konto) {
        if (konto.getKontotyp() > 0) {
            return true;
        }
        // Allow 30 days for trial
        if (konto.getEdatum() != null) {
            long diffInDays = java.time.temporal.ChronoUnit.DAYS.between(
                    konto.getEdatum(), LocalDate.now());
            return diffInDays <= 30;
        }
        return false;
    }

    /**
     * Check if the account has an active fax server.
     */
    public boolean hasFaxServer(Konto konto) {
        return konto.getFaxno() != null && !konto.getFaxno().isEmpty();
    }

    /**
     * Get account type description.
     */
    public String getKontotypDescription(Konto konto) {
        return switch (konto.getKontotyp()) {
            case 0 -> "Free";
            case 1 -> "Premium";
            case 2 -> "Fax 1 Year";
            case 3 -> "Fax 3 Months";
            default -> "Unknown";
        };
    }
}
