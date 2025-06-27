package com.lengoga.webtech_projekt.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;
    private final String baseUrl; // Frontend-URL

    @Autowired
    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
        // Konfiguriere die Frontend-URL basierend auf der Umgebung
        this.baseUrl = "http://localhost:5173"; // Für Entwicklung
    }

    public void sendPasswordResetEmail(String to, String token) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("Passwort zurücksetzen");
        message.setText("Klicken Sie hier, um Ihr Passwort zurückzusetzen: "
                + baseUrl + "/reset-password?token=" + token);

        try {
            mailSender.send(message);
        } catch (Exception e) {
            // Log den Fehler, wirf aber keine Exception
            System.err.println("Fehler beim E-Mail-Versand: " + e.getMessage());
        }
    }
}