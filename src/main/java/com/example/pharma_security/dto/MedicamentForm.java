package com.example.pharma_security.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

/** Le formulaire ne permet pas de modifier directement l'identifiant ou le statut actif. */
public class MedicamentForm {
    @NotBlank(message = "Le nom est obligatoire.")
    @Size(max = 120, message = "Le nom ne doit pas dépasser 120 caractères.")
    private String nom;
    @Size(max = 1000, message = "La description ne doit pas dépasser 1000 caractères.")
    private String description = "";
    @NotNull(message = "Le prix est obligatoire.")
    @DecimalMin(value = "0.01", message = "Le prix doit être supérieur ou égal à 0,01.")
    @Digits(integer = 8, fraction = 2, message = "Le prix accepte au plus 8 chiffres et 2 décimales.")
    private BigDecimal prix;
    @NotNull(message = "La quantité est obligatoire.")
    @Min(value = 0, message = "La quantité ne peut pas être négative.")
    private Integer quantite = 0;

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public BigDecimal getPrix() { return prix; }
    public void setPrix(BigDecimal prix) { this.prix = prix; }
    public Integer getQuantite() { return quantite; }
    public void setQuantite(Integer quantite) { this.quantite = quantite; }
}
