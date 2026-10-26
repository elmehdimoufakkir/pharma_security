package com.example.pharma_security.controller;

import com.example.pharma_security.dto.VenteForm;
import com.example.pharma_security.exception.BusinessException;
import com.example.pharma_security.service.MedicamentService;
import com.example.pharma_security.service.VenteService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/ventes")
public class VenteController {
    private final VenteService ventes;
    private final MedicamentService medicaments;

    public VenteController(VenteService ventes, MedicamentService medicaments) {
        this.ventes = ventes;
        this.medicaments = medicaments;
    }

    @GetMapping
    public String ventes(Authentication authentication, Model model) {
        model.addAttribute("venteForm", new VenteForm());
        remplirPage(authentication, model);
        return "ventes";
    }

    @PostMapping
    public String enregistrer(@Valid @ModelAttribute("venteForm") VenteForm form,
                              BindingResult errors, Authentication authentication,
                              Model model, RedirectAttributes redirect) {
        if (!errors.hasErrors()) {
            try {
                ventes.enregistrer(form.getMedicamentId(), form.getQuantite(), authentication.getName());
            } catch (BusinessException ex) {
                errors.reject("metier", ex.getMessage());
            }
        }
        if (errors.hasErrors()) {
            remplirPage(authentication, model);
            return "ventes";
        }
        redirect.addFlashAttribute("success", "Vente enregistrée et stock décrémenté.");
        return "redirect:/ventes";
    }

    private void remplirPage(Authentication authentication, Model model) {
        boolean caissier = authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_CAISSIER".equals(a.getAuthority()));
        model.addAttribute("medicaments", medicaments.disponibles());
        model.addAttribute("ventes", ventes.historique(authentication.getName(), caissier));
    }
}
