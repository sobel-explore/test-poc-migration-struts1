package com.doctordoc.controller;

import com.doctordoc.config.DoctorDocProperties;
import com.doctordoc.entity.Bestellungen;
import com.doctordoc.entity.Konto;
import com.doctordoc.security.DoctorDocUserDetails;
import com.doctordoc.service.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for the main overview page showing orders.
 * Replaces parts of OrderAction.java (overview functionality) from Struts.
 */
@Controller
@RequestMapping("/uebersicht")
public class OverviewController {

    private static final Logger LOG = LoggerFactory.getLogger(OverviewController.class);
    private static final String MENU_OVERVIEW = "uebersicht";
    private static final String MENU_SEARCH_ORDER = "suchenbestellen";
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final String DEFAULT_SORT_FIELD = "orderdate";

    private final OrderService orderService;
    private final DoctorDocProperties properties;

    public OverviewController(OrderService orderService, DoctorDocProperties properties) {
        this.orderService = orderService;
        this.properties = properties;
    }

    @GetMapping
    public String overview(@AuthenticationPrincipal DoctorDocUserDetails userDetails,
                           @RequestParam(name = "filter", defaultValue = "") String filter,
                           @RequestParam(defaultValue = "") String search,
                           @RequestParam(defaultValue = "0") int page,
                           @RequestParam(defaultValue = "20") int size, // DEFAULT_PAGE_SIZE
                           Model model) {

        if (userDetails == null) {
            return "redirect:/login";
        }

        // Check if account is selected
        Konto konto = userDetails.getActiveKonto();
        if (konto == null) {
            if (userDetails.hasMultipleKontos()) {
                return "redirect:/selectkonto";
            }
            return "redirect:/login?error=true";
        }

        // Limit page size
        if (size > properties.getMaxResultsDisplay()) {
            size = properties.getMaxResultsDisplay();
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, DEFAULT_SORT_FIELD));

        Page<Bestellungen> orders;
        if (!search.isEmpty()) {
            orders = orderService.searchOrders(konto.getId(), search, pageable);
        } else if (!filter.isEmpty()) {
            orders = orderService.findByKontoIdAndState(konto.getId(), filter, pageable);
        } else {
            orders = orderService.findByKontoIdPaged(konto.getId(), pageable);
        }

        model.addAttribute("orders", orders);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", orders.getTotalPages());
        model.addAttribute("totalItems", orders.getTotalElements());
        model.addAttribute("status", filter);
        model.addAttribute("search", search);
        model.addAttribute("konto", konto);
        model.addAttribute("user", userDetails.getBenutzer());
        model.addAttribute("isLibrarian", userDetails.isLibrarian() || userDetails.isAdmin());
        model.addAttribute("activeMenu", MENU_OVERVIEW);

        return "order/overview";
    }

    @GetMapping("/detail/{id}")
    public String orderDetail(@PathVariable Long id,
                              @AuthenticationPrincipal DoctorDocUserDetails userDetails,
                              Model model) {

        if (userDetails == null) {
            return "redirect:/login";
        }

        Konto konto = userDetails.getActiveKonto();
        if (konto == null) {
            return "redirect:/selectkonto";
        }

        return orderService.findById(id)
                .map(order -> {
                    // Check authorization
                    if (!orderService.isLegitimateOrder(userDetails.getBenutzer(), konto, order)) {
                        LOG.warn("User {} attempted to access unauthorized order {}",
                                userDetails.getUsername(), id);
                        return "redirect:/uebersicht";
                    }

                    model.addAttribute("order", order);
                    model.addAttribute("konto", konto);
                    model.addAttribute("user", userDetails.getBenutzer());
                    model.addAttribute("isLibrarian", userDetails.isLibrarian() || userDetails.isAdmin());
                    model.addAttribute("activeMenu", MENU_SEARCH_ORDER);

                    return "order/detail";
                })
                .orElse("redirect:/uebersicht");
    }
}
