package com.doctordoc.controller;

import com.doctordoc.config.DoctorDocProperties;
import com.doctordoc.entity.Konto;
import com.doctordoc.security.DoctorDocUserDetails;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Controller for static and placeholder pages.
 */
@Controller
public class PageController {

    // Menu constants
    private static final String MENU_SEARCH_ORDER = "suchenbestellen";
    private static final String MENU_OVERVIEW = "uebersicht";
    private static final String MENU_ACCOUNT_TYPE = "kontotyp";
    private static final String MENU_IMPRESSUM = "impressum";
    private static final String MENU_HOWTO = "howto";
    private static final String MENU_STATS = "stats";
    private static final String MENU_STOCK = "stock";
    private static final String MENU_ADMIN = "admin";

    // Year range for date selectors
    private static final int YEAR_RANGE = 10;

    private final DoctorDocProperties properties;

    public PageController(DoctorDocProperties properties) {
        this.properties = properties;
    }

    @GetMapping("/searchfree")
    public String searchFree(@AuthenticationPrincipal DoctorDocUserDetails userDetails, Model model) {
        addCommonAttributes(userDetails, model);
        model.addAttribute("activeMenu", MENU_SEARCH_ORDER);
        return "search/free";
    }

    @GetMapping("/searchorder")
    public String searchOrder(@AuthenticationPrincipal DoctorDocUserDetails userDetails, Model model) {
        addCommonAttributes(userDetails, model);
        model.addAttribute("activeMenu", MENU_OVERVIEW);
        addYears(model);
        return "search/order";
    }

    @GetMapping("/services")
    public String services(Model model) {
        model.addAttribute("activeMenu", MENU_ACCOUNT_TYPE);
        return "pages/services";
    }

    @GetMapping("/impressum")
    public String impressum(Model model) {
        model.addAttribute("activeMenu", MENU_IMPRESSUM);
        return "pages/impressum";
    }

    @GetMapping("/howto")
    public String howto(@AuthenticationPrincipal DoctorDocUserDetails userDetails,
                        @RequestParam(required = false, defaultValue = "OpenURL") String activesubmenu,
                        Model model) {
        if (userDetails != null) {
            addCommonAttributes(userDetails, model);
        }
        model.addAttribute("activeMenu", MENU_HOWTO);
        model.addAttribute("activesubmenu", activesubmenu);
        model.addAttribute("serverInstallation", properties.getServerInstallation());
        return "pages/howto";
    }

    @GetMapping("/statistics")
    public String statistics(@AuthenticationPrincipal DoctorDocUserDetails userDetails,
                            @RequestParam(required = false, defaultValue = "1") Integer dfrom,
                            @RequestParam(required = false, defaultValue = "1") Integer mfrom,
                            @RequestParam(required = false) Integer yfrom,
                            @RequestParam(required = false, defaultValue = "31") Integer dto,
                            @RequestParam(required = false, defaultValue = "12") Integer mto,
                            @RequestParam(required = false) Integer yto,
                            Model model) {
        addCommonAttributes(userDetails, model);
        model.addAttribute("activeMenu", MENU_STATS);

        int currentYear = LocalDate.now().getYear();
        if (yfrom == null) yfrom = currentYear;
        if (yto == null) yto = currentYear;

        addYears(model);
        model.addAttribute("dfrom", dfrom);
        model.addAttribute("mfrom", mfrom);
        model.addAttribute("yfrom", yfrom);
        model.addAttribute("dto", dto);
        model.addAttribute("mto", mto);
        model.addAttribute("yto", yto);

        // TODO: Add actual statistics data from service
        model.addAttribute("totalOrders", 0);

        return "pages/statistics";
    }

    @GetMapping("/stock")
    public String stock(@AuthenticationPrincipal DoctorDocUserDetails userDetails,
                       @RequestParam(required = false) String q,
                       @RequestParam(required = false, defaultValue = "1") Integer page,
                       Model model) {
        addCommonAttributes(userDetails, model);
        model.addAttribute("activeMenu", MENU_STOCK);
        model.addAttribute("query", q);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", 1);
        model.addAttribute("stocks", new ArrayList<>());
        return "pages/stock";
    }

    @GetMapping("/admin")
    public String admin(@AuthenticationPrincipal DoctorDocUserDetails userDetails, Model model) {
        addCommonAttributes(userDetails, model);
        model.addAttribute("activeMenu", MENU_ADMIN);

        // System info
        model.addAttribute("appVersion", "2.0.0-SPRING");
        model.addAttribute("javaVersion", System.getProperty("java.version"));
        Runtime runtime = Runtime.getRuntime();
        model.addAttribute("freeMemory", (runtime.freeMemory() / 1024 / 1024) + " MB");
        model.addAttribute("totalMemory", (runtime.totalMemory() / 1024 / 1024) + " MB");

        // TODO: Add actual counts from services
        model.addAttribute("totalKontos", 0);
        model.addAttribute("totalUsers", 0);
        model.addAttribute("totalOrders", 0);

        return "pages/admin";
    }

    private void addCommonAttributes(DoctorDocUserDetails userDetails, Model model) {
        if (userDetails != null) {
            Konto konto = userDetails.getActiveKonto();
            model.addAttribute("konto", konto);
            model.addAttribute("user", userDetails.getBenutzer());
            model.addAttribute("isLibrarian", userDetails.isLibrarian() || userDetails.isAdmin());
            model.addAttribute("isAdmin", userDetails.isAdmin());
        }
    }

    private void addYears(Model model) {
        List<Integer> years = new ArrayList<>();
        int currentYear = LocalDate.now().getYear();
        for (int i = currentYear; i >= currentYear - YEAR_RANGE; i--) {
            years.add(i);
        }
        model.addAttribute("years", years);
    }
}
