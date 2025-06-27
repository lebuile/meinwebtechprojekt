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

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public void createPasswordResetTokenForUser(User user, String token) {
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
        tokenRepository.deleteByUser(user);
    }

    public User registerUser(String username, String email, String password) {
        if (!EmailValidator.isValid(email)) {
            throw new IllegalArgumentException("Ungültige E-Mail-Adresse");
        }

        if (userRepository.existsByEmail(email)) {
            throw new UserAlreadyExistsException("E-Mail bereits registriert");
        }
        if (userRepository.existsByUsername(username)) {
            throw new UserAlreadyExistsException("Benutzername bereits vergeben");
        }

        User user = new User(username, email, passwordEncoder.encode(password));
        return userRepository.save(user);
    }
}