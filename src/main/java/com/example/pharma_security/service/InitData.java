package com.example.pharma_security.service;

import com.example.pharma_security.model.Medicament;
import com.example.pharma_security.model.Role;
import com.example.pharma_security.model.User;
import com.example.pharma_security.repository.MedicamentRepository;
import com.example.pharma_security.repository.RoleRepository;
import com.example.pharma_security.repository.UserRepository;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Comptes du TP uniquement : ils ne sont jamais créés dans le profil par défaut. */
@Component
@Profile({"demo", "tp"})
public class InitData implements CommandLineRunner {
    private final RoleRepository roles;
    private final UserRepository users;
    private final MedicamentRepository medicaments;
    private final PasswordEncoder passwords;

    public InitData(RoleRepository roles, UserRepository users,
                    MedicamentRepository medicaments, PasswordEncoder passwords) {
        this.roles = roles;
        this.users = users;
        this.medicaments = medicaments;
        this.passwords = passwords;
    }

    @Override
    @Transactional
    public void run(String... args) {
        for (String name : UserService.ROLE_NAMES) {
            if (roles.findByName(name).isEmpty()) { roles.save(new Role(name)); }
        }
        creerCompte("admin", "admin123", "ADMIN");
        creerCompte("pharma", "pharma123", "PHARMACIEN");
        creerCompte("caisse", "caisse123", "CAISSIER");
        creerCompte("client", "client123", "CLIENT");
        if (medicaments.count() == 0) {
            medicaments.saveAll(List.of(
                    new Medicament("Paracétamol", "Boîte de démonstration A", new BigDecimal("12.50"), 40),
                    new Medicament("Vitamine C", "Boîte de démonstration B", new BigDecimal("25.00"), 20),
                    new Medicament("Produit en rupture", "Exemple de stock nul pour tester le rôle CLIENT", new BigDecimal("18.00"), 0)
            ));
        }
    }

    private void creerCompte(String username, String password, String roleName) {
        if (!users.existsByUsername(username)) {
            users.save(new User(username, passwords.encode(password), roles.findByName(roleName).orElseThrow()));
        }
    }
}
