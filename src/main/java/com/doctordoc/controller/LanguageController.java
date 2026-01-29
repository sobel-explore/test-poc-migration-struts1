package com.doctordoc.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.LocaleResolver;

import java.net.URI;
import java.util.Locale;
import java.util.Set;

/**
 * Controller for language/locale switching.
 */
@Controller
public class LanguageController {

    private static final String LANG_GERMAN = "de";
    private static final String LANG_FRENCH = "fr";
    private static final Set<String> SUPPORTED_LANGUAGES = Set.of(LANG_GERMAN, LANG_FRENCH, "en");

    private final LocaleResolver localeResolver;

    public LanguageController(LocaleResolver localeResolver) {
        this.localeResolver = localeResolver;
    }

    @GetMapping("/language")
    public String changeLanguage(@RequestParam(defaultValue = "en") String lang,
                                  HttpServletRequest request,
                                  HttpServletResponse response) {
        // Validate language parameter
        if (!SUPPORTED_LANGUAGES.contains(lang)) {
            lang = "en";
        }

        Locale locale = switch (lang) {
            case LANG_GERMAN -> Locale.GERMAN;
            case LANG_FRENCH -> Locale.FRENCH;
            default -> Locale.ENGLISH;
        };

        localeResolver.setLocale(request, response, locale);

        // Redirect back to the previous page or home (with validation)
        String referer = request.getHeader("Referer");
        if (isValidRedirectUrl(referer, request)) {
            return "redirect:" + referer;
        }
        return "redirect:/";
    }

    /**
     * Validates that the redirect URL is safe (same host) to prevent open redirect attacks.
     */
    private boolean isValidRedirectUrl(String url, HttpServletRequest request) {
        if (url == null || url.isEmpty()) {
            return false;
        }
        try {
            URI uri = URI.create(url);
            String refererHost = uri.getHost();
            String serverHost = request.getServerName();

            // Only allow redirects to same host or relative URLs
            return refererHost == null || refererHost.equalsIgnoreCase(serverHost);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
