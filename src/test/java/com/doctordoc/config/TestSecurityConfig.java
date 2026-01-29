//  Copyright (C) 2005 - 2010  Markus Fischer, Pascal Steiner
//
//  This program is free software; you can redistribute it and/or
//  modify it under the terms of the GNU General Public License
//  as published by the Free Software Foundation; version 2 of the License.

package com.doctordoc.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Test security configuration for controller tests.
 * Provides a simplified security configuration for @WebMvcTest tests.
 */
@TestConfiguration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class TestSecurityConfig {

    @Bean
    @Order(1)
    public SecurityFilterChain testFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                // Public resources
                .requestMatchers("/", "/login", "/register/**").permitAll()
                .requestMatchers("/css/**", "/js/**", "/img/**", "/images/**", "/webjars/**").permitAll()
                .requestMatchers("/error", "/favicon.ico").permitAll()

                // Admin only
                .requestMatchers("/admin", "/admin/**").hasRole("ADMIN")
                .requestMatchers("/konto/list", "/konto/add/**", "/konto/delete/**").hasRole("ADMIN")

                // Librarian and Admin
                .requestMatchers("/konto/modify/**").hasAnyRole("LIBRARIAN", "ADMIN")

                // All other requests require authentication
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .permitAll()
            )
            .csrf(csrf -> csrf.disable()); // Disable CSRF for tests

        return http.build();
    }
}
