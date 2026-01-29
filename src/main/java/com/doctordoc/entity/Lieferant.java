package com.doctordoc.entity;

import jakarta.persistence.*;
import java.io.Serializable;

/**
 * Entity representing a supplier (Lieferanten).
 * Mapped exactly to database schema from master_dump.sql.
 */
@Entity
@Table(name = "lieferanten")
public class Lieferant implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "LID")
    private Long id;

    @Column(name = "siegel", length = 15)
    private String siegel;

    @Column(name = "lieferant", length = 200, nullable = false)
    private String lieferant;

    @Column(name = "emailILL", length = 100, nullable = false)
    private String emailILL;

    @Column(name = "countryCode", length = 2, nullable = false)
    private String countryCode;

    @Column(name = "allgemein", nullable = false)
    private Boolean allgemein = false;

    @Column(name = "KID")
    private Long kontoId;

    // Constructors

    public Lieferant() {
    }

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSiegel() {
        return siegel;
    }

    public void setSiegel(String siegel) {
        this.siegel = siegel;
    }

    public String getLieferant() {
        return lieferant;
    }

    public void setLieferant(String lieferant) {
        this.lieferant = lieferant;
    }

    public String getEmailILL() {
        return emailILL;
    }

    public void setEmailILL(String emailILL) {
        this.emailILL = emailILL;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }

    public Boolean getAllgemein() {
        return allgemein;
    }

    public void setAllgemein(Boolean allgemein) {
        this.allgemein = allgemein;
    }

    public Long getKontoId() {
        return kontoId;
    }

    public void setKontoId(Long kontoId) {
        this.kontoId = kontoId;
    }
}
