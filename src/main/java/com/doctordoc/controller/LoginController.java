package com.doctordoc.controller;

import com.doctordoc.config.DoctorDocProperties;
import com.doctordoc.entity.Konto;
import com.doctordoc.security.DoctorDocUserDetails;
import com.doctordoc.service.KontoService;
import com.doctordoc.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpSession;
import java.util.List;

/**
 * Controller for authentication operations.
 * Replaces LoginAction.java from Struts.
 */
@Controller
public class LoginController {

    private static final Logger LOG = LoggerFactory.getLogger(LoginController.class);

    private final DoctorDocProperties properties;
    private final KontoService kontoService;
    private final UserService userService;

    public LoginController(DoctorDocProperties properties,
                           KontoService kontoService,
                           UserService userService) {
        this.properties = properties;
        this.kontoService = kontoService;
        this.userService = userService;
    }

    @GetMapping("/")
    public String home() {
        if (properties.getWelcomePage() != null && !properties.getWelcomePage().isEmpty()) {
            return "redirect:" + properties.getWelcomePage();
        }
        return "redirect:/login";
    }

    @GetMapping("/login")
    public String loginPage(@RequestParam(required = false) String error,
                            @RequestParam(required = false) String logout,
                            @RequestParam(required = false) String expired,
                            Model model) {
        if (error != null) {
            model.addAttribute("error", "error.username_pw");
        }
        if (logout != null) {
            model.addAttribute("message", "login.logged_out");
        }
        if (expired != null) {
            model.addAttribute("message", "login.session_expired");
        }
        model.addAttribute("applicationName", properties.getApplicationName());
        return "login";
    }

    /**
     * Handle account selection when user has multiple accounts.
     */
    @GetMapping("/selectkonto")
    public String selectKontoPage(@AuthenticationPrincipal DoctorDocUserDetails userDetails,
                                  Model model) {
        if (userDetails == null) {
            return "redirect:/login";
        }

        List<Konto> kontos = userDetails.getAvailableKontos();
        if (kontos == null || kontos.isEmpty()) {
            return "redirect:/login?error=true";
        }

        if (kontos.size() == 1) {
            userDetails.setActiveKonto(kontos.get(0));
            return "redirect:/uebersicht";
        }

        model.addAttribute("kontos", kontos);
        model.addAttribute("user", userDetails.getBenutzer());
        return "selectkonto";
    }

    @PostMapping("/selectkonto")
    public String selectKonto(@RequestParam Long kontoId,
                              @AuthenticationPrincipal DoctorDocUserDetails userDetails,
                              HttpSession session,
                              RedirectAttributes redirectAttributes) {
        if (userDetails == null) {
            return "redirect:/login";
        }

        Konto selectedKonto = userDetails.getAvailableKontos().stream()
                .filter(k -> k.getId().equals(kontoId))
                .findFirst()
                .orElse(null);

        if (selectedKonto == null) {
            redirectAttributes.addFlashAttribute("error", "error.konto.invalid");
            return "redirect:/selectkonto";
        }

        userDetails.setActiveKonto(selectedKonto);
        session.setAttribute("activeKonto", selectedKonto);

        LOG.info("User {} selected konto: {} ({})",
                userDetails.getUsername(),
                selectedKonto.getBibliotheksname(),
                selectedKonto.getId());

        return "redirect:/uebersicht";
    }

    /**
     * Handle GTC (General Terms & Conditions) acceptance.
     */
    @GetMapping("/gtc")
    public String gtcPage(@AuthenticationPrincipal DoctorDocUserDetails userDetails,
                          Model model) {
        if (!properties.getGtc().isEnabled()) {
            return "redirect:/uebersicht";
        }
        // TODO: Load current GTC version from database
        model.addAttribute("gtcContent", "Terms and Conditions content here...");
        return "gtc";
    }

    @PostMapping("/gtc/accept")
    public String acceptGtc(@AuthenticationPrincipal DoctorDocUserDetails userDetails,
                            RedirectAttributes redirectAttributes) {
        if (userDetails == null) {
            return "redirect:/login";
        }

        // TODO: Update user's GTC acceptance in database
        LOG.info("User {} accepted GTC", userDetails.getUsername());

        return "redirect:/uebersicht";
    }
}
