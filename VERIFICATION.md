# Vérification de la correction

Vérification effectuée le 26 octobre 2026.

- Compilation de 25 classes Java avec une cible Java 17 : réussie.
- Environnement d'exécution utilisé : Temurin JDK 25.0.1.
- Tests des règles métier : 15 réussis.
- Tests de connexion, autorisation, CSRF, contrôleurs et rendu Thymeleaf : 58 réussis.
- Total : **73 tests réussis, 0 échec, 0 erreur, 0 test ignoré**.
- Création du JAR exécutable Spring Boot : réussie.
- Lanceur inclus : Maven Wrapper 3.3.4 et Maven 3.9.9, commande de version exécutée avec succès.
- Démonstration démarrée avec le profil `demo` ; connexion ADMIN et affichage du catalogue vérifiés dans le navigateur.

Les tests automatiques et la démonstration utilisent H2. Aucun serveur MySQL n'a été démarré ni validé dans cet environnement. La procédure MySQL et le profil `tp` sont fournis dans `README.md`.

Les rapports JUnit détaillés sont générés par `mvnw.cmd test` dans `target/surefire-reports/`.
