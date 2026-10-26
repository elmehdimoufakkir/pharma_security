package com.example.pharma_security.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Aucun hash de mot de passe n'est renvoyé au navigateur. */
public class UserForm {
    @NotBlank(message = "Le nom d'utilisateur est obligatoire.")
    @Pattern(regexp = "[a-zA-Z0-9._-]{3,50}", message = "Utilisez 3 à 50 lettres, chiffres, points, tirets ou underscores.")
    private String username;
    @Size(max = 72, message = "Le mot de passe ne doit pas dépasser 72 caractères.")
    private String password;
    @NotBlank(message = "Le rôle est obligatoire.")
    @Pattern(regexp = "ADMIN|PHARMACIEN|CAISSIER|CLIENT", message = "Le rôle sélectionné est invalide.")
    private String roleName = "CLIENT";
    private boolean enabled = true;

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getRoleName() { return roleName; }
    public void setRoleName(String roleName) { this.roleName = roleName; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
}
