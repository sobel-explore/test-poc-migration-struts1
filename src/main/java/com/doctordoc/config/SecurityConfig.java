package com.doctordoc.config;

import com.doctordoc.security.DoctorDocUserDetailsService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

/**
 * Spring Security configuration for Doctor-Doc.
 * Replaces the Auth.java authorization logic from Struts.
 *
 * Role mapping:
 * - Auth.isAdmin() -> hasRole('ADMIN')
 * - Auth.isBibliothekar() -> hasRole('LIBRARIAN')
 * - Auth.isBenutzer() -> hasRole('USER')
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    private final DoctorDocUserDetailsService userDetailsService;

    public SecurityConfig(DoctorDocUserDetailsService userDetailsService) {
        this.userDetailsService = userDetailsService;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                // Public resources - static assets
                .requestMatchers("/", "/login", "/register/**").permitAll()
                .requestMatchers("/css/**", "/js/**", "/img/**", "/images/**", "/webjars/**").permitAll()
                .requestMatchers("/error", "/favicon.ico").permitAll()

                // Language switching
                .requestMatchers("/language").permitAll()

                // Public pages (no auth required)
                .requestMatchers("/services", "/impressum", "/howto").permitAll()

                // API endpoints (OpenURL, DAIA, KBART)
                .requestMatchers("/openurl/**", "/daia/**", "/kbart/**").permitAll()

                // Admin only - both /admin and /admin/** patterns
                .requestMatchers("/admin", "/admin/**").hasRole("ADMIN")

                // Librarian and Admin
                .requestMatchers("/konto/modify/**").hasAnyRole("LIBRARIAN", "ADMIN")
                .requestMatchers("/user/add/**", "/user/modify/**").hasAnyRole("LIBRARIAN", "ADMIN")
                .requestMatchers("/order/status/**").hasAnyRole("LIBRARIAN", "ADMIN")
                .requestMatchers("/supplier/**").hasAnyRole("LIBRARIAN", "ADMIN")
                .requestMatchers("/billing/**").hasAnyRole("LIBRARIAN", "ADMIN")
                .requestMatchers("/reports/**").hasAnyRole("LIBRARIAN", "ADMIN")

                // User order permissions (checked further in controller)
                .requestMatchers("/order/new/**").authenticated()

                // All other requests require authentication
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/logincheck")
                .defaultSuccessUrl("/uebersicht", true)
                .failureUrl("/login?error=true")
                .usernameParameter("email")
                .passwordParameter("password")
                .permitAll()
            )
            .logout(logout -> logout
                .logoutRequestMatcher(new AntPathRequestMatcher("/logout"))
                .logoutSuccessUrl("/login?logout=true")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
            )
            .sessionManagement(session -> session
                .maximumSessions(1)
                .expiredUrl("/login?expired=true")
            )
            .exceptionHandling(ex -> ex
                .accessDeniedPage("/error/403")
            );

        return http.build();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * Custom password encoder that uses SHA-1 hashing (matching legacy system).
     * Note: For new installations, consider migrating to BCrypt.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new LegacySha1PasswordEncoder();
    }

    /**
     * Legacy SHA-1 password encoder to match existing password hashes.
     * The original system used Encrypt.makeSHA() which produces SHA-1.
     */
    private static class LegacySha1PasswordEncoder implements PasswordEncoder {

        @Override
        public String encode(CharSequence rawPassword) {
            try {
                java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-1");
                byte[] digest = md.digest(rawPassword.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
                StringBuilder sb = new StringBuilder();
                for (byte b : digest) {
                    sb.append(String.format("%02x", b));
                }
                return sb.toString();
            } catch (java.security.NoSuchAlgorithmException e) {
                throw new RuntimeException("SHA-1 not available", e);
            }
        }

        @Override
        public boolean matches(CharSequence rawPassword, String encodedPassword) {
            return encode(rawPassword).equals(encodedPassword);
        }
    }
}
