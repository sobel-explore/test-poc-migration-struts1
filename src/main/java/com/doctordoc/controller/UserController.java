package com.doctordoc.controller;

import com.doctordoc.dto.UserDto;
import com.doctordoc.entity.Benutzer;
import com.doctordoc.entity.Konto;
import com.doctordoc.security.DoctorDocUserDetails;
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
import java.util.List;

/**
 * Controller for user management operations.
 * Replaces UserAction.java from Struts.
 */
@Controller
@RequestMapping("/user")
public class UserController {

    private static final Logger LOG = LoggerFactory.getLogger(UserController.class);

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/list")
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    public String listUsers(@AuthenticationPrincipal DoctorDocUserDetails userDetails,
                            Model model) {

        Konto konto = userDetails.getActiveKonto();
        if (konto == null) {
            return "redirect:/selectkonto";
        }

        List<Benutzer> users = userService.findByKontoId(konto.getId());

        model.addAttribute("users", users);
        model.addAttribute("konto", konto);
        model.addAttribute("activeMenu", "benutzerverwaltung");

        return "user/list";
    }

    @GetMapping("/add")
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    public String addUserForm(@AuthenticationPrincipal DoctorDocUserDetails userDetails,
                              Model model) {

        Konto konto = userDetails.getActiveKonto();
        if (konto == null) {
            return "redirect:/selectkonto";
        }

        model.addAttribute("userDto", new UserDto());
        model.addAttribute("konto", konto);
        model.addAttribute("activeMenu", "benutzerverwaltung");

        return "user/form";
    }

    @PostMapping("/add")
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    public String addUser(@Valid @ModelAttribute("userDto") UserDto userDto,
                          BindingResult result,
                          @AuthenticationPrincipal DoctorDocUserDetails userDetails,
                          Model model,
                          RedirectAttributes redirectAttributes) {

        Konto konto = userDetails.getActiveKonto();
        if (konto == null) {
            return "redirect:/selectkonto";
        }

        // Validate password confirmation
        if (userDto.getPassword() != null && !userDto.getPassword().isEmpty()) {
            if (!userDto.getPassword().equals(userDto.getPasswordConfirm())) {
                result.rejectValue("passwordConfirm", "error.password.mismatch");
            }
        }

        if (result.hasErrors()) {
            model.addAttribute("konto", konto);
            model.addAttribute("activeMenu", "benutzerverwaltung");
            return "user/form";
        }

        // Create new user
        Benutzer benutzer = new Benutzer();
        mapDtoToEntity(userDto, benutzer);

        // Add user to konto
        benutzer.getKontos().add(konto);

        userService.save(benutzer);

        LOG.info("User {} created new user: {} ({})",
                userDetails.getUsername(),
                benutzer.getEmail(),
                benutzer.getId());

        redirectAttributes.addFlashAttribute("success", "user.created");
        return "redirect:/user/list";
    }

    @GetMapping("/edit/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    public String editUserForm(@PathVariable Long id,
                               @AuthenticationPrincipal DoctorDocUserDetails userDetails,
                               Model model) {

        Konto konto = userDetails.getActiveKonto();
        if (konto == null) {
            return "redirect:/selectkonto";
        }

        return userService.findById(id)
                .map(benutzer -> {
                    // Check if user belongs to current konto
                    if (!benutzer.getKontos().stream().anyMatch(k -> k.getId().equals(konto.getId()))) {
                        return "redirect:/user/list";
                    }

                    UserDto userDto = new UserDto();
                    mapEntityToDto(benutzer, userDto);

                    model.addAttribute("userDto", userDto);
                    model.addAttribute("konto", konto);
                    model.addAttribute("activeMenu", "benutzerverwaltung");

                    return "user/form";
                })
                .orElse("redirect:/user/list");
    }

    @PostMapping("/edit/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    public String editUser(@PathVariable Long id,
                           @Valid @ModelAttribute("userDto") UserDto userDto,
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
            model.addAttribute("activeMenu", "benutzerverwaltung");
            return "user/form";
        }

