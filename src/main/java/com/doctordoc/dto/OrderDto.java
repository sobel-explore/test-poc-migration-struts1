package com.doctordoc.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * DTO for order form data.
 * Simplified version of OrderForm.java from Struts.
 * The original had 200+ fields - this is broken into logical groups.
 */
public class OrderDto {

    private Long id;

    // Article metadata
    @Size(max = 500, message = "{error.artikeltitel.toolong}")
    private String artikeltitel;

    @Size(max = 255, message = "{error.zeitschrift.toolong}")
    private String zeitschriftentitel;

    @Size(max = 255, message = "{error.author.toolong}")
    private String author;

    @Size(max = 20, message = "{error.issn.toolong}")
    private String issn;

    @Size(max = 20, message = "{error.isbn.toolong}")
    private String isbn;

    @Size(max = 10, message = "{error.jahr.toolong}")
    private String jahr;

    @Size(max = 20, message = "{error.jahrgang.toolong}")
    private String jahrgang;

    @Size(max = 20, message = "{error.heft.toolong}")
    private String heft;

    @Size(max = 50, message = "{error.seiten.toolong}")
    private String seiten;

    @Size(max = 100, message = "{error.doi.toolong}")
    private String doi;

    @Size(max = 50, message = "{error.pmid.toolong}")
    private String pmid;

    // Book specific
    @Size(max = 255, message = "{error.buchtitel.toolong}")
    private String buchtitel;

    @Size(max = 100, message = "{error.kapitel.toolong}")
    private String kapitel;

    @Size(max = 255, message = "{error.verlag.toolong}")
    private String verlag;

    // Media type: Artikel, Buch, Teilkopie Buch
    private String mediatype = "Artikel";

    // Order settings
    private Long lieferantId;
    private String bestellquelle;
    private String priority = "normal";
    private String fileformat;
    private String deloptions;

    // Library info
    private String sigel;
    private String bibliothek;
    private String signatur;

    // Order tracking
    private String subitonr;
    private String gbvnr;
    private String trackingnr;
    private String interneBestellnr;

    // Notes
    @Size(max = 2000, message = "{error.systembemerkung.toolong}")
    private String systembemerkung;

    @Size(max = 2000, message = "{error.notizen.toolong}")
    private String notizen;

    // Status
    private String status;
    private boolean erledigt;

    // Pricing
    private BigDecimal kaufpreis;
    private String waehrung = "CHF";

    // Customer selection (for librarians placing orders on behalf of users)
    private Long forUserId;

    // Constructors

    public OrderDto() {
    }

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getArtikeltitel() {
        return artikeltitel;
    }

    public void setArtikeltitel(String artikeltitel) {
        this.artikeltitel = artikeltitel;
    }

    public String getZeitschriftentitel() {
        return zeitschriftentitel;
    }

    public void setZeitschriftentitel(String zeitschriftentitel) {
        this.zeitschriftentitel = zeitschriftentitel;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
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

    public String getBuchtitel() {
        return buchtitel;
    }

    public void setBuchtitel(String buchtitel) {
        this.buchtitel = buchtitel;
    }

    public String getKapitel() {
        return kapitel;
    }

    public void setKapitel(String kapitel) {
        this.kapitel = kapitel;
    }

    public String getVerlag() {
        return verlag;
    }

    public void setVerlag(String verlag) {
        this.verlag = verlag;
    }

    public String getMediatype() {
        return mediatype;
    }

    public void setMediatype(String mediatype) {
        this.mediatype = mediatype;
    }

    public Long getLieferantId() {
        return lieferantId;
    }

    public void setLieferantId(Long lieferantId) {
        this.lieferantId = lieferantId;
    }

    public String getBestellquelle() {
        return bestellquelle;
    }

    public void setBestellquelle(String bestellquelle) {
        this.bestellquelle = bestellquelle;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getFileformat() {
        return fileformat;
    }

    public void setFileformat(String fileformat) {
        this.fileformat = fileformat;
    }

    public String getDeloptions() {
        return deloptions;
    }

    public void setDeloptions(String deloptions) {
        this.deloptions = deloptions;
    }

    public String getSigel() {
        return sigel;
    }

    public void setSigel(String sigel) {
        this.sigel = sigel;
    }

    public String getBibliothek() {
        return bibliothek;
    }

    public void setBibliothek(String bibliothek) {
        this.bibliothek = bibliothek;
    }

    public String getSignatur() {
        return signatur;
    }

    public void setSignatur(String signatur) {
        this.signatur = signatur;
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

    public String getInterneBestellnr() {
        return interneBestellnr;
    }

    public void setInterneBestellnr(String interneBestellnr) {
        this.interneBestellnr = interneBestellnr;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public boolean isErledigt() {
        return erledigt;
    }

    public void setErledigt(boolean erledigt) {
        this.erledigt = erledigt;
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

    public Long getForUserId() {
        return forUserId;
    }

    public void setForUserId(Long forUserId) {
        this.forUserId = forUserId;
    }

    public boolean isJournalArticle() {
        return "Artikel".equals(mediatype);
    }

    public boolean isBook() {
        return "Buch".equals(mediatype) || "Teilkopie Buch".equals(mediatype);
    }
}
