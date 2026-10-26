package com.example.pharma_security.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class VenteForm {
    @NotNull(message = "Sélectionnez un médicament.")
    private Long medicamentId;
    @NotNull(message = "La quantité est obligatoire.")
    @Min(value = 1, message = "La quantité vendue doit être au moins égale à 1.")
    private Integer quantite = 1;

    public Long getMedicamentId() { return medicamentId; }
    public void setMedicamentId(Long medicamentId) { this.medicamentId = medicamentId; }
    public Integer getQuantite() { return quantite; }
    public void setQuantite(Integer quantite) { this.quantite = quantite; }
}
