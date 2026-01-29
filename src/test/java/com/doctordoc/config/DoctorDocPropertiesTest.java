//  Copyright (C) 2005 - 2010  Markus Fischer, Pascal Steiner
//
//  This program is free software; you can redistribute it and/or
//  modify it under the terms of the GNU General Public License
//  as published by the Free Software Foundation; version 2 of the License.

package com.doctordoc.config;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for DoctorDocProperties configuration class.
 */
class DoctorDocPropertiesTest {

    @Nested
    @DisplayName("Default Values")
    class DefaultValues {

        @Test
        @DisplayName("Should have default application name")
        void testDefaultApplicationName() {
            DoctorDocProperties props = new DoctorDocProperties();

            assertEquals("Doctor-Doc", props.getApplicationName());
        }

        @Test
        @DisplayName("Should have default system timezone")
        void testDefaultSystemTimezone() {
            DoctorDocProperties props = new DoctorDocProperties();

            assertEquals("Europe/Berlin", props.getSystemTimezone());
        }

        @Test
        @DisplayName("Should have default locale")
        void testDefaultLocale() {
            DoctorDocProperties props = new DoctorDocProperties();

            assertEquals("de", props.getDefaultLocale());
        }

        @Test
        @DisplayName("Should have default max results display")
        void testDefaultMaxResultsDisplay() {
            DoctorDocProperties props = new DoctorDocProperties();

            assertEquals(200, props.getMaxResultsDisplay());
        }

        @Test
        @DisplayName("Should not allow library account registration by default")
        void testDefaultAllowRegisterLibraryAccounts() {
            DoctorDocProperties props = new DoctorDocProperties();

            assertFalse(props.isAllowRegisterLibraryAccounts());
        }

        @Test
        @DisplayName("Should have empty server installation by default")
        void testDefaultServerInstallation() {
            DoctorDocProperties props = new DoctorDocProperties();

            assertEquals("", props.getServerInstallation());
        }

        @Test
        @DisplayName("Should have empty welcome page by default")
        void testDefaultWelcomePage() {
            DoctorDocProperties props = new DoctorDocProperties();

            assertEquals("", props.getWelcomePage());
        }
    }

    @Nested
    @DisplayName("Setter Methods")
    class SetterMethods {

        @Test
        @DisplayName("Should set application name")
        void testSetApplicationName() {
            DoctorDocProperties props = new DoctorDocProperties();
            props.setApplicationName("Custom App");

            assertEquals("Custom App", props.getApplicationName());
        }

        @Test
        @DisplayName("Should set server installation")
        void testSetServerInstallation() {
            DoctorDocProperties props = new DoctorDocProperties();
            props.setServerInstallation("https://example.com");

            assertEquals("https://example.com", props.getServerInstallation());
        }

        @Test
        @DisplayName("Should set welcome page")
        void testSetWelcomePage() {
            DoctorDocProperties props = new DoctorDocProperties();
            props.setWelcomePage("/welcome");

            assertEquals("/welcome", props.getWelcomePage());
        }

        @Test
        @DisplayName("Should set system timezone")
        void testSetSystemTimezone() {
            DoctorDocProperties props = new DoctorDocProperties();
            props.setSystemTimezone("America/New_York");

            assertEquals("America/New_York", props.getSystemTimezone());
        }

        @Test
        @DisplayName("Should set default locale")
        void testSetDefaultLocale() {
            DoctorDocProperties props = new DoctorDocProperties();
            props.setDefaultLocale("en");

            assertEquals("en", props.getDefaultLocale());
        }

        @Test
        @DisplayName("Should set error email")
        void testSetErrorEmail() {
            DoctorDocProperties props = new DoctorDocProperties();
            props.setErrorEmail("error@example.com");

            assertEquals("error@example.com", props.getErrorEmail());
        }

        @Test
        @DisplayName("Should set billing email")
        void testSetBillingEmail() {
            DoctorDocProperties props = new DoctorDocProperties();
            props.setBillingEmail("billing@example.com");

            assertEquals("billing@example.com", props.getBillingEmail());
        }

        @Test
        @DisplayName("Should set allow register library accounts")
        void testSetAllowRegisterLibraryAccounts() {
            DoctorDocProperties props = new DoctorDocProperties();
            props.setAllowRegisterLibraryAccounts(true);

            assertTrue(props.isAllowRegisterLibraryAccounts());
        }

        @Test
        @DisplayName("Should set max results display")
        void testSetMaxResultsDisplay() {
            DoctorDocProperties props = new DoctorDocProperties();
            props.setMaxResultsDisplay(500);

            assertEquals(500, props.getMaxResultsDisplay());
        }
    }

    @Nested
    @DisplayName("System Email Configuration")
    class SystemEmailConfiguration {

        @Test
        @DisplayName("Should get system email with default values")
        void testDefaultSystemEmail() {
            DoctorDocProperties props = new DoctorDocProperties();
            DoctorDocProperties.SystemEmail email = props.getSystemEmail();

            assertNotNull(email);
            assertEquals("", email.getAddress());
            assertEquals("", email.getHost());
            assertEquals("", email.getAccountName());
        }

        @Test
        @DisplayName("Should set system email configuration")
        void testSetSystemEmail() {
            DoctorDocProperties props = new DoctorDocProperties();
            DoctorDocProperties.SystemEmail email = new DoctorDocProperties.SystemEmail();
            email.setAddress("system@example.com");
            email.setHost("smtp.example.com");
            email.setAccountName("system");
            props.setSystemEmail(email);

            assertEquals("system@example.com", props.getSystemEmail().getAddress());
            assertEquals("smtp.example.com", props.getSystemEmail().getHost());
            assertEquals("system", props.getSystemEmail().getAccountName());
        }
    }

