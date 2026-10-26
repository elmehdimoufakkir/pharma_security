package com.example.pharma_security;

import com.example.pharma_security.dto.MedicamentForm;
import com.example.pharma_security.dto.UserForm;
import com.example.pharma_security.exception.BusinessException;
import com.example.pharma_security.exception.NotFoundException;
import com.example.pharma_security.model.Medicament;
import com.example.pharma_security.model.Role;
import com.example.pharma_security.model.User;
import com.example.pharma_security.repository.MedicamentRepository;
import com.example.pharma_security.repository.RoleRepository;
import com.example.pharma_security.repository.UserRepository;
import com.example.pharma_security.repository.VenteRepository;
import com.example.pharma_security.service.MedicamentService;
import com.example.pharma_security.service.UserService;
import com.example.pharma_security.service.VenteService;
import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Transactions réelles : les assertions relisent la base après chaque commit ou rollback. */
@SpringBootTest
@ActiveProfiles("test")
@WithMockUser(username = "admin", roles = "ADMIN")
class BusinessIntegrationTest {
    @Autowired MedicamentRepository medicaments;
    @Autowired RoleRepository roles;
    @Autowired UserRepository users;
    @Autowired VenteRepository ventes;
    @Autowired MedicamentService medicamentService;
    @Autowired VenteService venteService;
    @Autowired UserService userService;
    @Autowired PasswordEncoder passwords;

    private Long medicamentId;
    private Long adminId;

    @BeforeEach
    void preparerBase() {
        ventes.deleteAll();
        medicaments.deleteAll();
        users.deleteAll();
        roles.deleteAll();
        for (String name : UserService.ROLE_NAMES) { roles.save(new Role(name)); }
        adminId = users.save(new User("admin", passwords.encode("admin123"), role("ADMIN"))).getId();
        users.save(new User("pharma", passwords.encode("pharma123"), role("PHARMACIEN")));
        users.save(new User("caisse", passwords.encode("caisse123"), role("CAISSIER")));
        medicamentId = medicaments.save(new Medicament("Produit A", "Boîte de test",
                new BigDecimal("12.50"), 5)).getId();
    }

    @Test
    @WithMockUser(username = "caisse", roles = "CAISSIER")
    void venteConservePrixVendeurEtTotalEtDecrementeLeStock() {
        var vente = venteService.enregistrer(medicamentId, 2, "caisse");
        assertThat(medicaments.findById(medicamentId).orElseThrow().getQuantite()).isEqualTo(3);
        assertThat(ventes.count()).isEqualTo(1);
        var persisted = ventes.findById(vente.getId()).orElseThrow();
        assertThat(persisted.getVendeur().getUsername()).isEqualTo("caisse");
        assertThat(persisted.getNomMedicament()).isEqualTo("Produit A");
        assertThat(persisted.getPrixUnitaire()).isEqualByComparingTo("12.50");
        assertThat(persisted.getTotal()).isEqualByComparingTo("25.00");
        assertThat(persisted.getDateVente()).isNotNull();

        var medicament = medicaments.findById(medicamentId).orElseThrow();
        medicament.setNom("Produit renommé");
        medicament.setPrix(new BigDecimal("99.00"));
        medicaments.save(medicament);
        var historique = ventes.findById(vente.getId()).orElseThrow();
        assertThat(historique.getNomMedicament()).isEqualTo("Produit A");
        assertThat(historique.getPrixUnitaire()).isEqualByComparingTo("12.50");
        assertThat(historique.getTotal()).isEqualByComparingTo("25.00");
    }

    @Test
    void venteRefuseStockInsuffisantSansModificationPartielle() {
        assertThatThrownBy(() -> venteService.enregistrer(medicamentId, 6, "admin"))
                .isInstanceOf(BusinessException.class).hasMessageContaining("Stock insuffisant");
        assertThat(ventes.count()).isZero();
        assertThat(medicaments.findById(medicamentId).orElseThrow().getQuantite()).isEqualTo(5);
    }

