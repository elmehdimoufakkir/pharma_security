package com.example.pharma_security.service;

import com.example.pharma_security.dto.MedicamentForm;
import com.example.pharma_security.exception.BusinessException;
import com.example.pharma_security.exception.NotFoundException;
import com.example.pharma_security.model.Medicament;
import com.example.pharma_security.repository.MedicamentRepository;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class MedicamentService {
    private final MedicamentRepository medicaments;

    public MedicamentService(MedicamentRepository medicaments) { this.medicaments = medicaments; }

    public List<Medicament> lister(boolean clientSeulement) {
        return clientSeulement ? disponibles() : medicaments.findByActifTrueOrderByNomAsc();
    }

    public List<Medicament> disponibles() {
        return medicaments.findByActifTrueAndQuantiteGreaterThanOrderByNomAsc(0);
    }

    public Medicament trouverActif(Long id) {
        return medicaments.findById(id).filter(Medicament::isActif)
                .orElseThrow(() -> new NotFoundException("Médicament introuvable."));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public Medicament creer(MedicamentForm form) {
        verifier(form);
        return medicaments.save(new Medicament(form.getNom().trim(), description(form),
                form.getPrix(), form.getQuantite()));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public Medicament modifier(Long id, MedicamentForm form) {
        verifier(form);
        var medicament = verrouillerActif(id);
        medicament.setNom(form.getNom().trim());
        medicament.setDescription(description(form));
        medicament.setPrix(form.getPrix());
        // Le stock ne se modifie que par le formulaire de stock ou une vente.
        return medicament;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public void supprimer(Long id) {
        // Archivage : les ventes précédentes restent consultables.
        verrouillerActif(id).setActif(false);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'PHARMACIEN')")
    @Transactional
    public void modifierStock(Long id, Integer quantite) {
        if (quantite == null || quantite < 0) {
            throw new BusinessException("Le stock doit être un entier positif ou nul.");
        }
        verrouillerActif(id).setQuantite(quantite);
    }

    private Medicament verrouillerActif(Long id) {
        return medicaments.findLockedById(id).filter(Medicament::isActif)
                .orElseThrow(() -> new NotFoundException("Médicament introuvable."));
    }

    private void verifier(MedicamentForm form) {
        if (form.getNom() == null || form.getNom().isBlank() || form.getPrix() == null
                || form.getPrix().signum() <= 0 || form.getQuantite() == null || form.getQuantite() < 0) {
            throw new BusinessException("Le nom, un prix positif et un stock positif ou nul sont obligatoires.");
        }
    }

    private String description(MedicamentForm form) {
        return form.getDescription() == null ? "" : form.getDescription().trim();
    }
}
