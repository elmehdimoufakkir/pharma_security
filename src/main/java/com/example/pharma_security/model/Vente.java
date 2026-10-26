package com.example.pharma_security.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import jakarta.persistence.*;

@Entity
@Table(name = "ventes")
public class Vente {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "medicament_id", nullable = false)
    private Medicament medicament;
    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "vendeur_id", nullable = false)
    private User vendeur;
    @Column(nullable = false, length = 120)
    private String nomMedicament;
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal prixUnitaire;
    @Column(nullable = false)
    private int quantite;
    @Column(nullable = false)
    private LocalDateTime dateVente;

    public Vente() { }
    public Vente(Medicament medicament, User vendeur, int quantite) {
        this.medicament = medicament; this.vendeur = vendeur; this.quantite = quantite;
        this.nomMedicament = medicament.getNom(); this.prixUnitaire = medicament.getPrix();
        this.dateVente = LocalDateTime.now();
    }
    public Long getId() { return id; }
    public Medicament getMedicament() { return medicament; }
    public User getVendeur() { return vendeur; }
    public String getNomMedicament() { return nomMedicament; }
    public BigDecimal getPrixUnitaire() { return prixUnitaire; }
    public int getQuantite() { return quantite; }
    public LocalDateTime getDateVente() { return dateVente; }
    public BigDecimal getTotal() { return prixUnitaire.multiply(BigDecimal.valueOf(quantite)); }
}
