package com.lengoga.webtech_projekt;

import com.lengoga.webtech_projekt.model.entity.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

class UserTest {

    private String testUsername;
    private String testPassword;

    @BeforeEach
    void setUp() {
        testUsername = "testuser";
        testPassword = "password123";
    }

    // Dieser Test prüft, ob MediaList leer ist, wenn ein neuer User erstellt wird.
    @Test
    void testMediaListInitialization() {
        User user = new User();

        assertNotNull(user.getMediaList());
        assertEquals(0, user.getMediaList().size());
        assertTrue(user.getMediaList().isEmpty());
    }

    // Dieser Test prüft, ob der Benutzername korrekt gesetzt und zurückgegeben wird.
    @Test
    void testSetAndGetUsername() {
        User user = new User();

        user.setUsername(testUsername);

        assertEquals(testUsername, user.getUsername());
    }

    // Dieser Test prüft, ob die ID anfangs null ist.
    @Test
    void testGetId() {
        User user = new User();

        assertNull(user.getId());
    }

    // Dieser Test prüft, ob null-Werte korrekt gespeichert und zurückgegeben werden.
    @Test
    void testNullValues() {
        User user = new User();
        user.setUsername(null);
        user.setPassword(null);

        assertNull(user.getUsername());
        assertNull(user.getPassword());
    }

    // Dieser Test prüft, ob Benutzername und Passwort korrekt gesetzt werden und die ID null bleibt.
    @Test
    void testCompleteUserSetup() {
        User user = new User();
        user.setUsername(testUsername);
        user.setPassword(testPassword);

        assertEquals(testUsername, user.getUsername());
        assertEquals(testPassword, user.getPassword());
        assertNull(user.getId());
    }
}