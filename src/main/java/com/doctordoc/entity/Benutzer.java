package com.doctordoc.entity;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Entity representing a user (Benutzer).
 * Supports different user types: USER (1), LIBRARIAN (2), ADMIN (3).
 * Migrated from Struts JDBC-based entity to JPA.
 */
@Entity
@Table(name = "benutzer")
public class Benutzer implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final int ROLE_USER = 1;
    public static final int ROLE_LIBRARIAN = 2;
    public static final int ROLE_ADMIN = 3;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "UID")
    private Long id;

    @Column(name = "institut")
    private String institut;

    @Column(name = "abteilung")
    private String abteilung;

    @Column(name = "category")
    private Long categoryId;

    @Column(name = "anrede")
    private String anrede = "";

    @Column(name = "vorname")
    private String vorname;

    @Column(name = "name")
    private String name;

    @Column(name = "adr")
    private String adresse;

    @Column(name = "adrzus")
    private String adresszusatz;

    @Column(name = "telp")
    private String telefonnrp;

    @Column(name = "telg")
    private String telefonnrg;

    @Column(name = "plz")
    private String plz;

    @Column(name = "ort")
    private String ort;

    @Column(name = "land")
    private String land;

    @Column(name = "mail")
    private String email;

    @Column(name = "pw")
    private String password;

    @Column(name = "librarycard", length = 50)
    private String librarycard = "";

    @Column(name = "kontoval")
    private boolean validation;

    @Column(name = "loginopt")
    private boolean loginopt;

    @Column(name = "userbestellung")
    private boolean userbestellung;

    @Column(name = "gbvbestellung")
    private boolean gbvbestellung;

    @Column(name = "kontostatus")
    private boolean kontostatus;

    @Column(name = "billing")
    private Long billing;

    @Column(name = "rechte")
    private int rechte = ROLE_USER;

    @Column(name = "datum")
    private LocalDateTime datum;

    @Column(name = "lastuse")
    private LocalDateTime lastuse;

    @Column(name = "gtc")
    private String gtc;

    @Column(name = "gtcdate")
    private LocalDateTime gtcdate;

    @ManyToMany
    @JoinTable(
        name = "v_konto_benutzer",
        joinColumns = @JoinColumn(name = "UID"),
        inverseJoinColumns = @JoinColumn(name = "KID")
    )
    private Set<Konto> kontos = new HashSet<>();

    // Constructors

    public Benutzer() {
    }

    // Helper methods for role checking

    public boolean isAdmin() {
        return rechte == ROLE_ADMIN;
    }

    public boolean isLibrarian() {
        return rechte == ROLE_LIBRARIAN;
    }

    public boolean isUser() {
        return rechte == ROLE_USER;
    }

    public String getRoleName() {
        return switch (rechte) {
            case ROLE_ADMIN -> "ADMIN";
            case ROLE_LIBRARIAN -> "LIBRARIAN";
            default -> "USER";
        };
    }

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getInstitut() {
        return institut;
    }

    public void setInstitut(String institut) {
        this.institut = institut;
    }

    public String getAbteilung() {
        return abteilung;
    }

    public void setAbteilung(String abteilung) {
        this.abteilung = abteilung;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public String getAnrede() {
        return anrede;
    }

    public void setAnrede(String anrede) {
        this.anrede = anrede;
    }

    public String getVorname() {
        return vorname;
    }

    public void setVorname(String vorname) {
        this.vorname = vorname;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAdresse() {
        return adresse;
    }

    public void setAdresse(String adresse) {
        this.adresse = adresse;
    }

    public String getAdresszusatz() {
        return adresszusatz;
    }

    public void setAdresszusatz(String adresszusatz) {
        this.adresszusatz = adresszusatz;
    }

    public String getTelefonnrp() {
        return telefonnrp;
    }

    public void setTelefonnrp(String telefonnrp) {
        this.telefonnrp = telefonnrp;
    }

    public String getTelefonnrg() {
        return telefonnrg;
    }

    public void setTelefonnrg(String telefonnrg) {
        this.telefonnrg = telefonnrg;
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

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getLibrarycard() {
        return librarycard;
    }

    public void setLibrarycard(String librarycard) {
        if (librarycard != null && librarycard.length() > 50) {
            librarycard = librarycard.substring(0, 49);
        }
        this.librarycard = librarycard;
    }

    public boolean isValidation() {
        return validation;
    }

    public void setValidation(boolean validation) {
        this.validation = validation;
    }

    public boolean isLoginopt() {
        return loginopt;
    }

    public void setLoginopt(boolean loginopt) {
        this.loginopt = loginopt;
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

    public Long getBilling() {
        return billing;
    }

    public void setBilling(Long billing) {
        this.billing = billing;
    }

    public int getRechte() {
        return rechte;
    }

    public void setRechte(int rechte) {
        this.rechte = rechte;
    }

    public LocalDateTime getDatum() {
        return datum;
    }

    public void setDatum(LocalDateTime datum) {
        this.datum = datum;
    }

    public LocalDateTime getLastuse() {
        return lastuse;
    }

    public void setLastuse(LocalDateTime lastuse) {
        this.lastuse = lastuse;
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

    public Set<Konto> getKontos() {
        return kontos;
    }

    public void setKontos(Set<Konto> kontos) {
        this.kontos = kontos;
    }

    public String getFullName() {
        StringBuilder sb = new StringBuilder();
        if (vorname != null && !vorname.isEmpty()) {
            sb.append(vorname);
        }
        if (name != null && !name.isEmpty()) {
            if (sb.length() > 0) {
                sb.append(" ");
            }
            sb.append(name);
        }
        return sb.toString();
    }
}