    @Test
    void venteRefuseQuantitesNullesNullesOuNegatives() {
        for (Integer quantite : new Integer[]{null, 0, -2}) {
            assertThatThrownBy(() -> venteService.enregistrer(medicamentId, quantite, "admin"))
                    .isInstanceOf(BusinessException.class);
        }
        assertThatThrownBy(() -> venteService.enregistrer(null, 1, "admin"))
                .isInstanceOf(BusinessException.class);
        assertThat(ventes.count()).isZero();
        assertThat(medicaments.findById(medicamentId).orElseThrow().getQuantite()).isEqualTo(5);
    }

    @Test
    void venteRefuseMedicamentInconnuOuArchive() {
        assertThatThrownBy(() -> venteService.enregistrer(Long.MAX_VALUE, 1, "admin"))
                .isInstanceOf(NotFoundException.class);
        medicamentService.supprimer(medicamentId);
        assertThatThrownBy(() -> venteService.enregistrer(medicamentId, 1, "admin"))
                .isInstanceOf(NotFoundException.class);
        assertThat(ventes.count()).isZero();
        assertThat(medicaments.findById(medicamentId).orElseThrow().getQuantite()).isEqualTo(5);
    }

    @Test
    void venteRefuseCompteDesactive() {
        var caisse = users.findByUsername("caisse").orElseThrow();
        caisse.setEnabled(false);
        users.save(caisse);
        assertThatThrownBy(() -> venteService.enregistrer(medicamentId, 1, "caisse"))
                .isInstanceOf(BusinessException.class).hasMessageContaining("désactivé");
        assertThat(ventes.count()).isZero();
        assertThat(medicaments.findById(medicamentId).orElseThrow().getQuantite()).isEqualTo(5);
    }

    @Test
    void editionMetadonneesIgnoreQuantiteSoumise() {
        var form = new MedicamentForm();
        form.setNom("Produit modifié");
        form.setDescription("Nouvelle boîte");
        form.setPrix(new BigDecimal("15.00"));
        form.setQuantite(999);
        medicamentService.modifier(medicamentId, form);
        var medicament = medicaments.findById(medicamentId).orElseThrow();
        assertThat(medicament.getNom()).isEqualTo("Produit modifié");
        assertThat(medicament.getPrix()).isEqualByComparingTo("15.00");
        assertThat(medicament.getQuantite()).isEqualTo(5);
    }

    @Test
    @WithMockUser(username = "pharma", roles = "PHARMACIEN")
    void pharmacienModifieStockSansPouvoirModifierPrix() {
        medicamentService.modifierStock(medicamentId, 8);
        assertThat(medicaments.findById(medicamentId).orElseThrow().getQuantite()).isEqualTo(8);
        var form = new MedicamentForm();
        form.setNom("Modification interdite");
        form.setPrix(new BigDecimal("0.01"));
        assertThatThrownBy(() -> medicamentService.modifier(medicamentId, form))
                .isInstanceOf(AccessDeniedException.class);
        assertThat(medicaments.findById(medicamentId).orElseThrow().getPrix()).isEqualByComparingTo("12.50");
    }