        return userService.findById(id)
                .map(benutzer -> {
                    mapDtoToEntity(userDto, benutzer);
                    userService.save(benutzer);

                    LOG.info("User {} updated user: {} ({})",
                            userDetails.getUsername(),
                            benutzer.getEmail(),
                            benutzer.getId());

                    redirectAttributes.addFlashAttribute("success", "user.updated");
                    return "redirect:/user/list";
                })
                .orElse("redirect:/user/list");
    }

    @PostMapping("/delete/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    public String deleteUser(@PathVariable Long id,
                             @AuthenticationPrincipal DoctorDocUserDetails userDetails,
                             RedirectAttributes redirectAttributes) {

        Konto konto = userDetails.getActiveKonto();
        if (konto == null) {
            return "redirect:/selectkonto";
        }

        // Prevent deleting yourself
        if (id.equals(userDetails.getUserId())) {
            redirectAttributes.addFlashAttribute("error", "user.delete.self");
            return "redirect:/user/list";
        }

        userService.delete(id);

        LOG.info("User {} deleted user: {}", userDetails.getUsername(), id);

        redirectAttributes.addFlashAttribute("success", "user.deleted");
        return "redirect:/user/list";
    }

    @GetMapping("/profile")
    public String profileForm(@AuthenticationPrincipal DoctorDocUserDetails userDetails,
                              Model model) {

        Konto konto = userDetails.getActiveKonto();
        Benutzer benutzer = userDetails.getBenutzer();

        UserDto userDto = new UserDto();
        mapEntityToDto(benutzer, userDto);

        model.addAttribute("userDto", userDto);
        model.addAttribute("konto", konto);
        model.addAttribute("activeMenu", "profil");

        return "user/profile";
    }

    @PostMapping("/profile")
    public String updateProfile(@Valid @ModelAttribute("userDto") UserDto userDto,
                                BindingResult result,
                                @AuthenticationPrincipal DoctorDocUserDetails userDetails,
                                Model model,
                                RedirectAttributes redirectAttributes) {

        Konto konto = userDetails.getActiveKonto();

        if (result.hasErrors()) {
            model.addAttribute("konto", konto);
            model.addAttribute("activeMenu", "profil");
            return "user/profile";
        }

        Benutzer benutzer = userDetails.getBenutzer();

        // Only allow updating specific fields for own profile
        benutzer.setInstitut(userDto.getInstitut());
        benutzer.setAbteilung(userDto.getAbteilung());
        benutzer.setVorname(userDto.getVorname());
        benutzer.setName(userDto.getName());
        benutzer.setAdresse(userDto.getAdresse());
        benutzer.setAdresszusatz(userDto.getAdresszusatz());
        benutzer.setTelefonnrp(userDto.getTelefonnrp());
        benutzer.setTelefonnrg(userDto.getTelefonnrg());
        benutzer.setPlz(userDto.getPlz());
        benutzer.setOrt(userDto.getOrt());
        benutzer.setLand(userDto.getLand());

        userService.save(benutzer);

        redirectAttributes.addFlashAttribute("success", "profile.updated");
        return "redirect:/user/profile";
    }

    private void mapDtoToEntity(UserDto dto, Benutzer entity) {
        entity.setInstitut(dto.getInstitut());
        entity.setAbteilung(dto.getAbteilung());
        entity.setCategoryId(dto.getCategoryId());
        entity.setAnrede(dto.getAnrede());
        entity.setVorname(dto.getVorname());
        entity.setName(dto.getName());
        entity.setAdresse(dto.getAdresse());
        entity.setAdresszusatz(dto.getAdresszusatz());
        entity.setTelefonnrp(dto.getTelefonnrp());
        entity.setTelefonnrg(dto.getTelefonnrg());
        entity.setPlz(dto.getPlz());
        entity.setOrt(dto.getOrt());
        entity.setLand(dto.getLand());
        entity.setEmail(dto.getEmail());
        entity.setLibrarycard(dto.getLibrarycard());
        entity.setValidation(dto.isValidation());
        entity.setLoginopt(dto.isLoginopt());
        entity.setUserbestellung(dto.isUserbestellung());
        entity.setGbvbestellung(dto.isGbvbestellung());
        entity.setKontostatus(dto.isKontostatus());
        entity.setRechte(dto.getRechte());

        // Only set password if provided
        if (dto.getPassword() != null && !dto.getPassword().isEmpty()) {
            // Password should be hashed by service layer
            entity.setPassword(dto.getPassword());
        }
    }

    private void mapEntityToDto(Benutzer entity, UserDto dto) {
        dto.setId(entity.getId());
        dto.setInstitut(entity.getInstitut());
        dto.setAbteilung(entity.getAbteilung());
        dto.setCategoryId(entity.getCategoryId());
        dto.setAnrede(entity.getAnrede());
        dto.setVorname(entity.getVorname());
        dto.setName(entity.getName());
        dto.setAdresse(entity.getAdresse());
        dto.setAdresszusatz(entity.getAdresszusatz());
        dto.setTelefonnrp(entity.getTelefonnrp());
        dto.setTelefonnrg(entity.getTelefonnrg());
        dto.setPlz(entity.getPlz());
        dto.setOrt(entity.getOrt());
        dto.setLand(entity.getLand());
        dto.setEmail(entity.getEmail());
        dto.setLibrarycard(entity.getLibrarycard());
        dto.setValidation(entity.isValidation());
        dto.setLoginopt(entity.isLoginopt());
        dto.setUserbestellung(entity.isUserbestellung());
        dto.setGbvbestellung(entity.isGbvbestellung());
        dto.setKontostatus(entity.isKontostatus());
        dto.setRechte(entity.getRechte());
    }
}
