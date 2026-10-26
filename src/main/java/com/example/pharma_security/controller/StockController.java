package com.example.pharma_security.controller;

import com.example.pharma_security.exception.BusinessException;
import com.example.pharma_security.service.MedicamentService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/stocks")
public class StockController {
    private final MedicamentService service;

    public StockController(MedicamentService service) { this.service = service; }

    @GetMapping
    public String stocks(Model model) {
        model.addAttribute("medicaments", service.lister(false));
        return "stocks";
    }

    @PostMapping("/{id}")
    public String modifier(@PathVariable Long id, @RequestParam Integer quantite, RedirectAttributes redirect) {
        try {
            service.modifierStock(id, quantite);
            redirect.addFlashAttribute("success", "Stock mis à jour.");
        } catch (BusinessException ex) {
            redirect.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/stocks";
    }
}