    @Test
    @WithMockUser(username = "client", roles = "CLIENT")
    void servicesRefusentMutationsClient() {
        assertThatThrownBy(() -> medicamentService.modifierStock(medicamentId, 0))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> venteService.enregistrer(medicamentId, 1, "client"))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> userService.lister()).isInstanceOf(AccessDeniedException.class);
        assertThat(ventes.count()).isZero();
    }

    @Test
    void stockNegatifRefuse() {
        assertThatThrownBy(() -> medicamentService.modifierStock(medicamentId, -1))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> medicamentService.modifierStock(medicamentId, null))
                .isInstanceOf(BusinessException.class);
        assertThat(medicaments.findById(medicamentId).orElseThrow().getQuantite()).isEqualTo(5);
    }

    @Test
    void creationUtilisateurChiffreMotDePasseEtRefuseDoublons() {
        var form = utilisateur("nouveau", "secret123", "CLIENT", true);
        var user = userService.creer(form);
        var persisted = users.findById(user.getId()).orElseThrow();
        assertThat(persisted.getPassword()).isNotEqualTo("secret123").startsWith("$2");
        assertThat(passwords.matches("secret123", persisted.getPassword())).isTrue();
        assertThat(persisted.getRole().getName()).isEqualTo("CLIENT");
        assertThatThrownBy(() -> userService.creer(form)).isInstanceOf(BusinessException.class)
                .hasMessageContaining("existe déjà");
        assertThat(users.count()).isEqualTo(4);
    }

    @Test
    void editionMotDePasseVideConserveHashExistant() {
        var caisse = users.findByUsername("caisse").orElseThrow();
        String hash = caisse.getPassword();
        userService.modifier(caisse.getId(), utilisateur("caisse", "", "PHARMACIEN", true), "admin");
        var persisted = users.findById(caisse.getId()).orElseThrow();
        assertThat(persisted.getPassword()).isEqualTo(hash);
        assertThat(persisted.getRole().getName()).isEqualTo("PHARMACIEN");
    }

    @Test
    void creationMotDePasseCourtRefuseeSansEnregistrement() {
        assertThatThrownBy(() -> userService.creer(utilisateur("nouveau", "court", "CLIENT", true)))
                .isInstanceOf(BusinessException.class).hasMessageContaining("8 caractères");
        assertThat(users.count()).isEqualTo(3);
    }

    @Test
    void propreDesactivationOuRetrogradationAdminRefusee() {
        assertThatThrownBy(() -> userService.modifier(adminId,
                utilisateur("admin", "", "ADMIN", false), "admin"))
                .isInstanceOf(BusinessException.class).hasMessageContaining("propre rôle ADMIN");
        assertThatThrownBy(() -> userService.modifier(adminId,
                utilisateur("admin", "", "CLIENT", true), "admin"))
                .isInstanceOf(BusinessException.class).hasMessageContaining("propre rôle ADMIN");
        var persisted = users.findById(adminId).orElseThrow();
        assertThat(persisted.isEnabled()).isTrue();
        assertThat(persisted.getRole().getName()).isEqualTo("ADMIN");
    }

    @Test
    void dernierAdministrateurActifToujoursConserve() {
        // On isole ici l'invariant du dernier administrateur de la protection de son propre compte.
        assertThatThrownBy(() -> userService.modifier(adminId,
                utilisateur("admin", "", "CLIENT", true), "autre-administrateur"))
                .isInstanceOf(BusinessException.class).hasMessageContaining("administrateur actif");
        assertThat(users.countByRole_NameAndEnabledTrue("ADMIN")).isEqualTo(1);
    }

    @Test
    void deuxVentesConcurrentesNePeuventPasDepasserLeStock() throws Exception {
        var executor = Executors.newFixedThreadPool(2);
        var prets = new CountDownLatch(2);
        var depart = new CountDownLatch(1);
        Callable<Boolean> vendre = () -> {
            var context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(UsernamePasswordAuthenticationToken.authenticated("caisse", "unused",
                    List.of(new SimpleGrantedAuthority("ROLE_CAISSIER"))));
            SecurityContextHolder.setContext(context);
            try {
                prets.countDown();
                if (!depart.await(10, TimeUnit.SECONDS)) { throw new IllegalStateException("Départ expiré"); }
                venteService.enregistrer(medicamentId, 4, "caisse");
                return true;
            } catch (BusinessException ex) {
                if (!ex.getMessage().contains("Stock insuffisant")) { throw ex; }
                return false;
            } finally {
                SecurityContextHolder.clearContext();
            }
        };
        try {
            var premiere = executor.submit(vendre);
            var seconde = executor.submit(vendre);
            assertThat(prets.await(10, TimeUnit.SECONDS)).isTrue();
            depart.countDown();
            assertThat(List.of(premiere.get(15, TimeUnit.SECONDS), seconde.get(15, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(true, false);
            assertThat(ventes.count()).isEqualTo(1);
            assertThat(medicaments.findById(medicamentId).orElseThrow().getQuantite()).isEqualTo(1);
        } finally {
            depart.countDown();
            executor.shutdownNow();
            executor.awaitTermination(10, TimeUnit.SECONDS);
        }
    }

    private Role role(String name) { return roles.findByName(name).orElseThrow(); }

    private UserForm utilisateur(String username, String password, String roleName, boolean enabled) {
        var form = new UserForm();
        form.setUsername(username);
        form.setPassword(password);
        form.setRoleName(roleName);
        form.setEnabled(enabled);
        return form;
    }
}
