package com.lengoga.webtech_projekt.service;

import com.lengoga.webtech_projekt.exception.UserAlreadyExistsException;
import com.lengoga.webtech_projekt.model.entity.PasswordResetToken;
import com.lengoga.webtech_projekt.model.entity.User;
import com.lengoga.webtech_projekt.repository.PasswordResetTokenRepository;
import com.lengoga.webtech_projekt.repository.UserRepository;
import com.lengoga.webtech_projekt.util.EmailValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public UserService(UserRepository userRepository,
                       PasswordResetTokenRepository tokenRepository,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // ===== BENUTZER FINDEN =====

    public List<User> getAllUsers() {
        List<User> users = userRepository.findAll();
        System.out.println("📋 Alle User geladen: " + users.size() + " gefunden");
        users.forEach(user ->
                System.out.println("  - ID: " + user.getId() + ", Username: " + user.getUsername() + ", Email: " + user.getEmail())
        );
        return users;
    }

    public Optional<User> findById(Long userId) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isPresent()) {
            System.out.println("✅ User gefunden: ID " + userId + " - " + userOpt.get().getUsername());
        } else {
            System.err.println("❌ User mit ID " + userId + " nicht gefunden");
            // Zeige verfügbare User IDs
            List<User> allUsers = userRepository.findAll();
            if (!allUsers.isEmpty()) {
                System.err.println("Verfügbare User IDs:");
                allUsers.forEach(user ->
                        System.err.println("  - ID: " + user.getId() + " (" + user.getUsername() + ")")
                );
            } else {
                System.err.println("❌ Keine User in der Datenbank gefunden!");
            }
        }
        return userOpt;
    }

    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    // ===== LOGIN VALIDIERUNG =====

    public boolean validateUser(Long userId) {
        if (userId == null) {
            System.err.println("❌ User-Validierung fehlgeschlagen: userId ist null");
            return false;
        }

        boolean exists = userRepository.existsById(userId);
        if (exists) {
            System.out.println("✅ User ID " + userId + " validiert");
        } else {
            System.err.println("❌ User ID " + userId + " existiert nicht");
            // Debug: Zeige verfügbare IDs
            List<User> allUsers = userRepository.findAll();
            System.err.println("Verfügbare User IDs: " +
                    allUsers.stream()
                            .map(u -> u.getId().toString())
                            .reduce((a, b) -> a + ", " + b)
                            .orElse("keine")
            );
        }
        return exists;
    }

    // Legacy Login-Methode (für UserController)
    public Optional<User> verifyLogin(String email, String password) {
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            if (passwordEncoder.matches(password, user.getPassword())) {
                System.out.println("✅ Login erfolgreich für User: " + user.getUsername() + " (ID: " + user.getId() + ")");
                return Optional.of(user);
            } else {
                System.err.println("❌ Login fehlgeschlagen: Falsches Passwort für " + email);
            }
        } else {
            System.err.println("❌ Login fehlgeschlagen: User mit Email " + email + " nicht gefunden");
        }
        return Optional.empty();
    }

    // ===== REGISTRIERUNG =====

    public User registerUser(String username, String email, String password) {
        // Validierung der Eingaben
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("Benutzername ist erforderlich");
        }
        if (password == null || password.length() < 6) {
            throw new IllegalArgumentException("Passwort muss mindestens 6 Zeichen haben");
        }

        // Email-Validierung nur wenn echte Email angegeben
        if (email != null && !email.contains("@watchlist.local") && !EmailValidator.isValid(email)) {
            throw new IllegalArgumentException("Ungültige E-Mail-Adresse");
        }

        // Prüfe ob Username bereits existiert
        if (userRepository.existsByUsername(username)) {
            throw new UserAlreadyExistsException("Benutzername bereits vergeben");
        }

        // Prüfe ob Email bereits existiert (nur bei echten Emails)
        if (email != null && !email.contains("@watchlist.local") && userRepository.existsByEmail(email)) {
            throw new UserAlreadyExistsException("E-Mail bereits registriert");
        }

        User user = new User(username, email, passwordEncoder.encode(password));
        User savedUser = userRepository.save(user);

        System.out.println("✅ Neuer User registriert: " + savedUser.getUsername() + " (ID: " + savedUser.getId() + ")");
        return savedUser;
    }

    // ===== PASSWORT RESET =====

    public void createPasswordResetTokenForUser(User user, String token) {
        // Lösche alte Tokens für diesen User
        try {
            tokenRepository.deleteByUser(user);
        } catch (Exception e) {
            // Ignoriere Fehler beim Löschen alter Tokens
        }

        PasswordResetToken myToken = new PasswordResetToken();
        myToken.setToken(token);
        myToken.setUser(user);
        myToken.setExpiryDate(LocalDateTime.now().plusHours(24));
        tokenRepository.save(myToken);
    }

    public String validatePasswordResetToken(String token) {
        PasswordResetToken passToken = tokenRepository.findByToken(token);
        if (passToken == null) {
            return "ungültigerToken";
        }

        if (passToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            tokenRepository.delete(passToken);
            return "abgelaufen";
        }

        return null;
    }

    public Optional<User> getUserByPasswordResetToken(String token) {
        PasswordResetToken resetToken = tokenRepository.findByToken(token);
        return Optional.ofNullable(resetToken != null ? resetToken.getUser() : null);
    }

    public void changeUserPassword(User user, String newPassword) {
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        try {
            tokenRepository.deleteByUser(user);
        } catch (Exception e) {
            // Ignoriere Fehler beim Löschen des Tokens
        }
    }

    // ===== DEBUG METHODEN =====

    public void debugUserState() {
        List<User> allUsers = getAllUsers();
        System.out.println("\n=== USER DEBUG INFO ===");
        System.out.println("Anzahl User in DB: " + allUsers.size());

        if (allUsers.isEmpty()) {
            System.out.println("❌ KEINE USER GEFUNDEN! Führe DataInitializer aus.");
        } else {
            System.out.println("Verfügbare User:");
            allUsers.forEach(user -> {
                System.out.println("  📱 ID: " + user.getId() +
                        " | Username: " + user.getUsername() +
                        " | Email: " + user.getEmail() +
                        " | Medien: " + user.getMediaList().size());
            });
        }
        System.out.println("========================\n");
    }
}