package com.pickleball.be.controller;

import com.pickleball.be.dto.auth.JwtResponse;
import com.pickleball.be.dto.auth.LoginRequest;
import com.pickleball.be.dto.auth.RegisterRequest;
import com.pickleball.be.model.User;
import com.pickleball.be.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UserService userService;

    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        String jwt = userService.authenticateUser(loginRequest);
        User user = userService.getCurrentUser();
        
        return ResponseEntity.ok(new JwtResponse(
            jwt,
            user.getId(),
            user.getEmail(),
            user.getFullName(),
            user.getRole().name()
        ));
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody RegisterRequest registerRequest) {
        if (userService.existsByEmail(registerRequest.getEmail())) {
            return ResponseEntity
                .badRequest()
                .body("Error: Email is already taken!");
        }

        User user = userService.registerUser(registerRequest);
        return ResponseEntity.ok("User registered successfully!");
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser() {
        User user = userService.getCurrentUser();
        return ResponseEntity.ok(user);
    }
} 