package com.lengoga.webtech_projekt.controller;

import com.lengoga.webtech_projekt.model.dto.LoginRequest;
import com.lengoga.webtech_projekt.model.dto.RegisterRequest;
import com.lengoga.webtech_projekt.model.entity.User;
import com.lengoga.webtech_projekt.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = {"http://localhost:5173"})
public class UserController {

    private final UserService userService;

    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody RegisterRequest request) {
        try {
            User user = userService.registerUser(
                    request.getUsername(),
                    request.getEmail(),
                    request.getPassword()
            );
            return ResponseEntity.ok(user);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> loginUser(@RequestBody LoginRequest request) {
        Optional<User> userOpt = userService.verifyLogin(request.getEmail(), request.getPassword());
        return userOpt
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.badRequest().body("Ungültige Anmeldedaten"));
    }
}