    @Nested
    @DisplayName("GTC Configuration")
    class GtcConfiguration {

        @Test
        @DisplayName("Should have GTC disabled by default")
        void testDefaultGtc() {
            DoctorDocProperties props = new DoctorDocProperties();

            assertFalse(props.getGtc().isEnabled());
        }

        @Test
        @DisplayName("Should set GTC configuration")
        void testSetGtc() {
            DoctorDocProperties props = new DoctorDocProperties();
            DoctorDocProperties.Gtc gtc = new DoctorDocProperties.Gtc();
            gtc.setEnabled(true);
            props.setGtc(gtc);

            assertTrue(props.getGtc().isEnabled());
        }
    }

    @Nested
    @DisplayName("Paid Access Configuration")
    class PaidAccessConfiguration {

        @Test
        @DisplayName("Should have paid access disabled by default")
        void testDefaultPaidAccess() {
            DoctorDocProperties props = new DoctorDocProperties();

            assertFalse(props.getPaidAccess().isEnabled());
        }

        @Test
        @DisplayName("Should set paid access configuration")
        void testSetPaidAccess() {
            DoctorDocProperties props = new DoctorDocProperties();
            DoctorDocProperties.PaidAccess paidAccess = new DoctorDocProperties.PaidAccess();
            paidAccess.setEnabled(true);
            props.setPaidAccess(paidAccess);

            assertTrue(props.getPaidAccess().isEnabled());
        }
    }

    @Nested
    @DisplayName("Anonymization Configuration")
    class AnonymizationConfiguration {

        @Test
        @DisplayName("Should have anonymization enabled by default")
        void testDefaultAnonymization() {
            DoctorDocProperties props = new DoctorDocProperties();
            DoctorDocProperties.Anonymization anon = props.getAnonymization();

            assertTrue(anon.isEnabled());
            assertEquals(12, anon.getAfterMonths());
        }

        @Test
        @DisplayName("Should set anonymization configuration")
        void testSetAnonymization() {
            DoctorDocProperties props = new DoctorDocProperties();
            DoctorDocProperties.Anonymization anon = new DoctorDocProperties.Anonymization();
            anon.setEnabled(false);
            anon.setAfterMonths(24);
            props.setAnonymization(anon);

            assertFalse(props.getAnonymization().isEnabled());
            assertEquals(24, props.getAnonymization().getAfterMonths());
        }
    }

    @Nested
    @DisplayName("Carelit Configuration")
    class CarelitConfiguration {

        @Test
        @DisplayName("Should have Carelit search disabled by default")
        void testDefaultCarelit() {
            DoctorDocProperties props = new DoctorDocProperties();

            assertFalse(props.getCarelit().isSearchEnabled());
        }

        @Test
        @DisplayName("Should set Carelit configuration")
        void testSetCarelit() {
            DoctorDocProperties props = new DoctorDocProperties();
            DoctorDocProperties.Carelit carelit = new DoctorDocProperties.Carelit();
            carelit.setSearchEnabled(true);
            props.setCarelit(carelit);

            assertTrue(props.getCarelit().isSearchEnabled());
        }
    }

    @Nested
    @DisplayName("DAIA Configuration")
    class DaiaConfiguration {

        @Test
        @DisplayName("Should have DAIA disabled by default")
        void testDefaultDaia() {
            DoctorDocProperties props = new DoctorDocProperties();
            DoctorDocProperties.Daia daia = props.getDaia();

            assertFalse(daia.isEnabled());
            assertEquals("", daia.getHosts());
        }

        @Test
        @DisplayName("Should set DAIA configuration")
        void testSetDaia() {
            DoctorDocProperties props = new DoctorDocProperties();
            DoctorDocProperties.Daia daia = new DoctorDocProperties.Daia();
            daia.setEnabled(true);
            daia.setHosts("daia.example.com");
            props.setDaia(daia);

            assertTrue(props.getDaia().isEnabled());
            assertEquals("daia.example.com", props.getDaia().getHosts());
        }
    }

    @Nested
    @DisplayName("Seeks Configuration")
    class SeeksConfiguration {

        @Test
        @DisplayName("Should have empty Seeks domains by default")
        void testDefaultSeeks() {
            DoctorDocProperties props = new DoctorDocProperties();

            assertEquals("", props.getSeeks().getDomains());
        }

        @Test
        @DisplayName("Should set Seeks configuration")
        void testSetSeeks() {
            DoctorDocProperties props = new DoctorDocProperties();
            DoctorDocProperties.Seeks seeks = new DoctorDocProperties.Seeks();
            seeks.setDomains("example.com,test.com");
            props.setSeeks(seeks);

            assertEquals("example.com,test.com", props.getSeeks().getDomains());
        }
    }

    @Nested
    @DisplayName("GBV Configuration")
    class GbvConfiguration {

        @Test
        @DisplayName("Should have empty GBV requester ID by default")
        void testDefaultGbv() {
            DoctorDocProperties props = new DoctorDocProperties();

            assertEquals("", props.getGbv().getRequesterId());
        }

        @Test
        @DisplayName("Should set GBV configuration")
        void testSetGbv() {
            DoctorDocProperties props = new DoctorDocProperties();
            DoctorDocProperties.Gbv gbv = new DoctorDocProperties.Gbv();
            gbv.setRequesterId("GBV-12345");
            props.setGbv(gbv);

            assertEquals("GBV-12345", props.getGbv().getRequesterId());
        }
    }
}
