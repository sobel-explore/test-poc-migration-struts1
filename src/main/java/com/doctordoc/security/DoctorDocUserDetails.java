package com.doctordoc.security;

import com.doctordoc.entity.Benutzer;
import com.doctordoc.entity.Konto;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Custom UserDetails implementation for Doctor-Doc.
 * Wraps the Benutzer entity and provides Spring Security integration.
 */
public class DoctorDocUserDetails implements UserDetails {

    private final Benutzer benutzer;
    private Konto activeKonto;
    private List<Konto> availableKontos;

    public DoctorDocUserDetails(Benutzer benutzer) {
        this.benutzer = benutzer;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        String role = switch (benutzer.getRechte()) {
            case Benutzer.ROLE_ADMIN -> "ROLE_ADMIN";
            case Benutzer.ROLE_LIBRARIAN -> "ROLE_LIBRARIAN";
            default -> "ROLE_USER";
        };
        return Collections.singleton(new SimpleGrantedAuthority(role));
    }

    @Override
    public String getPassword() {
        return benutzer.getPassword();
    }

    @Override
    public String getUsername() {
        return benutzer.getEmail();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return benutzer.isKontostatus();
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return benutzer.isKontostatus() && benutzer.isLoginopt();
    }

    // Convenience methods

    public Benutzer getBenutzer() {
        return benutzer;
    }

    public Long getUserId() {
        return benutzer.getId();
    }

    public String getFullName() {
        return benutzer.getFullName();
    }

    public Konto getActiveKonto() {
        return activeKonto;
    }

    public void setActiveKonto(Konto activeKonto) {
        this.activeKonto = activeKonto;
    }

    public List<Konto> getAvailableKontos() {
        return availableKontos;
    }

    public void setAvailableKontos(List<Konto> availableKontos) {
        this.availableKontos = availableKontos;
    }

    public boolean isAdmin() {
        return benutzer.isAdmin();
    }

    public boolean isLibrarian() {
        return benutzer.isLibrarian();
    }

    public boolean isUser() {
        return benutzer.isUser();
    }

    public boolean hasMultipleKontos() {
        return availableKontos != null && availableKontos.size() > 1;
    }
}
