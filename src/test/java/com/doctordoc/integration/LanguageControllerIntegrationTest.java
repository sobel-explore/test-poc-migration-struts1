//  Copyright (C) 2005 - 2010  Markus Fischer, Pascal Steiner
//
//  This program is free software; you can redistribute it and/or
//  modify it under the terms of the GNU General Public License
//  as published by the Free Software Foundation; version 2 of the License.

package com.doctordoc.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Integration tests for LanguageController.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class LanguageControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Nested
    @DisplayName("Language Change Tests")
    class LanguageChangeTests {

        @Test
        @DisplayName("Should change to German and redirect to home")
        void testChangeToGerman() throws Exception {
            mockMvc.perform(get("/language")
                            .param("lang", "de"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/"));
        }

        @Test
        @DisplayName("Should change to French and redirect to home")
        void testChangeToFrench() throws Exception {
            mockMvc.perform(get("/language")
                            .param("lang", "fr"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/"));
        }

        @Test
        @DisplayName("Should change to English and redirect to home")
        void testChangeToEnglish() throws Exception {
            mockMvc.perform(get("/language")
                            .param("lang", "en"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/"));
        }

        @Test
        @DisplayName("Should default to English for unsupported language")
        void testUnsupportedLanguage() throws Exception {
            mockMvc.perform(get("/language")
                            .param("lang", "es"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/"));
        }

        @Test
        @DisplayName("Should default to English when no lang parameter")
        void testNoLangParameter() throws Exception {
            mockMvc.perform(get("/language"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/"));
        }

        @Test
        @DisplayName("Should redirect to referer when valid")
        void testRedirectToReferer() throws Exception {
            mockMvc.perform(get("/language")
                            .param("lang", "de")
                            .header("Referer", "http://localhost/uebersicht"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("http://localhost/uebersicht"));
        }

        @Test
        @DisplayName("Should redirect to home for invalid referer")
        void testInvalidReferer() throws Exception {
            mockMvc.perform(get("/language")
                            .param("lang", "de")
                            .header("Referer", "http://malicious.com/attack"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/"));
        }

        @Test
        @DisplayName("Should redirect to home for empty referer")
        void testEmptyReferer() throws Exception {
            mockMvc.perform(get("/language")
                            .param("lang", "de")
                            .header("Referer", ""))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/"));
        }
    }
}
