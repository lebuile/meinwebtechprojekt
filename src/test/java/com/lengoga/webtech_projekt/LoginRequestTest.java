package com.lengoga.webtech_projekt;

import com.lengoga.webtech_projekt.model.dto.LoginRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class LoginRequestTest {

    private LoginRequest loginRequest;

    @BeforeEach
    void setUp() {
        loginRequest = new LoginRequest();
    }

    // Testet das Setzen eines numerischen Passworts.
    @Test
    void testNumericPassword() {
        String numericPassword = "123456789";
        loginRequest.setPassword(numericPassword);

        assertEquals(numericPassword, loginRequest.getPassword());
        assertTrue(loginRequest.getPassword().matches("\\d+"));
    }

    // Testet das Setzen und Auslesen eines normalen Passworts.
    @Test
    void testSetAndGetPassword() {
        loginRequest.setPassword("password123");

        assertEquals("password123", loginRequest.getPassword());
    }

    // Testet, ob das Setzen von null als Passwort korrekt behandelt wird.
    @Test
    void testSetNullPassword() {
        loginRequest.setPassword(null);

        assertNull(loginRequest.getPassword());
    }

    // Testet das Setzen eines leeren Strings als Passwort.
    @Test
    void testSetEmptyPassword() {
        loginRequest.setPassword("");

        assertEquals("", loginRequest.getPassword());
    }

    // Testet das Setzen eines Passworts mit Sonderzeichen.
    @Test
    void testPasswordWithSpecialCharacters() {
        String specialPassword = "P@ssw0rd!#$%";
        loginRequest.setPassword(specialPassword);

        assertEquals(specialPassword, loginRequest.getPassword());
    }
}