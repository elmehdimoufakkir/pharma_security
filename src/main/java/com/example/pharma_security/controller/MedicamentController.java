package com.example.pharma_security.controller;

import com.example.pharma_security.dto.MedicamentForm;
import com.example.pharma_security.exception.BusinessException;
import com.example.pharma_security.service.MedicamentService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/medicaments")
public class MedicamentController {
    private final MedicamentService service;

    public MedicamentController(MedicamentService service) { this.service = service; }

    @GetMapping
    public String lister(Authentication authentication, Model model) {
        boolean client = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_CLIENT"));
        model.addAttribute("medicaments", service.lister(client));
        return "medicaments";
    }

    @GetMapping("/nouveau")
    public String nouveau(Model model) {
        model.addAttribute("medicamentForm", new MedicamentForm());
        return "medicament-form";
    }

    @GetMapping("/{id}/modifier")
    public String modifierForm(@PathVariable Long id, Model model) {
        var medicament = service.trouverActif(id);
        var form = new MedicamentForm();
        form.setNom(medicament.getNom());
        form.setDescription(medicament.getDescription());
        form.setPrix(medicament.getPrix());
        form.setQuantite(medicament.getQuantite());
        model.addAttribute("medicamentForm", form);
        model.addAttribute("medicamentId", id);
        return "medicament-form";
    }

    @PostMapping
    public String creer(@Valid @ModelAttribute("medicamentForm") MedicamentForm form,
                        BindingResult errors, RedirectAttributes redirect) {
        if (errors.hasErrors()) { return "medicament-form"; }
        try { service.creer(form); }
        catch (BusinessException ex) { errors.reject("metier", ex.getMessage()); return "medicament-form"; }
        redirect.addFlashAttribute("success", "Médicament créé.");
        return "redirect:/medicaments";
    }

    @PostMapping("/{id}/modifier")
    public String modifier(@PathVariable Long id,
                           @Valid @ModelAttribute("medicamentForm") MedicamentForm form,
                           BindingResult errors, Model model, RedirectAttributes redirect) {
        model.addAttribute("medicamentId", id);
        if (errors.hasErrors()) { return "medicament-form"; }
        try { service.modifier(id, form); }
        catch (BusinessException ex) { errors.reject("metier", ex.getMessage()); return "medicament-form"; }
        redirect.addFlashAttribute("success", "Médicament modifié.");
        return "redirect:/medicaments";
    }

    @PostMapping("/{id}/supprimer")
    public String supprimer(@PathVariable Long id, RedirectAttributes redirect) {
        service.supprimer(id);
        redirect.addFlashAttribute("success", "Médicament archivé. Son historique de ventes est conservé.");
        return "redirect:/medicaments";
    }
}
