package com.doctordoc.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO for user form data.
 * Replaces UserForm.java from Struts.
 */
public class UserDto {

    private Long id;

    private String institut;

    private String abteilung;

    private Long categoryId;

    private String anrede = "";

    @NotBlank(message = "{error.vorname.required}")
    @Size(max = 100, message = "{error.vorname.toolong}")
    private String vorname;

    @NotBlank(message = "{error.name.required}")
    @Size(max = 100, message = "{error.name.toolong}")
    private String name;

    @Size(max = 255, message = "{error.adresse.toolong}")
    private String adresse;

    @Size(max = 255, message = "{error.adresszusatz.toolong}")
    private String adresszusatz;

    @Size(max = 50, message = "{error.telefon.toolong}")
    private String telefonnrp;

    @Size(max = 50, message = "{error.telefon.toolong}")
    private String telefonnrg;

    @Size(max = 20, message = "{error.plz.toolong}")
    private String plz;

    @Size(max = 100, message = "{error.ort.toolong}")
    private String ort;

    private String land;

    @NotBlank(message = "{error.email.required}")
    @Email(message = "{error.email.invalid}")
    private String email;

    @Size(min = 6, message = "{error.password.tooshort}")
    private String password;

    private String passwordConfirm;

    @Size(max = 50, message = "{error.librarycard.toolong}")
    private String librarycard = "";

    private boolean validation;
    private boolean loginopt;
    private boolean userbestellung;
    private boolean gbvbestellung;
    private boolean kontostatus = true;
    private boolean kontovalidation;

    private int rechte = 1; // Default to USER

    private String gtc;
    private String gtcdate;

    // Constructors

    public UserDto() {
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
        this.vorname = vorname != null ? vorname.trim() : null;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name != null ? name.trim() : null;
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
        this.email = email != null ? email.trim() : null;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPasswordConfirm() {
        return passwordConfirm;
    }

    public void setPasswordConfirm(String passwordConfirm) {
        this.passwordConfirm = passwordConfirm;
    }

    public String getLibrarycard() {
        return librarycard;
    }

    public void setLibrarycard(String librarycard) {
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

    public boolean isKontovalidation() {
        return kontovalidation;
    }

    public void setKontovalidation(boolean kontovalidation) {
        this.kontovalidation = kontovalidation;
    }

    public int getRechte() {
        return rechte;
    }

    public void setRechte(int rechte) {
        this.rechte = rechte;
    }

    public String getGtc() {
        return gtc;
    }

    public void setGtc(String gtc) {
        this.gtc = gtc;
    }

    public String getGtcdate() {
        return gtcdate;
    }

    public void setGtcdate(String gtcdate) {
        this.gtcdate = gtcdate;
    }

    public String getFullName() {
        StringBuilder sb = new StringBuilder();
        if (vorname != null && !vorname.isEmpty()) {
            sb.append(vorname);
        }
        if (name != null && !name.isEmpty()) {
            if (sb.length() > 0) sb.append(" ");
            sb.append(name);
        }
        return sb.toString();
    }
}
