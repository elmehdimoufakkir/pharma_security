package com.example.pharma_security.service;

import com.example.pharma_security.exception.BusinessException;
import com.example.pharma_security.exception.NotFoundException;
import com.example.pharma_security.model.Vente;
import com.example.pharma_security.repository.MedicamentRepository;
import com.example.pharma_security.repository.UserRepository;
import com.example.pharma_security.repository.VenteRepository;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VenteService {
    private final MedicamentRepository medicaments;
    private final UserRepository users;
    private final VenteRepository ventes;

    public VenteService(MedicamentRepository medicaments, UserRepository users, VenteRepository ventes) {
        this.medicaments = medicaments;
        this.users = users;
        this.ventes = ventes;
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'PHARMACIEN', 'CAISSIER')")
    @Transactional
    public Vente enregistrer(Long medicamentId, Integer quantite, String username) {
        if (medicamentId == null || quantite == null || quantite < 1) {
            throw new BusinessException("Choisissez un médicament et une quantité supérieure ou égale à 1.");
        }
        var vendeur = users.findByUsername(username)
                .orElseThrow(() -> new BusinessException("L'utilisateur connecté n'existe plus."));
        if (!vendeur.isEnabled()) {
            throw new BusinessException("Ce compte utilisateur est désactivé.");
        }
        // Le verrou bloque les ventes concurrentes sur le même médicament jusqu'au commit.
        var medicament = medicaments.findLockedById(medicamentId)
                .filter(m -> m.isActif())
                .orElseThrow(() -> new NotFoundException("Médicament introuvable."));
        if (quantite > medicament.getQuantite()) {
            throw new BusinessException("Stock insuffisant : " + medicament.getQuantite() + " unité(s) disponible(s).");
        }
        var vente = new Vente(medicament, vendeur, quantite);
        medicament.setQuantite(medicament.getQuantite() - quantite);
        // Le prix, le vendeur et la date sont déterminés côté serveur.
        return ventes.save(vente);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'PHARMACIEN', 'CAISSIER')")
    @Transactional(readOnly = true)
    public List<Vente> historique(String username, boolean caissierSeulement) {
        return caissierSeulement ? ventes.findTop50ByVendeur_UsernameOrderByDateVenteDesc(username)
                : ventes.findTop50ByOrderByDateVenteDesc();
    }
}
