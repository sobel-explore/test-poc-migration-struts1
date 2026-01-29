package com.doctordoc.controller;

import com.doctordoc.dto.KontoDto;
import com.doctordoc.entity.Konto;
import com.doctordoc.security.DoctorDocUserDetails;
import com.doctordoc.service.KontoService;
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
import java.util.List;

/**
 * Controller for Konto (library account) management.
 * Replaces KontoAction.java from Struts.
 */
@Controller
@RequestMapping("/konto")
public class KontoController {

    private static final Logger LOG = LoggerFactory.getLogger(KontoController.class);

    private final KontoService kontoService;

    public KontoController(KontoService kontoService) {
        this.kontoService = kontoService;
    }

    @GetMapping("/list")
    @PreAuthorize("hasRole('ADMIN')")
    public String listKontos(Model model) {
        List<Konto> kontos = kontoService.findAll();
        model.addAttribute("kontos", kontos);
        model.addAttribute("activeMenu", "kontoverwaltung");
        return "konto/list";
    }

    @GetMapping("/view")
    public String viewKonto(@AuthenticationPrincipal DoctorDocUserDetails userDetails,
                            Model model) {

        Konto konto = userDetails.getActiveKonto();
        if (konto == null) {
            return "redirect:/selectkonto";
        }

        model.addAttribute("konto", konto);
        model.addAttribute("user", userDetails.getBenutzer());
        model.addAttribute("isLibrarian", userDetails.isLibrarian() || userDetails.isAdmin());
        model.addAttribute("activeMenu", "kontoverwaltung");

        return "konto/view";
    }

    @GetMapping("/modify")
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    public String modifyKontoForm(@AuthenticationPrincipal DoctorDocUserDetails userDetails,
                                  Model model) {

        Konto konto = userDetails.getActiveKonto();
        if (konto == null) {
            return "redirect:/selectkonto";
        }

        KontoDto kontoDto = new KontoDto();
        mapEntityToDto(konto, kontoDto);

        model.addAttribute("kontoDto", kontoDto);
        model.addAttribute("konto", konto);
        model.addAttribute("activeMenu", "kontoverwaltung");

        return "konto/form";
    }

    @PostMapping("/modify")
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    public String modifyKonto(@Valid @ModelAttribute("kontoDto") KontoDto kontoDto,
                              BindingResult result,
                              @AuthenticationPrincipal DoctorDocUserDetails userDetails,
                              Model model,
                              RedirectAttributes redirectAttributes) {

        Konto konto = userDetails.getActiveKonto();
        if (konto == null) {
            return "redirect:/selectkonto";
        }

        if (result.hasErrors()) {
            model.addAttribute("konto", konto);
            model.addAttribute("activeMenu", "kontoverwaltung");
            return "konto/form";
        }

        mapDtoToEntity(kontoDto, konto);
        kontoService.save(konto);

        // Update session konto
        userDetails.setActiveKonto(konto);

        LOG.info("User {} modified konto {} ({})",
                userDetails.getUsername(),
                konto.getBibliotheksname(),
                konto.getId());

        redirectAttributes.addFlashAttribute("success", "konto.updated");
        return "redirect:/konto/view";
    }

    @GetMapping("/add")
    @PreAuthorize("hasRole('ADMIN')")
    public String addKontoForm(Model model) {
        model.addAttribute("kontoDto", new KontoDto());
        model.addAttribute("activeMenu", "kontoverwaltung");
        return "konto/form";
    }

    @PostMapping("/add")
    @PreAuthorize("hasRole('ADMIN')")
    public String addKonto(@Valid @ModelAttribute("kontoDto") KontoDto kontoDto,
                           BindingResult result,
                           @AuthenticationPrincipal DoctorDocUserDetails userDetails,
                           Model model,
                           RedirectAttributes redirectAttributes) {

        if (result.hasErrors()) {
            model.addAttribute("activeMenu", "kontoverwaltung");
            return "konto/form";
        }

        Konto konto = new Konto();
        mapDtoToEntity(kontoDto, konto);
        kontoService.save(konto);

        LOG.info("Admin {} created new konto {} ({})",
                userDetails.getUsername(),
                konto.getBibliotheksname(),
                konto.getId());

        redirectAttributes.addFlashAttribute("success", "konto.created");
        return "redirect:/konto/list";
    }

