package com.doctordoc.security;

import com.doctordoc.entity.Benutzer;
import com.doctordoc.entity.Konto;
import com.doctordoc.repository.BenutzerRepository;
import com.doctordoc.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Custom UserDetailsService for Spring Security authentication.
 * Integrates with the Benutzer entity and Konto selection logic.
 */
@Service
public class DoctorDocUserDetailsService implements UserDetailsService {

    private static final Logger LOG = LoggerFactory.getLogger(DoctorDocUserDetailsService.class);

    private final BenutzerRepository benutzerRepository;
    private final UserService userService;

    public DoctorDocUserDetailsService(BenutzerRepository benutzerRepository, UserService userService) {
        this.benutzerRepository = benutzerRepository;
        this.userService = userService;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        LOG.debug("Authenticating user: {}", email);

        Benutzer benutzer = benutzerRepository.findByEmailAndLoginoptTrue(email)
                .orElseThrow(() -> {
                    LOG.warn("User not found or not allowed to login: {}", email);
                    return new UsernameNotFoundException("User not found: " + email);
                });

        DoctorDocUserDetails userDetails = new DoctorDocUserDetails(benutzer);

        // Load available accounts
        List<Konto> kontos = userService.getAllowedKontos(benutzer);
        userDetails.setAvailableKontos(kontos);

        // Auto-select account if only one is available
        if (kontos.size() == 1) {
            userDetails.setActiveKonto(kontos.get(0));
        }

        return userDetails;
    }

    /**
     * Load user details with password verification (used during authentication).
     */
    @Transactional(readOnly = true)
    public DoctorDocUserDetails loadUserByEmailAndPassword(String email, String hashedPassword) {
        Benutzer benutzer = benutzerRepository.findByEmailAndPassword(email, hashedPassword)
                .orElseThrow(() -> new UsernameNotFoundException("Invalid credentials"));

        if (!benutzer.isLoginopt()) {
            throw new UsernameNotFoundException("User not allowed to login");
        }

        DoctorDocUserDetails userDetails = new DoctorDocUserDetails(benutzer);
        List<Konto> kontos = userService.getAllowedKontos(benutzer);
        userDetails.setAvailableKontos(kontos);

        if (kontos.size() == 1) {
            userDetails.setActiveKonto(kontos.get(0));
        }

        return userDetails;
    }
}
