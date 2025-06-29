package com.lengoga.webtech_projekt;

import com.lengoga.webtech_projekt.model.dto.RegisterRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class RegisterRequestTest {

    private RegisterRequest registerRequest;

    @BeforeEach
    void setUp() {
        registerRequest = new RegisterRequest();
    }

    // Dieser Test prüft das Setzen und Abrufen des Benutzernamens.
    @Test
    void testSetAndGetUsername() {
        registerRequest.setUsername("testuser");

        assertEquals("testuser", registerRequest.getUsername());
    }

    // Dieser Test prüft das Setzen und Abrufen des Passworts.
    @Test
    void testSetAndGetPassword() {
        registerRequest.setPassword("password123");

        assertEquals("password123", registerRequest.getPassword());
    }

    // Dieser Test prüft das gleichzeitige Setzen von Benutzername und Passwort.
    @Test
    void testSetUsernameAndPassword() {
        registerRequest.setUsername("john_doe");
        registerRequest.setPassword("securePass123");

        assertEquals("john_doe", registerRequest.getUsername());
        assertEquals("securePass123", registerRequest.getPassword());
    }

    // Dieser Test prüft das Verhalten beim Setzen von null-Werten.
    @Test
    void testSetNullValues() {
        registerRequest.setUsername(null);
        registerRequest.setPassword(null);

        assertNull(registerRequest.getUsername());
        assertNull(registerRequest.getPassword());
    }

    // Dieser Test prüft das Verhalten beim Setzen leerer Strings.
    @Test
    void testEmptyStringValues() {
        registerRequest.setUsername("");
        registerRequest.setPassword("");

        assertEquals("", registerRequest.getUsername());
        assertEquals("", registerRequest.getPassword());
    }
}