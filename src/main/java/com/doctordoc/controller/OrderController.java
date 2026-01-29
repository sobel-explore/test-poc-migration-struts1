package com.doctordoc.controller;

import com.doctordoc.dto.OrderDto;
import com.doctordoc.entity.Benutzer;
import com.doctordoc.entity.Bestellungen;
import com.doctordoc.entity.Konto;
import com.doctordoc.entity.Lieferant;
import com.doctordoc.repository.LieferantRepository;
import com.doctordoc.security.DoctorDocUserDetails;
import com.doctordoc.service.OrderService;
import com.doctordoc.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Controller for order (Bestellung) operations.
 * Replaces OrderAction.java and BestellformAction.java from Struts.
 */
@Controller
@RequestMapping("/order")
public class OrderController {

    private static final Logger LOG = LoggerFactory.getLogger(OrderController.class);

    // Order state constants
    private static final String STATE_PENDING = "pending";
    private static final String PRIORITY_NORMAL = "normal";
    private static final Long DEFAULT_SUPPLIER_ID = 0L;
    private static final String MENU_SEARCH_ORDER = "suchenbestellen";

    private final OrderService orderService;
    private final UserService userService;
    private final LieferantRepository lieferantRepository;

    public OrderController(OrderService orderService,
                           UserService userService,
                           LieferantRepository lieferantRepository) {
        this.orderService = orderService;
        this.userService = userService;
        this.lieferantRepository = lieferantRepository;
    }

    @GetMapping("/new")
    public String newOrderForm(@AuthenticationPrincipal DoctorDocUserDetails userDetails,
                               @RequestParam(required = false) String issn,
                               @RequestParam(required = false) String isbn,
                               @RequestParam(required = false) String doi,
                               Model model) {

        Konto konto = userDetails.getActiveKonto();
        if (konto == null) {
            return "redirect:/selectkonto";
        }

        // Check order limits
        if (!orderService.checkMaxOrdersKonto(konto)) {
            model.addAttribute("error", "order.limit.konto.exceeded");
            return "error/orderlimit";
        }

        if (!orderService.checkMaxOrdersUser(userDetails.getBenutzer(), konto)) {
            model.addAttribute("error", "order.limit.user.exceeded");
            return "error/orderlimit";
        }

        OrderDto orderDto = new OrderDto();
        // Pre-fill from URL parameters (OpenURL integration)
        if (issn != null) orderDto.setIssn(issn);
        if (isbn != null) orderDto.setIsbn(isbn);
        if (doi != null) orderDto.setDoi(doi);

        // Set default delivery option
        orderDto.setDeloptions(konto.getDefaultDeloptions());

        model.addAttribute("orderDto", orderDto);
        addFormAttributes(model, konto, userDetails);

        return "order/form";
    }

    @PostMapping("/new")
    public String createOrder(@Valid @ModelAttribute("orderDto") OrderDto orderDto,
                              BindingResult result,
                              @AuthenticationPrincipal DoctorDocUserDetails userDetails,
                              Model model,
                              RedirectAttributes redirectAttributes) {

        Konto konto = userDetails.getActiveKonto();
        if (konto == null) {
            return "redirect:/selectkonto";
        }

        if (result.hasErrors()) {
            addFormAttributes(model, konto, userDetails);
            return "order/form";
        }

        // Determine the ordering user (librarians can order on behalf of users)
        Benutzer orderingUser = userDetails.getBenutzer();
        if (orderDto.getForUserId() != null && orderDto.getForUserId() > 0) {
            if (userDetails.isLibrarian() || userDetails.isAdmin()) {
                orderingUser = userService.findById(orderDto.getForUserId())
                        .orElse(userDetails.getBenutzer());
            }
        }

        // Create order
        Bestellungen order = new Bestellungen();
        mapDtoToEntity(orderDto, order);
        order.setBenutzerId(orderingUser.getId());
        order.setKontoId(konto.getId());
        order.setOrderdate(LocalDateTime.now());
        order.setStatedate(LocalDateTime.now());
        order.setState(STATE_PENDING);

        // Set supplier if selected
        if (orderDto.getLieferantId() != null) {
            order.setLieferantId(orderDto.getLieferantId());
        } else {
            order.setLieferantId(DEFAULT_SUPPLIER_ID);
        }

        orderService.save(order);

        LOG.info("User {} created order {} for user {}",
                userDetails.getUsername(),
                order.getId(),
                orderingUser.getEmail());

        redirectAttributes.addFlashAttribute("success", "order.created");
        return "redirect:/uebersicht";
    }

