package com.doctordoc.entity;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Entity representing a library account (Konto).
 * Migrated from Struts JDBC-based entity to JPA.
 */
@Entity
@Table(name = "konto")
public class Konto implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "KID")
    private Long id;

    @Column(name = "DID")
    private Long did;

    @Column(name = "biblioname")
    private String bibliotheksname;

    @Column(name = "isil")
    private String isil;

    @Column(name = "adresse")
    private String adresse;

    @Column(name = "adresszusatz")
    private String adressenzusatz;

    @Column(name = "plz")
    private String plz;

    @Column(name = "ort")
    private String ort;

    @Column(name = "land")
    private String land;

    @Column(name = "timezone")
    private String timezone = "Europe/Berlin";

    @Column(name = "faxno")
    private String faxno;

    @Column(name = "faxusername")
    private String faxusername;

    @Column(name = "faxpassword")
    private String faxpassword;

    @Column(name = "popfaxend")
    private LocalDate popfaxend;

    @Column(name = "fax2")
    private String faxExtern;

    @Column(name = "telefon")
    private String telefon;

    @Column(name = "bibliomail")
    private String bibliotheksmail;

    @Column(name = "dbsmail")
    private String dbsmail;

    @Column(name = "dbsmailpw")
    private String dbsmailpw;

    @Column(name = "gbvbn")
    private String gbvbenutzername;

    @Column(name = "gbvpw")
    private String gbvpasswort;

    @Column(name = "gbv_requester_id")
    private String gbvrequesterid;

    @Column(name = "ids_id")
    private String idsid;

    @Column(name = "ids_passwort")
    private String idspasswort;

    @Column(name = "ezbid")
    private String ezbid;

    @Column(name = "instlogolink")
    private String instlogolink;

    @Column(name = "zdb")
    private boolean zdb;

    @Column(name = "billing")
    private Long billing;

    @Column(name = "billingtype")
    private Long billingtype;

    @Column(name = "accounting_rhythmvalue")
    private Long accountingRhythmvalue;

    @Column(name = "accounting_rhythmday")
    private Long accountingRhythmday;

    @Column(name = "accounting_rhythmtimeout")
    private Long accountingRhythmtimeout;

    @Column(name = "billingschwellwert")
    private int thresholdValue;

    @Column(name = "maxordersu")
    private Long maxordersu;

    @Column(name = "maxordersutotal")
    private Long maxordersutotal;

    @Column(name = "maxordersj")
    private Long maxordersj;

    @Column(name = "orderlimits")
    private int orderlimits;

    @Column(name = "userlogin")
    private boolean userlogin;

    @Column(name = "userbestellung")
    private boolean userbestellung;

    @Column(name = "gbvbestellung")
    private boolean gbvbestellung;

    @Column(name = "kontostatus")
    private boolean kontostatus;

    @Column(name = "kontotyp")
    private int kontotyp;

    @Column(name = "default_deloptions")
    private String defaultDeloptions = "post";

    @Column(name = "paydate")
    private LocalDate paydate;

    @Column(name = "expdate")
    private LocalDate expdate;

    @Column(name = "edatum")
    private LocalDate edatum;

    @Column(name = "gtc")
    private String gtc;

    @Column(name = "gtcdate")
    private LocalDateTime gtcdate;

    @Column(name = "showprivsuppliers")
    private boolean showprivsuppliers;

    @Column(name = "showpubsuppliers")
    private boolean showpubsuppliers = true;

    @Column(name = "ilvformnr")
    private int ilvformnr;

    @Transient
    private boolean selected;

    // Constructors

    public Konto() {
    }

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getDid() {
        return did;
    }

    public void setDid(Long did) {
        this.did = did;
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

    public String getFaxusername() {
        return faxusername;
    }

    public void setFaxusername(String faxusername) {
        this.faxusername = faxusername;
    }

    public String getFaxpassword() {
        return faxpassword;
    }

    public void setFaxpassword(String faxpassword) {
        this.faxpassword = faxpassword;
    }

    public LocalDate getPopfaxend() {
        return popfaxend;
    }

    public void setPopfaxend(LocalDate popfaxend) {
        this.popfaxend = popfaxend;
    }

    public String getFaxExtern() {
        return faxExtern;
    }

    public void setFaxExtern(String faxExtern) {
        this.faxExtern = faxExtern;
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

    public Long getAccountingRhythmvalue() {
        return accountingRhythmvalue;
    }

    public void setAccountingRhythmvalue(Long accountingRhythmvalue) {
        this.accountingRhythmvalue = accountingRhythmvalue;
    }

    public Long getAccountingRhythmday() {
        return accountingRhythmday;
    }

    public void setAccountingRhythmday(Long accountingRhythmday) {
        this.accountingRhythmday = accountingRhythmday;
    }

    public Long getAccountingRhythmtimeout() {
        return accountingRhythmtimeout;
    }

    public void setAccountingRhythmtimeout(Long accountingRhythmtimeout) {
        this.accountingRhythmtimeout = accountingRhythmtimeout;
    }

    public int getThresholdValue() {
        return thresholdValue;
    }

    public void setThresholdValue(int thresholdValue) {
        this.thresholdValue = thresholdValue;
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

    public LocalDate getPaydate() {
        return paydate;
    }

    public void setPaydate(LocalDate paydate) {
        this.paydate = paydate;
    }

    public LocalDate getExpdate() {
        return expdate;
    }

    public void setExpdate(LocalDate expdate) {
        this.expdate = expdate;
    }

    public LocalDate getEdatum() {
        return edatum;
    }

    public void setEdatum(LocalDate edatum) {
        this.edatum = edatum;
    }

    public String getGtc() {
        return gtc;
    }

    public void setGtc(String gtc) {
        this.gtc = gtc;
    }

    public LocalDateTime getGtcdate() {
        return gtcdate;
    }

    public void setGtcdate(LocalDateTime gtcdate) {
        this.gtcdate = gtcdate;
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

    public int getIlvformnr() {
        return ilvformnr;
    }

    public void setIlvformnr(int ilvformnr) {
        this.ilvformnr = ilvformnr;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }
}
