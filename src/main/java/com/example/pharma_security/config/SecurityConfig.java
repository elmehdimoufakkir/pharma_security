package com.example.pharma_security.config;

import jakarta.servlet.DispatcherType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {
    @Bean
    PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(auth -> auth
                .dispatcherTypeMatchers(DispatcherType.ERROR, DispatcherType.FORWARD).permitAll()
                .requestMatchers("/login", "/css/**").permitAll()
                .requestMatchers("/", "/home", "/access-denied").authenticated()
                .requestMatchers("/admin", "/admin/**").hasRole("ADMIN")
                // La règle de lecture précise doit précéder /medicaments/**.
                .requestMatchers(HttpMethod.GET, "/medicaments").hasAnyRole("ADMIN", "PHARMACIEN", "CLIENT")
                .requestMatchers("/medicaments", "/medicaments/**").hasRole("ADMIN")
                .requestMatchers("/stocks", "/stocks/**").hasAnyRole("ADMIN", "PHARMACIEN")
                .requestMatchers("/ventes", "/ventes/**").hasAnyRole("ADMIN", "PHARMACIEN", "CAISSIER")
                .anyRequest().denyAll())
            .formLogin(form -> form.loginPage("/login").defaultSuccessUrl("/home", true).permitAll())
            .logout(logout -> logout.logoutSuccessUrl("/login?logout").permitAll())
            .exceptionHandling(errors -> errors.accessDeniedPage("/access-denied"));
        // CSRF reste activé. Thymeleaf insère son jeton dans les formulaires POST.
        return http.build();
    }
}