    @GetMapping("/edit/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    public String editOrderForm(@PathVariable Long id,
                                @AuthenticationPrincipal DoctorDocUserDetails userDetails,
                                Model model) {

        Konto konto = userDetails.getActiveKonto();
        if (konto == null) {
            return "redirect:/selectkonto";
        }

        return orderService.findById(id)
                .map(order -> {
                    // Check authorization
                    if (!orderService.isLegitimateOrder(userDetails.getBenutzer(), konto, order)) {
                        return "redirect:/uebersicht";
                    }

                    OrderDto orderDto = new OrderDto();
                    mapEntityToDto(order, orderDto);

                    model.addAttribute("orderDto", orderDto);
                    model.addAttribute("order", order);
                    addFormAttributes(model, konto, userDetails);

                    return "order/form";
                })
                .orElse("redirect:/uebersicht");
    }

    @PostMapping("/edit/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    public String updateOrder(@PathVariable Long id,
                              @Valid @ModelAttribute("orderDto") OrderDto orderDto,
                              BindingResult result,
                              @AuthenticationPrincipal DoctorDocUserDetails userDetails,
                              Model model,
                              RedirectAttributes redirectAttributes) {

        Konto konto = userDetails.getActiveKonto();
        if (konto == null) {
            return "redirect:/selectkonto";
        }

        if (result.hasErrors()) {
            addFormAttributes(model, konto, userDetails);
            return "order/form";
        }

        return orderService.findById(id)
                .map(order -> {
                    // Check authorization
                    if (!orderService.isLegitimateOrder(userDetails.getBenutzer(), konto, order)) {
                        return "redirect:/uebersicht";
                    }

                    mapDtoToEntity(orderDto, order);
                    order.setStatedate(LocalDateTime.now());

                    // Update supplier
                    if (orderDto.getLieferantId() != null) {
                        order.setLieferantId(orderDto.getLieferantId());
                    }

                    orderService.save(order);

                    LOG.info("User {} updated order {}",
                            userDetails.getUsername(),
                            order.getId());

                    redirectAttributes.addFlashAttribute("success", "order.updated");
                    return "redirect:/uebersicht/detail/" + id;
                })
                .orElse("redirect:/uebersicht");
    }

    @PostMapping("/status/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    public String updateStatus(@PathVariable Long id,
                               @RequestParam String status,
                               @AuthenticationPrincipal DoctorDocUserDetails userDetails,
                               RedirectAttributes redirectAttributes) {

        Konto konto = userDetails.getActiveKonto();
        if (konto == null) {
            return "redirect:/selectkonto";
        }

        // Security: Verify order belongs to current konto before updating (IDOR protection)
        return orderService.findById(id)
                .map(order -> {
                    if (!orderService.isLegitimateOrder(userDetails.getBenutzer(), konto, order)) {
                        LOG.warn("Unauthorized status update attempt: User {} tried to update order {} not in their konto",
                                userDetails.getUsername(), id);
                        redirectAttributes.addFlashAttribute("error", "error.noaccess");
                        return "redirect:/uebersicht";
                    }

                    orderService.updateStatus(id, status);

                    LOG.info("User {} updated order {} status to {}",
                            userDetails.getUsername(), id, status);

                    redirectAttributes.addFlashAttribute("success", "order.status.updated");
                    return "redirect:/uebersicht/detail/" + id;
                })
                .orElseGet(() -> {
                    redirectAttributes.addFlashAttribute("error", "order.notfound");
                    return "redirect:/uebersicht";
                });
    }

    @PostMapping("/delete/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    public String deleteOrder(@PathVariable Long id,
                              @AuthenticationPrincipal DoctorDocUserDetails userDetails,
                              RedirectAttributes redirectAttributes) {

        Konto konto = userDetails.getActiveKonto();
        if (konto == null) {
            return "redirect:/selectkonto";
        }

        return orderService.findById(id)
                .map(order -> {
                    if (!orderService.isLegitimateOrder(userDetails.getBenutzer(), konto, order)) {
                        return "redirect:/uebersicht";
                    }

                    orderService.delete(id);

                    LOG.info("User {} deleted order {}",
                            userDetails.getUsername(), id);

                    redirectAttributes.addFlashAttribute("success", "order.deleted");
                    return "redirect:/uebersicht";
                })
                .orElse("redirect:/uebersicht");
    }

    private void addFormAttributes(Model model, Konto konto, DoctorDocUserDetails userDetails) {
        model.addAttribute("konto", konto);
        model.addAttribute("user", userDetails.getBenutzer());
        model.addAttribute("isLibrarian", userDetails.isLibrarian() || userDetails.isAdmin());
        model.addAttribute("activeMenu", MENU_SEARCH_ORDER);

        // Load suppliers (allgemein=true or belonging to this konto)
        List<Lieferant> suppliers = lieferantRepository.findByAllgemeinTrueOrKontoId(konto.getId());
        model.addAttribute("suppliers", suppliers);

        // Load users for librarian order-on-behalf
        if (userDetails.isLibrarian() || userDetails.isAdmin()) {
            List<Benutzer> users = userService.findByKontoId(konto.getId());
            model.addAttribute("users", users);
        }
    }

