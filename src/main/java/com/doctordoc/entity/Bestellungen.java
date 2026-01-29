package com.doctordoc.entity;

import jakarta.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entity representing an order (Bestellungen).
 * Mapped exactly to database schema from master_dump.sql.
 */
@Entity
@Table(name = "bestellungen")
public class Bestellungen implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "BID")
    private Long id;

    @Column(name = "KID", nullable = false)
    private Long kontoId;

    @Column(name = "UID", nullable = false)
    private Long benutzerId;

    @Column(name = "LID", nullable = false)
    private Long lieferantId;

    @Column(name = "mediatype", length = 20, nullable = false)
    private String mediatype;

    @Column(name = "orderpriority", length = 20, nullable = false)
    private String orderpriority;

    @Column(name = "deloptions", length = 50, nullable = false)
    private String deloptions;

    @Column(name = "fileformat", length = 20, nullable = false)
    private String fileformat;

    @Column(name = "zeitschrift", length = 100, nullable = false)
    private String zeitschrift;

    @Column(name = "autor", length = 100, nullable = false)
    private String autor;

    @Column(name = "artikeltitel", length = 100, nullable = false)
    private String artikeltitel;

    @Column(name = "jahr", length = 4, nullable = false)
    private String jahr;

    @Column(name = "jahrgang", length = 20, nullable = false)
    private String jahrgang;

    @Column(name = "heft", length = 20, nullable = false)
    private String heft;

    @Column(name = "seiten", length = 20, nullable = false)
    private String seiten;

    @Column(name = "issn", length = 20)
    private String issn;

    @Column(name = "isbn", length = 20, nullable = false)
    private String isbn;

    @Column(name = "buchkapitel", length = 100, nullable = false)
    private String buchkapitel;

    @Column(name = "buchtitel", length = 100, nullable = false)
    private String buchtitel;

    @Column(name = "verlag", length = 100, nullable = false)
    private String verlag;

    @Column(name = "doi", length = 200, nullable = false)
    private String doi;

    @Column(name = "pmid", length = 100, nullable = false)
    private String pmid;

    @Column(name = "subitonr", length = 100)
    private String subitonr;

    @Column(name = "gbvnr", length = 100)
    private String gbvnr;

    @Column(name = "trackingnr", length = 100, nullable = false)
    private String trackingnr;

    @Column(name = "internenr", length = 100, nullable = false)
    private String internenr;

    @Column(name = "biblionr", length = 100)
    private String biblionr;

    @Column(name = "bibliothek", length = 250)
    private String bibliothek;

    @Column(name = "bestellquelle", length = 250)
    private String bestellquelle;

    @Column(name = "state", length = 20)
    private String state;

    @Column(name = "statedate")
    private LocalDateTime statedate;

    @Column(name = "orderdate")
    private LocalDateTime orderdate;

    @Column(name = "erledigt")
    private Boolean erledigt = false;

    @Column(name = "systembemerkung", length = 100, nullable = false)
    private String systembemerkung;

    @Column(name = "notizen", length = 500, nullable = false)
    private String notizen;

    @Column(name = "kaufpreis", precision = 6, scale = 2)
    private BigDecimal kaufpreis;

    @Column(name = "waehrung", length = 10)
    private String waehrung;

    @Column(name = "signatur", length = 250, nullable = false)
    private String signatur;

    // Constructors

    public Bestellungen() {
    }

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getKontoId() {
        return kontoId;
    }

    public void setKontoId(Long kontoId) {
        this.kontoId = kontoId;
    }

    public Long getBenutzerId() {
        return benutzerId;
    }

    public void setBenutzerId(Long benutzerId) {
        this.benutzerId = benutzerId;
    }

    public Long getLieferantId() {
        return lieferantId;
    }

    public void setLieferantId(Long lieferantId) {
        this.lieferantId = lieferantId;
    }

    public String getMediatype() {
        return mediatype;
    }

    public void setMediatype(String mediatype) {
        this.mediatype = mediatype;
    }

    public String getOrderpriority() {
        return orderpriority;
    }

    public void setOrderpriority(String orderpriority) {
        this.orderpriority = orderpriority;
    }

    public String getDeloptions() {
        return deloptions;
    }

    public void setDeloptions(String deloptions) {
        this.deloptions = deloptions;
    }

    public String getFileformat() {
        return fileformat;
    }

    public void setFileformat(String fileformat) {
        this.fileformat = fileformat;
    }

    public String getZeitschrift() {
        return zeitschrift;
    }

    public void setZeitschrift(String zeitschrift) {
        this.zeitschrift = zeitschrift;
    }

    public String getAutor() {
        return autor;
    }

    public void setAutor(String autor) {
        this.autor = autor;
    }

    public String getArtikeltitel() {
        return artikeltitel;
    }

    public void setArtikeltitel(String artikeltitel) {
        this.artikeltitel = artikeltitel;
    }

    public String getJahr() {
        return jahr;
    }

    public void setJahr(String jahr) {
        this.jahr = jahr;
    }

    public String getJahrgang() {
        return jahrgang;
    }

    public void setJahrgang(String jahrgang) {
        this.jahrgang = jahrgang;
    }

    public String getHeft() {
        return heft;
    }

    public void setHeft(String heft) {
        this.heft = heft;
    }

    public String getSeiten() {
        return seiten;
    }

    public void setSeiten(String seiten) {
        this.seiten = seiten;
    }

    public String getIssn() {
        return issn;
    }

    public void setIssn(String issn) {
        this.issn = issn;
    }

    public String getIsbn() {
        return isbn;
    }

    public void setIsbn(String isbn) {
        this.isbn = isbn;
    }

    public String getBuchkapitel() {
        return buchkapitel;
    }

    public void setBuchkapitel(String buchkapitel) {
        this.buchkapitel = buchkapitel;
    }

    public String getBuchtitel() {
        return buchtitel;
    }

    public void setBuchtitel(String buchtitel) {
        this.buchtitel = buchtitel;
    }

    public String getVerlag() {
        return verlag;
    }

    public void setVerlag(String verlag) {
        this.verlag = verlag;
    }

    public String getDoi() {
        return doi;
    }

    public void setDoi(String doi) {
        this.doi = doi;
    }

    public String getPmid() {
        return pmid;
    }

    public void setPmid(String pmid) {
        this.pmid = pmid;
    }

    public String getSubitonr() {
        return subitonr;
    }

    public void setSubitonr(String subitonr) {
        this.subitonr = subitonr;
    }

    public String getGbvnr() {
        return gbvnr;
    }

    public void setGbvnr(String gbvnr) {
        this.gbvnr = gbvnr;
    }

    public String getTrackingnr() {
        return trackingnr;
    }

    public void setTrackingnr(String trackingnr) {
        this.trackingnr = trackingnr;
    }

    public String getInternenr() {
        return internenr;
    }

    public void setInternenr(String internenr) {
        this.internenr = internenr;
    }

    public String getBiblionr() {
        return biblionr;
    }

    public void setBiblionr(String biblionr) {
        this.biblionr = biblionr;
    }

    public String getBibliothek() {
        return bibliothek;
    }

    public void setBibliothek(String bibliothek) {
        this.bibliothek = bibliothek;
    }

    public String getBestellquelle() {
        return bestellquelle;
    }

    public void setBestellquelle(String bestellquelle) {
        this.bestellquelle = bestellquelle;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    /**
     * Returns the status text for display in templates.
     * This is used by Thymeleaf for i18n status display.
     */
    public String getStatustext() {
        return state != null ? state : "pending";
    }

    public LocalDateTime getStatedate() {
        return statedate;
    }

    public void setStatedate(LocalDateTime statedate) {
        this.statedate = statedate;
    }

    public LocalDateTime getOrderdate() {
        return orderdate;
    }

    public void setOrderdate(LocalDateTime orderdate) {
        this.orderdate = orderdate;
    }

    public Boolean getErledigt() {
        return erledigt;
    }

    public void setErledigt(Boolean erledigt) {
        this.erledigt = erledigt;
    }

    public String getSystembemerkung() {
        return systembemerkung;
    }

    public void setSystembemerkung(String systembemerkung) {
        this.systembemerkung = systembemerkung;
    }

    public String getNotizen() {
        return notizen;
    }

    public void setNotizen(String notizen) {
        this.notizen = notizen;
    }

    public BigDecimal getKaufpreis() {
        return kaufpreis;
    }

    public void setKaufpreis(BigDecimal kaufpreis) {
        this.kaufpreis = kaufpreis;
    }

    public String getWaehrung() {
        return waehrung;
    }

    public void setWaehrung(String waehrung) {
        this.waehrung = waehrung;
    }

    public String getSignatur() {
        return signatur;
    }

    public void setSignatur(String signatur) {
        this.signatur = signatur;
    }
}