    @PostMapping("/delete/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public String deleteKonto(@PathVariable Long id,
                              @AuthenticationPrincipal DoctorDocUserDetails userDetails,
                              RedirectAttributes redirectAttributes) {

        // Prevent deleting current konto
        if (userDetails.getActiveKonto() != null &&
            userDetails.getActiveKonto().getId().equals(id)) {
            redirectAttributes.addFlashAttribute("error", "konto.delete.current");
            return "redirect:/konto/list";
        }

        kontoService.delete(id);

        LOG.info("Admin {} deleted konto {}", userDetails.getUsername(), id);

        redirectAttributes.addFlashAttribute("success", "konto.deleted");
        return "redirect:/konto/list";
    }

    private void mapDtoToEntity(KontoDto dto, Konto entity) {
        entity.setBibliotheksname(dto.getBibliotheksname());
        entity.setIsil(dto.getIsil());
        entity.setAdresse(dto.getAdresse());
        entity.setAdressenzusatz(dto.getAdressenzusatz());
        entity.setPlz(dto.getPlz());
        entity.setOrt(dto.getOrt());
        entity.setLand(dto.getLand());
        entity.setTimezone(dto.getTimezone());
        entity.setFaxno(dto.getFaxno());
        entity.setTelefon(dto.getTelefon());
        entity.setBibliotheksmail(dto.getBibliotheksmail());
        entity.setDbsmail(dto.getDbsmail());
        entity.setDbsmailpw(dto.getDbsmailpw());
        entity.setEzbid(dto.getEzbid());
        entity.setInstlogolink(dto.getInstlogolink());
        entity.setZdb(dto.isZdb());
        entity.setBilling(dto.getBilling());
        entity.setBillingtype(dto.getBillingtype());
        entity.setMaxordersu(dto.getMaxordersu());
        entity.setMaxordersutotal(dto.getMaxordersutotal());
        entity.setMaxordersj(dto.getMaxordersj());
        entity.setOrderlimits(dto.getOrderlimits());
        entity.setUserlogin(dto.isUserlogin());
        entity.setUserbestellung(dto.isUserbestellung());
        entity.setGbvbestellung(dto.isGbvbestellung());
        entity.setKontostatus(dto.isKontostatus());
        entity.setKontotyp(dto.getKontotyp());
        entity.setDefaultDeloptions(dto.getDefaultDeloptions());
        entity.setShowprivsuppliers(dto.isShowprivsuppliers());
        entity.setShowpubsuppliers(dto.isShowpubsuppliers());
        entity.setGbvbenutzername(dto.getGbvbenutzername());
        entity.setGbvpasswort(dto.getGbvpasswort());
        entity.setGbvrequesterid(dto.getGbvrequesterid());
        entity.setIdsid(dto.getIdsid());
        entity.setIdspasswort(dto.getIdspasswort());
    }

    private void mapEntityToDto(Konto entity, KontoDto dto) {
        dto.setId(entity.getId());
        dto.setBibliotheksname(entity.getBibliotheksname());
        dto.setIsil(entity.getIsil());
        dto.setAdresse(entity.getAdresse());
        dto.setAdressenzusatz(entity.getAdressenzusatz());
        dto.setPlz(entity.getPlz());
        dto.setOrt(entity.getOrt());
        dto.setLand(entity.getLand());
        dto.setTimezone(entity.getTimezone());
        dto.setFaxno(entity.getFaxno());
        dto.setTelefon(entity.getTelefon());
        dto.setBibliotheksmail(entity.getBibliotheksmail());
        dto.setDbsmail(entity.getDbsmail());
        dto.setDbsmailpw(entity.getDbsmailpw());
        dto.setEzbid(entity.getEzbid());
        dto.setInstlogolink(entity.getInstlogolink());
        dto.setZdb(entity.isZdb());
        dto.setBilling(entity.getBilling());
        dto.setBillingtype(entity.getBillingtype());
        dto.setMaxordersu(entity.getMaxordersu());
        dto.setMaxordersutotal(entity.getMaxordersutotal());
        dto.setMaxordersj(entity.getMaxordersj());
        dto.setOrderlimits(entity.getOrderlimits());
        dto.setUserlogin(entity.isUserlogin());
        dto.setUserbestellung(entity.isUserbestellung());
        dto.setGbvbestellung(entity.isGbvbestellung());
        dto.setKontostatus(entity.isKontostatus());
        dto.setKontotyp(entity.getKontotyp());
        dto.setDefaultDeloptions(entity.getDefaultDeloptions());
        dto.setShowprivsuppliers(entity.isShowprivsuppliers());
        dto.setShowpubsuppliers(entity.isShowpubsuppliers());
        dto.setGbvbenutzername(entity.getGbvbenutzername());
        dto.setGbvpasswort(entity.getGbvpasswort());
        dto.setGbvrequesterid(entity.getGbvrequesterid());
        dto.setIdsid(entity.getIdsid());
        dto.setIdspasswort(entity.getIdspasswort());
    }
}
