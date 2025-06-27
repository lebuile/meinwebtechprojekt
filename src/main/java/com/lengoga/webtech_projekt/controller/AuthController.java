package com.lengoga.webtech_projekt.controller;

import com.lengoga.webtech_projekt.service.EmailService;
import com.lengoga.webtech_projekt.model.entity.User;
import com.lengoga.webtech_projekt.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = {"http://localhost:5173", "https://meinvueprojekt2.onrender.com"},
        allowCredentials = "true")
public class AuthController {

    private final UserService userService;
    private final EmailService emailService;

    @Autowired
    public AuthController(UserService userService, EmailService emailService) {
        this.userService = userService;
        this.emailService = emailService;
    }

    // Bestehende register- und login-Methoden bleiben unverändert...

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody Map<String, String> request) {
        try {
            String email = request.get("email");
            Optional<User> userOpt = userService.findByEmail(email);

            // Sende immer die gleiche Antwort zurück, unabhängig davon ob der Benutzer existiert
            if (userOpt.isPresent()) {
                String token = UUID.randomUUID().toString();
                userService.createPasswordResetTokenForUser(userOpt.get(), token);
                emailService.sendPasswordResetEmail(email, token);
            }

            return ResponseEntity.ok().body(Map.of(
                    "message", "Falls ein Konto mit dieser E-Mail existiert, wurde eine E-Mail gesendet"
            ));
        } catch (Exception e) {
            // Log den Fehler, aber gib keine Details preis
            System.err.println("Fehler bei Passwort-Reset-Anfrage: " + e.getMessage());
            return ResponseEntity.ok().body(Map.of(
                    "message", "Falls ein Konto mit dieser E-Mail existiert, wurde eine E-Mail gesendet"
            ));
        }
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody Map<String, String> request) {
        try {
            String token = request.get("token");
            String newPassword = request.get("password");

            String validationResult = userService.validatePasswordResetToken(token);
            if (validationResult != null) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "Token ungültig oder abgelaufen"
                ));
            }

            Optional<User> user = userService.getUserByPasswordResetToken(token);
            if (user.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "Benutzer nicht gefunden"
                ));
            }

            userService.changeUserPassword(user.get(), newPassword);
            return ResponseEntity.ok().body(Map.of(
                    "message", "Passwort erfolgreich zurückgesetzt"
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "error", "Ein Fehler ist aufgetreten"
            ));
        }
    }
}