package com.lengoga.webtech_projekt;

import org.mindrot.jbcrypt.BCrypt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;

    @Autowired
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User registerUser(String username, String password) {
        if (userRepository.existsByUsername(username)) {
            throw new RuntimeException("Benutzername bereits vergeben");
        }

        // Passwort mit BCrypt verschlüsseln
        String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());
        User user = new User(username, hashedPassword);
        return userRepository.save(user);
    }

    public Optional<User> loginUser(String username, String password) {
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            // Passwort mit BCrypt überprüfen
            if (BCrypt.checkpw(password, user.getPassword())) {
                return userOpt;
            }
        }
        return Optional.empty();
    }

    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }

    public Optional<User> getUserByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public boolean validateUser(Long userId) {
        return userRepository.existsById(userId);
    }

    public User updateUser(Long userId, String newUsername, String newPassword) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isPresent()) {
            User user = userOpt.get();

            if (newUsername != null && !newUsername.trim().isEmpty()) {
                // Prüfen ob neuer Username bereits existiert
                if (!user.getUsername().equals(newUsername) &&
                        userRepository.existsByUsername(newUsername)) {
                    throw new RuntimeException("Benutzername bereits vergeben");
                }
                user.setUsername(newUsername);
            }

            if (newPassword != null && !newPassword.trim().isEmpty()) {
                String hashedPassword = BCrypt.hashpw(newPassword, BCrypt.gensalt());
                user.setPassword(hashedPassword);
            }

            return userRepository.save(user);
        }
        throw new RuntimeException("Benutzer nicht gefunden");
    }

    public void deleteUser(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new RuntimeException("Benutzer nicht gefunden");
        }
        userRepository.deleteById(userId);
    }
}