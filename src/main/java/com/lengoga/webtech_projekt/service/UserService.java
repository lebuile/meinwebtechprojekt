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

    public Optional<User> findById(Long userId) {
        return userRepository.findById(userId);
    }

    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    // ===== LOGIN VALIDIERUNG =====

    public boolean validateUser(Long userId) {
        return userRepository.existsById(userId);
    }

    // Legacy Login-Methode (für UserController)
    public Optional<User> verifyLogin(String email, String password) {
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            if (passwordEncoder.matches(password, user.getPassword())) {
                return Optional.of(user);
            }
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
        return userRepository.save(user);
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
}