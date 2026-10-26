package com.example.pharma_security.controller;

import com.example.pharma_security.dto.UserForm;
import com.example.pharma_security.exception.BusinessException;
import com.example.pharma_security.service.UserService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin")
public class AdminController {
    private final UserService users;

    public AdminController(UserService users) { this.users = users; }

    @ModelAttribute("roleNames")
    public java.util.List<String> roleNames() { return UserService.ROLE_NAMES; }

    @GetMapping({"", "/"})
    public String admin(Model model) {
        model.addAttribute("users", users.lister());
        return "admin";
    }

    @GetMapping("/utilisateurs/nouveau")
    public String nouveau(Model model) {
        model.addAttribute("userForm", new UserForm());
        return "user-form";
    }

    @GetMapping("/utilisateurs/{id}/modifier")
    public String modifierForm(@PathVariable Long id, Model model) {
        var user = users.trouver(id);
        var form = new UserForm();
        form.setUsername(user.getUsername());
        form.setRoleName(user.getRole().getName());
        form.setEnabled(user.isEnabled());
        model.addAttribute("userForm", form);
        model.addAttribute("userId", id);
        return "user-form";
    }

    @PostMapping("/utilisateurs")
    public String creer(@Valid @ModelAttribute("userForm") UserForm form, BindingResult errors,
                        RedirectAttributes redirect) {
        if (!errors.hasErrors()) {
            try { users.creer(form); }
            catch (BusinessException ex) { errors.reject("metier", ex.getMessage()); }
        }
        if (errors.hasErrors()) { return "user-form"; }
        redirect.addFlashAttribute("success", "Utilisateur créé.");
        return "redirect:/admin";
    }

    @PostMapping("/utilisateurs/{id}/modifier")
    public String modifier(@PathVariable Long id, @Valid @ModelAttribute("userForm") UserForm form,
                           BindingResult errors, Authentication authentication,
                           Model model, RedirectAttributes redirect) {
        model.addAttribute("userId", id);
        if (!errors.hasErrors()) {
            try { users.modifier(id, form, authentication.getName()); }
            catch (BusinessException ex) { errors.reject("metier", ex.getMessage()); }
        }
        if (errors.hasErrors()) { return "user-form"; }
        redirect.addFlashAttribute("success", "Utilisateur modifié. Les nouveaux droits s'appliquent à sa prochaine connexion.");
        return "redirect:/admin";
    }
}
