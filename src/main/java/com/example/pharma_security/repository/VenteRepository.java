package com.example.pharma_security.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import com.example.pharma_security.model.Vente;

public interface VenteRepository extends JpaRepository<Vente, Long> {
    List<Vente> findTop50ByOrderByDateVenteDesc();
    List<Vente> findTop50ByVendeur_UsernameOrderByDateVenteDesc(String username);
}
