package com.example.pharma_security.model;

import java.math.BigDecimal;
import jakarta.persistence.*;

@Entity
@Table(name = "medicaments")
public class Medicament {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 120)
    private String nom;
    @Column(nullable = false, length = 1000)
    private String description;
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal prix;
    @Column(nullable = false)
    private int quantite;
    @Column(nullable = false)
    private boolean actif = true;
    @Version
    private Long version;

    public Medicament() { }
    public Medicament(String nom, String description, BigDecimal prix, int quantite) {
        this.nom = nom; this.description = description; this.prix = prix; this.quantite = quantite;
    }
    public Long getId() { return id; }
    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public BigDecimal getPrix() { return prix; }
    public void setPrix(BigDecimal prix) { this.prix = prix; }
    public int getQuantite() { return quantite; }
    public void setQuantite(int quantite) { this.quantite = quantite; }
    public boolean isActif() { return actif; }
    public void setActif(boolean actif) { this.actif = actif; }
    public Long getVersion() { return version; }
}
