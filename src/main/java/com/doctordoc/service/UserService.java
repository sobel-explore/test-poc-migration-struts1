package com.doctordoc.service;

import com.doctordoc.entity.Benutzer;
import com.doctordoc.entity.Konto;
import com.doctordoc.repository.BenutzerRepository;
import com.doctordoc.repository.KontoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service for user-related operations.
 * Extracts business logic from LoginAction, UserAction.
 */
@Service
@Transactional
public class UserService {

    private static final Logger LOG = LoggerFactory.getLogger(UserService.class);

    private final BenutzerRepository benutzerRepository;
    private final KontoRepository kontoRepository;

    public UserService(BenutzerRepository benutzerRepository, KontoRepository kontoRepository) {
        this.benutzerRepository = benutzerRepository;
        this.kontoRepository = kontoRepository;
    }

    public Optional<Benutzer> findById(Long id) {
        return benutzerRepository.findById(id);
    }

    public Optional<Benutzer> findByEmail(String email) {
        return benutzerRepository.findByEmail(email);
    }

    public List<Benutzer> findByKontoId(Long kontoId) {
        return benutzerRepository.findByKontoId(kontoId);
    }

    public List<Benutzer> findAll() {
        return benutzerRepository.findAll();
    }

    public Benutzer save(Benutzer benutzer) {
        if (benutzer.getDatum() == null) {
            benutzer.setDatum(LocalDateTime.now());
        }
        return benutzerRepository.save(benutzer);
    }

    public void delete(Long id) {
        benutzerRepository.deleteById(id);
    }

    public void updateLastUse(Benutzer benutzer) {
        benutzer.setLastuse(LocalDateTime.now());
        benutzerRepository.save(benutzer);
    }

    /**
     * Get all accounts (Kontos) the user is allowed to login to.
     */
    public List<Konto> getAllowedKontos(Benutzer benutzer) {
        Set<Konto> userKontos = benutzer.getKontos();

        if (benutzer.isAdmin()) {
            return kontoRepository.findAllByOrderByKontotypDescBibliotheksnameAsc();
        }

        return userKontos.stream()
                .filter(k -> k.isKontostatus())
                .filter(k -> benutzer.isLibrarian() || benutzer.isAdmin() ||
                        (benutzer.isLoginopt() && k.isUserlogin()))
                .collect(Collectors.toList());
    }

    /**
     * Check if a user is allowed to place orders at Subito.
     */
    public boolean canOrderSubito(Benutzer benutzer, Konto konto) {
        if (benutzer.isAdmin() || benutzer.isLibrarian()) {
            return true;
        }
        return benutzer.isUserbestellung() && konto.isUserbestellung();
    }

    /**
     * Check if a user is allowed to place orders at GBV.
     */
    public boolean canOrderGBV(Benutzer benutzer, Konto konto) {
        if (benutzer.isAdmin() || benutzer.isLibrarian()) {
            return true;
        }
        return benutzer.isGbvbestellung() && konto.isGbvbestellung()
                && konto.getGbvrequesterid() != null
                && konto.getIsil() != null;
    }

    /**
     * Check if a user account is active.
     */
    public boolean isUserAccountActive(Benutzer benutzer, Konto konto) {
        if (benutzer.isAdmin() || benutzer.isLibrarian()) {
            return true;
        }
        return benutzer.isKontostatus() && konto.isKontostatus();
    }

    /**
     * Reset all user categories to 0 for a given category ID.
     */
    public void resetCategories(Long categoryId) {
        List<Benutzer> users = benutzerRepository.findAll();
        users.stream()
                .filter(b -> categoryId.equals(b.getCategoryId()))
                .forEach(b -> {
                    b.setCategoryId(0L);
                    benutzerRepository.save(b);
                });
    }
}
