package com.doctordoc.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO for Konto (library account) form data.
 * Replaces KontoForm.java from Struts.
 */
public class KontoDto {

    private Long id;

    @NotBlank(message = "{error.biblioname.required}")
    @Size(max = 255, message = "{error.biblioname.toolong}")
    private String bibliotheksname;

    @Size(max = 50, message = "{error.isil.toolong}")
    private String isil;

    @Size(max = 255, message = "{error.adresse.toolong}")
    private String adresse;

    @Size(max = 255, message = "{error.adresszusatz.toolong}")
    private String adressenzusatz;

    @Size(max = 20, message = "{error.plz.toolong}")
    private String plz;

    @Size(max = 100, message = "{error.ort.toolong}")
    private String ort;

    private String land;

    private String timezone = "Europe/Berlin";

    @Size(max = 50, message = "{error.faxno.toolong}")
    private String faxno;

    @Size(max = 50, message = "{error.telefon.toolong}")
    private String telefon;

    @Email(message = "{error.email.invalid}")
    private String bibliotheksmail;

    @Email(message = "{error.email.invalid}")
    private String dbsmail;

    private String dbsmailpw;

    @Size(max = 50, message = "{error.ezbid.toolong}")
    private String ezbid;

    private String instlogolink;

    private boolean zdb;
    private Long billing;
    private Long billingtype;

    private Long maxordersu;
    private Long maxordersutotal;
    private Long maxordersj;
    private int orderlimits;

    private boolean userlogin;
    private boolean userbestellung;
    private boolean gbvbestellung;
    private boolean kontostatus = true;
    private int kontotyp;

    private String defaultDeloptions = "post";

    private boolean showprivsuppliers;
    private boolean showpubsuppliers = true;

    // GBV credentials
    private String gbvbenutzername;
    private String gbvpasswort;
    private String gbvrequesterid;

    // IDS credentials
    private String idsid;
    private String idspasswort;

    // Constructors

    public KontoDto() {
    }

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getBibliotheksname() {
        return bibliotheksname;
    }

    public void setBibliotheksname(String bibliotheksname) {
        this.bibliotheksname = bibliotheksname;
    }

    public String getIsil() {
        return isil;
    }

    public void setIsil(String isil) {
        this.isil = isil;
    }

    public String getAdresse() {
        return adresse;
    }

    public void setAdresse(String adresse) {
        this.adresse = adresse;
    }

    public String getAdressenzusatz() {
        return adressenzusatz;
    }

    public void setAdressenzusatz(String adressenzusatz) {
        this.adressenzusatz = adressenzusatz;
    }

    public String getPlz() {
        return plz;
    }

    public void setPlz(String plz) {
        this.plz = plz;
    }

    public String getOrt() {
        return ort;
    }

    public void setOrt(String ort) {
        this.ort = ort;
    }

    public String getLand() {
        return land;
    }

    public void setLand(String land) {
        this.land = land;
    }

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    public String getFaxno() {
        return faxno;
    }

    public void setFaxno(String faxno) {
        this.faxno = faxno;
    }

    public String getTelefon() {
        return telefon;
    }

    public void setTelefon(String telefon) {
        this.telefon = telefon;
    }

    public String getBibliotheksmail() {
        return bibliotheksmail;
    }

    public void setBibliotheksmail(String bibliotheksmail) {
        this.bibliotheksmail = bibliotheksmail;
    }

    public String getDbsmail() {
        return dbsmail;
    }

    public void setDbsmail(String dbsmail) {
        this.dbsmail = dbsmail;
    }

    public String getDbsmailpw() {
        return dbsmailpw;
    }

    public void setDbsmailpw(String dbsmailpw) {
        this.dbsmailpw = dbsmailpw;
    }

    public String getEzbid() {
        return ezbid;
    }

    public void setEzbid(String ezbid) {
        this.ezbid = ezbid;
    }

    public String getInstlogolink() {
        return instlogolink;
    }

    public void setInstlogolink(String instlogolink) {
        this.instlogolink = instlogolink;
    }

    public boolean isZdb() {
        return zdb;
    }

    public void setZdb(boolean zdb) {
        this.zdb = zdb;
    }

    public Long getBilling() {
        return billing;
    }

    public void setBilling(Long billing) {
        this.billing = billing;
    }

    public Long getBillingtype() {
        return billingtype;
    }

    public void setBillingtype(Long billingtype) {
        this.billingtype = billingtype;
    }

    public Long getMaxordersu() {
        return maxordersu;
    }

    public void setMaxordersu(Long maxordersu) {
        this.maxordersu = maxordersu;
    }

    public Long getMaxordersutotal() {
        return maxordersutotal;
    }

    public void setMaxordersutotal(Long maxordersutotal) {
        this.maxordersutotal = maxordersutotal;
    }

    public Long getMaxordersj() {
        return maxordersj;
    }

    public void setMaxordersj(Long maxordersj) {
        this.maxordersj = maxordersj;
    }

    public int getOrderlimits() {
        return orderlimits;
    }

    public void setOrderlimits(int orderlimits) {
        this.orderlimits = orderlimits;
    }

    public boolean isUserlogin() {
        return userlogin;
    }

    public void setUserlogin(boolean userlogin) {
        this.userlogin = userlogin;
    }

    public boolean isUserbestellung() {
        return userbestellung;
    }

    public void setUserbestellung(boolean userbestellung) {
        this.userbestellung = userbestellung;
    }

    public boolean isGbvbestellung() {
        return gbvbestellung;
    }

    public void setGbvbestellung(boolean gbvbestellung) {
        this.gbvbestellung = gbvbestellung;
    }

    public boolean isKontostatus() {
        return kontostatus;
    }

    public void setKontostatus(boolean kontostatus) {
        this.kontostatus = kontostatus;
    }

    public int getKontotyp() {
        return kontotyp;
    }

    public void setKontotyp(int kontotyp) {
        this.kontotyp = kontotyp;
    }

    public String getDefaultDeloptions() {
        return defaultDeloptions;
    }

    public void setDefaultDeloptions(String defaultDeloptions) {
        this.defaultDeloptions = defaultDeloptions;
    }

    public boolean isShowprivsuppliers() {
        return showprivsuppliers;
    }

    public void setShowprivsuppliers(boolean showprivsuppliers) {
        this.showprivsuppliers = showprivsuppliers;
    }

    public boolean isShowpubsuppliers() {
        return showpubsuppliers;
    }

    public void setShowpubsuppliers(boolean showpubsuppliers) {
        this.showpubsuppliers = showpubsuppliers;
    }

    public String getGbvbenutzername() {
        return gbvbenutzername;
    }

    public void setGbvbenutzername(String gbvbenutzername) {
        this.gbvbenutzername = gbvbenutzername;
    }

    public String getGbvpasswort() {
        return gbvpasswort;
    }

    public void setGbvpasswort(String gbvpasswort) {
        this.gbvpasswort = gbvpasswort;
    }

    public String getGbvrequesterid() {
        return gbvrequesterid;
    }

    public void setGbvrequesterid(String gbvrequesterid) {
        this.gbvrequesterid = gbvrequesterid;
    }

    public String getIdsid() {
        return idsid;
    }

    public void setIdsid(String idsid) {
        this.idsid = idsid;
    }

    public String getIdspasswort() {
        return idspasswort;
    }

    public void setIdspasswort(String idspasswort) {
        this.idspasswort = idspasswort;
    }
}