    private void mapDtoToEntity(OrderDto dto, Bestellungen entity) {
        entity.setArtikeltitel(dto.getArtikeltitel() != null ? dto.getArtikeltitel() : "");
        entity.setZeitschrift(dto.getZeitschriftentitel() != null ? dto.getZeitschriftentitel() : "");
        entity.setAutor(dto.getAuthor() != null ? dto.getAuthor() : "");
        entity.setIssn(dto.getIssn());
        entity.setIsbn(dto.getIsbn() != null ? dto.getIsbn() : "");
        entity.setJahr(dto.getJahr() != null ? dto.getJahr() : "");
        entity.setJahrgang(dto.getJahrgang() != null ? dto.getJahrgang() : "");
        entity.setHeft(dto.getHeft() != null ? dto.getHeft() : "");
        entity.setSeiten(dto.getSeiten() != null ? dto.getSeiten() : "");
        entity.setDoi(dto.getDoi() != null ? dto.getDoi() : "");
        entity.setPmid(dto.getPmid() != null ? dto.getPmid() : "");
        entity.setBuchtitel(dto.getBuchtitel() != null ? dto.getBuchtitel() : "");
        entity.setBuchkapitel(dto.getKapitel() != null ? dto.getKapitel() : "");
        entity.setVerlag(dto.getVerlag() != null ? dto.getVerlag() : "");
        entity.setMediatype(dto.getMediatype() != null ? dto.getMediatype() : "");
        entity.setBestellquelle(dto.getBestellquelle());
        entity.setOrderpriority(dto.getPriority() != null ? dto.getPriority() : PRIORITY_NORMAL);
        entity.setFileformat(dto.getFileformat() != null ? dto.getFileformat() : "");
        entity.setDeloptions(dto.getDeloptions() != null ? dto.getDeloptions() : "");
        entity.setBibliothek(dto.getBibliothek());
        entity.setSignatur(dto.getSignatur() != null ? dto.getSignatur() : "");
        entity.setSubitonr(dto.getSubitonr());
        entity.setGbvnr(dto.getGbvnr());
        entity.setTrackingnr(dto.getTrackingnr() != null ? dto.getTrackingnr() : "");
        entity.setInternenr(dto.getInterneBestellnr() != null ? dto.getInterneBestellnr() : "");
        entity.setSystembemerkung(dto.getSystembemerkung() != null ? dto.getSystembemerkung() : "");
        entity.setNotizen(dto.getNotizen() != null ? dto.getNotizen() : "");
        entity.setKaufpreis(dto.getKaufpreis());
        entity.setWaehrung(dto.getWaehrung());
        entity.setErledigt(dto.isErledigt());
    }

    private void mapEntityToDto(Bestellungen entity, OrderDto dto) {
        dto.setId(entity.getId());
        dto.setArtikeltitel(entity.getArtikeltitel());
        dto.setZeitschriftentitel(entity.getZeitschrift());
        dto.setAuthor(entity.getAutor());
        dto.setIssn(entity.getIssn());
        dto.setIsbn(entity.getIsbn());
        dto.setJahr(entity.getJahr());
        dto.setJahrgang(entity.getJahrgang());
        dto.setHeft(entity.getHeft());
        dto.setSeiten(entity.getSeiten());
        dto.setDoi(entity.getDoi());
        dto.setPmid(entity.getPmid());
        dto.setBuchtitel(entity.getBuchtitel());
        dto.setKapitel(entity.getBuchkapitel());
        dto.setVerlag(entity.getVerlag());
        dto.setMediatype(entity.getMediatype());
        dto.setBestellquelle(entity.getBestellquelle());
        dto.setPriority(entity.getOrderpriority());
        dto.setFileformat(entity.getFileformat());
        dto.setDeloptions(entity.getDeloptions());
        dto.setBibliothek(entity.getBibliothek());
        dto.setSignatur(entity.getSignatur());
        dto.setSubitonr(entity.getSubitonr());
        dto.setGbvnr(entity.getGbvnr());
        dto.setTrackingnr(entity.getTrackingnr());
        dto.setInterneBestellnr(entity.getInternenr());
        dto.setSystembemerkung(entity.getSystembemerkung());
        dto.setNotizen(entity.getNotizen());
        dto.setKaufpreis(entity.getKaufpreis());
        dto.setWaehrung(entity.getWaehrung());
        dto.setErledigt(entity.getErledigt() != null && entity.getErledigt());
        dto.setStatus(entity.getState());
        dto.setLieferantId(entity.getLieferantId());
    }
}
