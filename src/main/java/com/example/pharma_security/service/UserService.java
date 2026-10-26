package com.example.pharma_security.service;

import com.example.pharma_security.dto.UserForm;
import com.example.pharma_security.exception.BusinessException;
import com.example.pharma_security.exception.NotFoundException;
import com.example.pharma_security.model.Role;
import com.example.pharma_security.model.User;
import com.example.pharma_security.repository.RoleRepository;
import com.example.pharma_security.repository.UserRepository;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@PreAuthorize("hasRole('ADMIN')")
@Transactional(readOnly = true)
public class UserService {
    public static final List<String> ROLE_NAMES = List.of("ADMIN", "PHARMACIEN", "CAISSIER", "CLIENT");
    private final UserRepository users;
    private final RoleRepository roles;
    private final PasswordEncoder passwords;

    public UserService(UserRepository users, RoleRepository roles, PasswordEncoder passwords) {
        this.users = users;
        this.roles = roles;
        this.passwords = passwords;
    }

    public List<User> lister() { return users.findAllByOrderByUsernameAsc(); }

    public User trouver(Long id) {
        return users.findById(id).orElseThrow(() -> new NotFoundException("Utilisateur introuvable."));
    }

    @Transactional
    public User creer(UserForm form) {
        verrouillerAdministration();
        verifierIdentifiant(form);
        verifierMotDePasse(form.getPassword(), true);
        if (users.existsByUsername(form.getUsername())) {
            throw new BusinessException("Ce nom d'utilisateur existe déjà.");
        }
        var user = new User(form.getUsername(), passwords.encode(form.getPassword()), role(form.getRoleName()));
        user.setEnabled(form.isEnabled());
        return users.save(user);
    }

    @Transactional
    public User modifier(Long id, UserForm form, String currentUsername) {
        // Tous les changements de comptes utilisent le même verrou pour préserver le dernier ADMIN.
        verrouillerAdministration();
        verifierIdentifiant(form);
        verifierMotDePasse(form.getPassword(), false);
        var user = users.findLockedById(id)
                .orElseThrow(() -> new NotFoundException("Utilisateur introuvable."));
        boolean soiMeme = user.getUsername().equals(currentUsername);
        if (soiMeme && (!form.isEnabled() || !"ADMIN".equals(form.getRoleName())
                || !user.getUsername().equals(form.getUsername()))) {
            throw new BusinessException("Vous ne pouvez pas désactiver, renommer ou retirer votre propre rôle ADMIN.");
        }
        boolean perdAccesAdmin = !form.isEnabled() || !"ADMIN".equals(form.getRoleName());
        if (user.isEnabled() && "ADMIN".equals(user.getRole().getName()) && perdAccesAdmin
                && users.countByRole_NameAndEnabledTrue("ADMIN") <= 1) {
            throw new BusinessException("Au moins un administrateur actif doit être conservé.");
        }
        users.findByUsername(form.getUsername()).filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> { throw new BusinessException("Ce nom d'utilisateur existe déjà."); });
        user.setUsername(form.getUsername());
        user.setRole(role(form.getRoleName()));
        user.setEnabled(form.isEnabled());
        if (form.getPassword() != null && !form.getPassword().isEmpty()) {
            user.setPassword(passwords.encode(form.getPassword()));
        }
        return user;
    }

    private void verrouillerAdministration() {
        roles.findLockedByName("ADMIN")
                .orElseThrow(() -> new BusinessException("Le rôle ADMIN doit être initialisé."));
    }

    private Role role(String name) {
        if (!ROLE_NAMES.contains(name)) { throw new BusinessException("Le rôle sélectionné est invalide."); }
        return roles.findByName(name).orElseThrow(() -> new BusinessException("Le rôle sélectionné n'existe pas."));
    }

    private void verifierIdentifiant(UserForm form) {
        if (form.getUsername() == null || !form.getUsername().matches("[a-zA-Z0-9._-]{3,50}")) {
            throw new BusinessException("Le nom d'utilisateur doit contenir 3 à 50 caractères autorisés.");
        }
    }

    private void verifierMotDePasse(String password, boolean obligatoire) {
        if (!obligatoire && (password == null || password.isEmpty())) { return; }
        if (password == null || password.length() < 8 || password.isBlank()) {
            throw new BusinessException("Le mot de passe doit contenir au moins 8 caractères.");
        }
        if (password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new BusinessException("Le mot de passe ne doit pas dépasser 72 octets UTF-8 (limite BCrypt).");
        }
    }
}
