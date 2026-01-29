package com.doctordoc.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * DTO for login form data.
 * Replaces LoginForm.java from Struts.
 */
public record LoginDto(
    @NotBlank(message = "{error.email.required}")
    @Email(message = "{error.email.invalid}")
    String email,

    @NotBlank(message = "{error.password.required}")
    String password,

    Long kontoid,

    Long userid,

    // Fields for link resolver integration
    boolean resolver,
    String artikeltitel,
    String heft,
    String jahr,
    String jahrgang,
    String issn,
    String zdbid,
    String author,
    String seiten,
    String zeitschriftentitel,
    String doi,
    String pmid,
    String sici,
    String lccn,
    String isbn,
    String genre,
    String rfr_id,
    String mediatype,
    String verlag,
    String kapitel,
    String buchtitel,
    String foruser
) {
    public LoginDto {
        if (mediatype == null) {
            mediatype = "Artikel";
        }
        if (foruser == null) {
            foruser = "0";
        }
    }

    public static LoginDto empty() {
        return new LoginDto("", "", null, null, false, "", "", "", "", "", null,
                "", "", "", "", "", "", "", "", "", "", "", "", "", "Artikel", "0");
    }
}
