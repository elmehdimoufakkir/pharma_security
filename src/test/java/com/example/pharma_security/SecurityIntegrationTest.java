package com.example.pharma_security;

import com.example.pharma_security.model.Medicament;
import com.example.pharma_security.model.Role;
import com.example.pharma_security.model.User;
import com.example.pharma_security.model.Vente;
import com.example.pharma_security.repository.MedicamentRepository;
import com.example.pharma_security.repository.RoleRepository;
import com.example.pharma_security.repository.UserRepository;
import com.example.pharma_security.repository.VenteRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Vérifie les filtres, les contrôleurs et le rendu réel des vues Thymeleaf. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class SecurityIntegrationTest {
    private static final String PASSWORD = "test12345";
    private static final Map<String, String> USERNAMES = Map.of(
            "ADMIN", "admin", "PHARMACIEN", "pharma", "CAISSIER", "caisse", "CLIENT", "client");

    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired RoleRepository roles;
    @Autowired MedicamentRepository medicaments;
    @Autowired VenteRepository ventes;
    @Autowired PasswordEncoder passwords;

    private Medicament disponible;
    private Medicament rupture;
    private Medicament archive;
    private User admin;
    private User caissier;

    @BeforeEach
    void preparerDonnees() {
        // Ordre respectant les clés étrangères ; chaque test est ensuite annulé.
        ventes.deleteAll();
        medicaments.deleteAll();
        users.deleteAll();
        roles.deleteAll();
        String hash = passwords.encode(PASSWORD);
        USERNAMES.forEach((roleName, username) -> {
            Role role = roles.save(new Role(roleName));
            users.save(new User(username, hash, role));
        });
        admin = users.findByUsername("admin").orElseThrow();
        caissier = users.findByUsername("caisse").orElseThrow();
        User disabled = new User("disabled", hash, roles.findByName("CLIENT").orElseThrow());
        disabled.setEnabled(false);
        users.save(disabled);
        disponible = medicaments.save(new Medicament("Disponible-Test", "Description", new BigDecimal("12.50"), 10));
        rupture = medicaments.save(new Medicament("Rupture-Test", "Description", new BigDecimal("5.00"), 0));
        archive = new Medicament("Archive-Test", "Description", new BigDecimal("8.00"), 5);
        archive.setActif(false);
        archive = medicaments.saveAndFlush(archive);
    }

    @Test
    void connexionEtCssSontPublicsEtLeFormulaireContientCsrf() throws Exception {
        mvc.perform(get("/login"))
                .andExpect(status().isOk()).andExpect(view().name("login"))
                .andExpect(content().string(containsString("name=\"_csrf\"")));
        mvc.perform(get("/css/style.css")).andExpect(status().isOk());
    }

    @ParameterizedTest
    @CsvSource({"admin, ADMIN", "pharma, PHARMACIEN", "caisse, CAISSIER", "client, CLIENT"})
    void connexionReelleUtiliseLesComptesEtRolesEnBase(String username, String role) throws Exception {
        assertThat(users.findByUsername(username).orElseThrow().getPassword()).isNotEqualTo(PASSWORD);
        mvc.perform(formLogin().user(username).password(PASSWORD))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/home"))
                .andExpect(authenticated().withUsername(username).withRoles(role));
    }

    @ParameterizedTest
    @CsvSource({"admin, incorrect", "inconnu, test12345", "disabled, test12345"})
    void identifiantsInvalidesEtCompteDesactiveSontRefuses(String username, String password) throws Exception {
        mvc.perform(formLogin().user(username).password(password))
                .andExpect(redirectedUrl("/login?error")).andExpect(unauthenticated());
    }

    @Test
    void deconnexionInvalideLaSession() throws Exception {
        MvcResult login = mvc.perform(formLogin().user("admin").password(PASSWORD))
                .andExpect(authenticated()).andReturn();
        MockHttpSession session = (MockHttpSession) login.getRequest().getSession(false);
        mvc.perform(post("/logout").session(session).with(csrf()))
                .andExpect(redirectedUrl("/login?logout")).andExpect(unauthenticated());
        assertThat(session.isInvalid()).isTrue();
    }

    @ParameterizedTest
    @CsvSource({"/home", "/admin", "/medicaments", "/stocks", "/ventes"})
    void visiteurEstRedirigeVersConnexion(String url) throws Exception {
        mvc.perform(get(url)).andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    static Stream<Arguments> matriceLecture() {
        Map<String, String> views = Map.of("/home", "home", "/admin", "admin",
                "/medicaments", "medicaments", "/stocks", "stocks", "/ventes", "ventes");
        return USERNAMES.keySet().stream().flatMap(role -> views.entrySet().stream().map(entry -> {
            boolean allowed = switch (entry.getKey()) {
                case "/home" -> true;
                case "/admin" -> role.equals("ADMIN");
                case "/medicaments" -> !role.equals("CAISSIER");
                case "/stocks" -> role.equals("ADMIN") || role.equals("PHARMACIEN");
                case "/ventes" -> !role.equals("CLIENT");
                default -> false;
            };
            return Arguments.of(role, entry.getKey(), entry.getValue(), allowed);
        }));
    }

    @ParameterizedTest(name = "{0} : GET {1}, autorisé={3}")
    @MethodSource("matriceLecture")
    void chaqueRoleRespecteLaMatriceDeLecture(String role, String url, String expectedView, boolean allowed)
            throws Exception {
        var result = mvc.perform(get(url).with(user(USERNAMES.get(role)).roles(role)));
        if (allowed) {
            result.andExpect(status().isOk()).andExpect(view().name(expectedView));
        } else {
            result.andExpect(status().isForbidden());
        }
    }

    @Test
    void formulairesDeCreationEtModificationAdminSontRendusSansExposerLeHash() throws Exception {
        for (String url : List.of("/admin/utilisateurs/nouveau", "/admin/utilisateurs/" + caissier.getId() + "/modifier")) {
            mvc.perform(get(url).with(user("admin").roles("ADMIN")))
                    .andExpect(status().isOk()).andExpect(view().name("user-form"))
                    .andExpect(model().attributeExists("userForm", "roleNames"))
                    .andExpect(content().string(not(containsString(admin.getPassword()))));
        }
        for (String url : List.of("/medicaments/nouveau", "/medicaments/" + disponible.getId() + "/modifier")) {
            mvc.perform(get(url).with(user("admin").roles("ADMIN")))
                    .andExpect(status().isOk()).andExpect(view().name("medicament-form"))
                    .andExpect(model().attributeExists("medicamentForm"));
        }
    }

    @ParameterizedTest
    @CsvSource({"CLIENT", "CAISSIER", "PHARMACIEN"})
    void autresRolesNePeuventPasOuvrirLesFormulairesAdmin(String role) throws Exception {
        for (String url : List.of("/admin/utilisateurs/nouveau", "/admin/utilisateurs/" + caissier.getId() + "/modifier",
                "/medicaments/nouveau", "/medicaments/" + disponible.getId() + "/modifier")) {
            mvc.perform(get(url).with(user(USERNAMES.get(role)).roles(role))).andExpect(status().isForbidden());
        }
    }

    @ParameterizedTest
    @CsvSource({"CLIENT", "CAISSIER", "PHARMACIEN"})
    void mutationsAdminSontInterditesMemeAvecUnJetonCsrfValide(String role) throws Exception {
        for (String url : List.of("/medicaments", "/medicaments/" + disponible.getId() + "/modifier",
                "/medicaments/" + disponible.getId() + "/supprimer", "/admin/utilisateurs",
                "/admin/utilisateurs/" + caissier.getId() + "/modifier")) {
            mvc.perform(post(url).with(user(USERNAMES.get(role)).roles(role)).with(csrf()))
                    .andExpect(status().isForbidden());
        }
        assertThat(disponible.isActif()).isTrue();
        assertThat(disponible.getQuantite()).isEqualTo(10);
    }

    @ParameterizedTest
    @CsvSource({"CLIENT", "CAISSIER"})
    void modificationStockRefuseeAuxRolesSansDroit(String role) throws Exception {
        mvc.perform(post("/stocks/{id}", disponible.getId()).param("quantite", "100")
                        .with(user(USERNAMES.get(role)).roles(role)).with(csrf()))
                .andExpect(status().isForbidden());
        assertThat(disponible.getQuantite()).isEqualTo(10);
    }

    @Test
    void clientNePeutPasEnregistrerDeVente() throws Exception {
        mvc.perform(post("/ventes").with(user("client").roles("CLIENT")).with(csrf())
                        .param("medicamentId", disponible.getId().toString()).param("quantite", "1"))
                .andExpect(status().isForbidden());
        assertThat(ventes.count()).isZero();
    }

    @Test
    void aucuneMutationNestAccepteeSansCsrfMemePourAdmin() throws Exception {
        for (String url : List.of("/medicaments", "/admin/utilisateurs", "/stocks/" + disponible.getId(), "/ventes", "/logout")) {
            mvc.perform(post(url).with(user("admin").roles("ADMIN")))
                    .andExpect(status().isForbidden());
        }
        assertThat(ventes.count()).isZero();
        assertThat(disponible.getQuantite()).isEqualTo(10);
    }

    @Test
    void pageInconnueEstInterditeParDefautEtPage403EstRendue() throws Exception {
        mvc.perform(get("/non-configure").with(user("admin").roles("ADMIN")))
                .andExpect(status().isForbidden());
        mvc.perform(get("/access-denied").with(user("client").roles("CLIENT")))
                .andExpect(status().isForbidden()).andExpect(view().name("access-denied"));
    }

    @Test
    void clientNeVoitNiRupturesNiArchivesNiActionsAdministration() throws Exception {
        MvcResult result = mvc.perform(get("/medicaments").with(user("client").roles("CLIENT")))
                .andExpect(status().isOk()).andExpect(view().name("medicaments"))
                .andExpect(content().string(not(containsString("/medicaments/nouveau"))))
                .andExpect(content().string(not(containsString("/supprimer"))))
                .andReturn();
        assertThat(listeMedicaments(result)).extracting(Medicament::getId).containsExactly(disponible.getId());
    }

    @ParameterizedTest
    @CsvSource({"ADMIN", "PHARMACIEN"})
    void personnelVoitAussiLesRupturesMaisPasLesArchives(String role) throws Exception {
        MvcResult result = mvc.perform(get("/medicaments").with(user(USERNAMES.get(role)).roles(role)))
                .andExpect(status().isOk()).andReturn();
        assertThat(listeMedicaments(result)).extracting(Medicament::getId)
                .containsExactlyInAnyOrder(disponible.getId(), rupture.getId());
    }

    @Test
    void adminPeutCreerModifierEtArchiverUnMedicament() throws Exception {
        mvc.perform(medicamentPost("/medicaments", "Nouveau-Test", "9.50", "3"))
                .andExpect(redirectedUrl("/medicaments"));
        Medicament nouveau = medicaments.findAll().stream().filter(m -> m.getNom().equals("Nouveau-Test"))
                .findFirst().orElseThrow();
        mvc.perform(medicamentPost("/medicaments/" + nouveau.getId() + "/modifier", "Modifie-Test", "10.00", "4"))
                .andExpect(redirectedUrl("/medicaments"));
        assertThat(nouveau.getNom()).isEqualTo("Modifie-Test");
        assertThat(nouveau.getPrix()).isEqualByComparingTo("10.00");
        mvc.perform(post("/medicaments/{id}/supprimer", nouveau.getId()).with(user("admin").roles("ADMIN")).with(csrf()))
                .andExpect(redirectedUrl("/medicaments"));
        assertThat(medicaments.findById(nouveau.getId()).orElseThrow().isActif()).isFalse();
    }

    @Test
    void adminPeutCreerEtModifierUnCompteAvecMotDePasseEncode() throws Exception {
        mvc.perform(post("/admin/utilisateurs").with(user("admin").roles("ADMIN")).with(csrf())
                        .param("username", "nouveau").param("password", "nouveau123").param("roleName", "CLIENT")
                        .param("enabled", "true"))
                .andExpect(redirectedUrl("/admin"));
        User nouveau = users.findByUsername("nouveau").orElseThrow();
        assertThat(passwords.matches("nouveau123", nouveau.getPassword())).isTrue();
        String hash = nouveau.getPassword();
        mvc.perform(post("/admin/utilisateurs/{id}/modifier", nouveau.getId())
                        .with(user("admin").roles("ADMIN")).with(csrf())
                        .param("username", "nouveau").param("password", "").param("roleName", "CAISSIER")
                        .param("enabled", "true"))
                .andExpect(redirectedUrl("/admin"));
        assertThat(nouveau.getRole().getName()).isEqualTo("CAISSIER");
        assertThat(nouveau.getPassword()).isEqualTo(hash);
    }

    @ParameterizedTest
    @CsvSource({"ADMIN", "PHARMACIEN"})
    void personnelAutorisePeutModifierLeStock(String role) throws Exception {
        mvc.perform(post("/stocks/{id}", disponible.getId()).with(user(USERNAMES.get(role)).roles(role)).with(csrf())
                        .param("quantite", "25"))
                .andExpect(redirectedUrl("/stocks"));
        assertThat(disponible.getQuantite()).isEqualTo(25);
    }

    @ParameterizedTest
    @CsvSource({"ADMIN", "PHARMACIEN", "CAISSIER"})
    void rolesAutorisesPeuventVendreEtLeServeurFixeLePrixEtLeVendeur(String role) throws Exception {
        String username = USERNAMES.get(role);
        mvc.perform(post("/ventes").with(user(username).roles(role)).with(csrf())
                        .param("medicamentId", disponible.getId().toString()).param("quantite", "2")
                        .param("prixUnitaire", "0.01").param("vendeur", "client"))
                .andExpect(redirectedUrl("/ventes"));
        assertThat(disponible.getQuantite()).isEqualTo(8);
        Vente vente = ventes.findAll().get(0);
        assertThat(vente.getPrixUnitaire()).isEqualByComparingTo("12.50");
        assertThat(vente.getTotal()).isEqualByComparingTo("25.00");
        assertThat(vente.getVendeur().getUsername()).isEqualTo(username);
        mvc.perform(get("/ventes").with(user(username).roles(role)))
                .andExpect(status().isOk()).andExpect(view().name("ventes"));
    }

    @Test
    void historiqueEstRenduAvecDonneesEtLimiteAuxVentesDuCaissier() throws Exception {
        Vente venteAdmin = ventes.save(new Vente(disponible, admin, 1));
        Vente venteCaisse = ventes.saveAndFlush(new Vente(disponible, caissier, 2));
        MvcResult caisse = mvc.perform(get("/ventes").with(user("caisse").roles("CAISSIER")))
                .andExpect(status().isOk()).andExpect(view().name("ventes"))
                .andExpect(model().attributeExists("medicaments", "venteForm", "ventes")).andReturn();
        assertThat(listeVentes(caisse)).extracting(Vente::getId).containsExactly(venteCaisse.getId());
        MvcResult adminPage = mvc.perform(get("/ventes").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk()).andExpect(view().name("ventes")).andReturn();
        assertThat(listeVentes(adminPage)).extracting(Vente::getId)
                .containsExactlyInAnyOrder(venteAdmin.getId(), venteCaisse.getId());
        assertThat(listeMedicaments(caisse)).extracting(Medicament::getId).containsExactly(disponible.getId());
    }

    @Test
    void erreursDeValidationReaffichentLesFormulairesComplets() throws Exception {
        mvc.perform(medicamentPost("/medicaments", "", "-1", "-1"))
                .andExpect(status().isOk()).andExpect(view().name("medicament-form"))
                .andExpect(model().attributeHasFieldErrors("medicamentForm", "nom", "prix", "quantite"));
        mvc.perform(post("/admin/utilisateurs").with(user("admin").roles("ADMIN")).with(csrf())
                        .param("username", "!").param("password", "abc").param("roleName", "ROOT"))
                .andExpect(status().isOk()).andExpect(view().name("user-form"))
                .andExpect(model().attributeHasFieldErrors("userForm", "username", "roleName"));
        mvc.perform(post("/ventes").with(user("caisse").roles("CAISSIER")).with(csrf())
                        .param("medicamentId", disponible.getId().toString()).param("quantite", "0"))
                .andExpect(status().isOk()).andExpect(view().name("ventes"))
                .andExpect(model().attributeHasFieldErrors("venteForm", "quantite"))
                .andExpect(model().attributeExists("medicaments", "ventes"));
    }

    private MockHttpServletRequestBuilder medicamentPost(String url, String nom, String prix, String quantite) {
        return post(url).with(user("admin").roles("ADMIN")).with(csrf())
                .param("nom", nom).param("description", "Description")
                .param("prix", prix).param("quantite", quantite);
    }

    @SuppressWarnings("unchecked")
    private List<Medicament> listeMedicaments(MvcResult result) {
        return (List<Medicament>) result.getModelAndView().getModel().get("medicaments");
    }

    @SuppressWarnings("unchecked")
    private List<Vente> listeVentes(MvcResult result) {
        return (List<Vente>) result.getModelAndView().getModel().get("ventes");
    }
}
