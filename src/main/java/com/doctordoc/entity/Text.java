package com.doctordoc.entity;

import jakarta.persistence.*;
import java.io.Serializable;

/**
 * Entity representing text/configuration entries.
 * Used for various configuration texts, categories, and settings.
 */
@Entity
@Table(name = "text")
public class Text implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "TID")
    private Long id;

    @Column(name = "KID")
    private Long kontoId;

    @Column(name = "texttyp")
    private String texttyp;

    @Column(name = "inhalt", columnDefinition = "TEXT")
    private String inhalt;

    // Constructors

    public Text() {
    }

    public Text(Long id) {
        this.id = id;
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

    public String getTexttyp() {
        return texttyp;
    }

    public void setTexttyp(String texttyp) {
        this.texttyp = texttyp;
    }

    public String getInhalt() {
        return inhalt;
    }

    public void setInhalt(String inhalt) {
        this.inhalt = inhalt;
    }

    @Override
    public String toString() {
        return inhalt != null ? inhalt : "";
    }
}
