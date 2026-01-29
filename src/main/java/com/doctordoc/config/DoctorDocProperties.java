package com.doctordoc.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for Doctor-Doc application.
 * Replaces ReadSystemConfigurations.java from the Struts version.
 */
@Component
@ConfigurationProperties(prefix = "doctordoc")
public class DoctorDocProperties {

    private String applicationName = "Doctor-Doc";
    private String serverInstallation = "";
    private String welcomePage = "";
    private String systemTimezone = "Europe/Berlin";
    private String defaultLocale = "de";
    private SystemEmail systemEmail = new SystemEmail();
    private String errorEmail = "";
    private String billingEmail = "";
    private boolean allowRegisterLibraryAccounts = false;
    private Gtc gtc = new Gtc();
    private PaidAccess paidAccess = new PaidAccess();
    private Anonymization anonymization = new Anonymization();
    private int maxResultsDisplay = 200;
    private Carelit carelit = new Carelit();
    private Daia daia = new Daia();
    private Seeks seeks = new Seeks();
    private Gbv gbv = new Gbv();

    // Getters and Setters

    public String getApplicationName() {
        return applicationName;
    }

    public void setApplicationName(String applicationName) {
        this.applicationName = applicationName;
    }

    public String getServerInstallation() {
        return serverInstallation;
    }

    public void setServerInstallation(String serverInstallation) {
        this.serverInstallation = serverInstallation;
    }

    public String getWelcomePage() {
        return welcomePage;
    }

    public void setWelcomePage(String welcomePage) {
        this.welcomePage = welcomePage;
    }

    public String getSystemTimezone() {
        return systemTimezone;
    }

    public void setSystemTimezone(String systemTimezone) {
        this.systemTimezone = systemTimezone;
    }

    public String getDefaultLocale() {
        return defaultLocale;
    }

    public void setDefaultLocale(String defaultLocale) {
        this.defaultLocale = defaultLocale;
    }

    public SystemEmail getSystemEmail() {
        return systemEmail;
    }

    public void setSystemEmail(SystemEmail systemEmail) {
        this.systemEmail = systemEmail;
    }

    public String getErrorEmail() {
        return errorEmail;
    }

    public void setErrorEmail(String errorEmail) {
        this.errorEmail = errorEmail;
    }

    public String getBillingEmail() {
        return billingEmail;
    }

    public void setBillingEmail(String billingEmail) {
        this.billingEmail = billingEmail;
    }

    public boolean isAllowRegisterLibraryAccounts() {
        return allowRegisterLibraryAccounts;
    }

    public void setAllowRegisterLibraryAccounts(boolean allowRegisterLibraryAccounts) {
        this.allowRegisterLibraryAccounts = allowRegisterLibraryAccounts;
    }

    public Gtc getGtc() {
        return gtc;
    }

    public void setGtc(Gtc gtc) {
        this.gtc = gtc;
    }

    public PaidAccess getPaidAccess() {
        return paidAccess;
    }

    public void setPaidAccess(PaidAccess paidAccess) {
        this.paidAccess = paidAccess;
    }

    public Anonymization getAnonymization() {
        return anonymization;
    }

    public void setAnonymization(Anonymization anonymization) {
        this.anonymization = anonymization;
    }

    public int getMaxResultsDisplay() {
        return maxResultsDisplay;
    }

    public void setMaxResultsDisplay(int maxResultsDisplay) {
        this.maxResultsDisplay = maxResultsDisplay;
    }

    public Carelit getCarelit() {
        return carelit;
    }

    public void setCarelit(Carelit carelit) {
        this.carelit = carelit;
    }

    public Daia getDaia() {
        return daia;
    }

    public void setDaia(Daia daia) {
        this.daia = daia;
    }

    public Seeks getSeeks() {
        return seeks;
    }

    public void setSeeks(Seeks seeks) {
        this.seeks = seeks;
    }

    public Gbv getGbv() {
        return gbv;
    }

    public void setGbv(Gbv gbv) {
        this.gbv = gbv;
    }

    // Nested configuration classes

    public static class SystemEmail {
        private String address = "";
        private String host = "";
        private String accountName = "";

        public String getAddress() {
            return address;
        }

        public void setAddress(String address) {
            this.address = address;
        }

        public String getHost() {
            return host;
        }

        public void setHost(String host) {
            this.host = host;
        }

        public String getAccountName() {
            return accountName;
        }

        public void setAccountName(String accountName) {
            this.accountName = accountName;
        }
    }

    public static class Gtc {
        private boolean enabled = false;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }

    public static class PaidAccess {
        private boolean enabled = false;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }

    public static class Anonymization {
        private boolean enabled = true;
        private int afterMonths = 12;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public int getAfterMonths() {
            return afterMonths;
        }

        public void setAfterMonths(int afterMonths) {
            this.afterMonths = afterMonths;
        }
    }

    public static class Carelit {
        private boolean searchEnabled = false;

        public boolean isSearchEnabled() {
            return searchEnabled;
        }

        public void setSearchEnabled(boolean searchEnabled) {
            this.searchEnabled = searchEnabled;
        }
    }

    public static class Daia {
        private boolean enabled = false;
        private String hosts = "";

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getHosts() {
            return hosts;
        }

        public void setHosts(String hosts) {
            this.hosts = hosts;
        }
    }

    public static class Seeks {
        private String domains = "";

        public String getDomains() {
            return domains;
        }

        public void setDomains(String domains) {
            this.domains = domains;
        }
    }

    public static class Gbv {
        private String requesterId = "";

        public String getRequesterId() {
            return requesterId;
        }

        public void setRequesterId(String requesterId) {
            this.requesterId = requesterId;
        }
    }
}
