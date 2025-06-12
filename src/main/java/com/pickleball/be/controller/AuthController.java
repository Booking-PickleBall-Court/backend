package com.pickleball.be.controller;

import com.pickleball.be.dto.auth.JwtResponse;
import com.pickleball.be.dto.auth.LoginRequest;
import com.pickleball.be.dto.auth.RegisterRequest;
import com.pickleball.be.model.User;
import com.pickleball.be.service.UserService;
import com.pickleball.be.dto.user.UserProfileUpdateRequest;
import com.pickleball.be.dto.user.UserManagementResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.pickleball.be.service.BookingService;
import com.pickleball.be.model.Booking;
import java.util.List;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UserService userService;

    @Autowired
    private BookingService bookingService;

    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        String jwt = userService.authenticateUser(loginRequest);
        User user = userService.getCurrentUser();
        return ResponseEntity.ok(new JwtResponse(
            jwt,
            user.getId(),
            user.getEmail(),
            user.getFullName(),
            user.getRole().name(),
            user.getAvatarUrl(),
            user.getPhoneNumber()
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
        List<Booking> bookings = bookingService.getBookingsByUser(user.getId());
        int paymentsMade = (int) bookings.stream().filter(b -> "PAID".equalsIgnoreCase(b.getPaymentStatus())).count();
        int bookingMinutes = bookings.stream()
            .filter(b -> "PAID".equalsIgnoreCase(b.getPaymentStatus()))
            .mapToInt(b -> (int) java.time.Duration.between(b.getStartTime(), b.getEndTime()).toMinutes())
            .sum();
        int bookingHours = bookingMinutes / 60;
        return ResponseEntity.ok(
            new java.util.HashMap<String, Object>() {{
                put("id", user.getId());
                put("email", user.getEmail());
                put("fullName", user.getFullName());
                put("role", user.getRole().name());
                put("avatarUrl", user.getAvatarUrl());
                put("phoneNumber", user.getPhoneNumber());
                put("paymentsMade", paymentsMade);
                put("bookingHours", bookingHours);
            }}
        );
    }

    @PutMapping("/updateProfile")
    public ResponseEntity<UserManagementResponse> updateProfile(@ModelAttribute UserProfileUpdateRequest request) {
        User updated = userService.updateProfile(request);
        return ResponseEntity.ok(UserManagementResponse.fromUser(updated));
    }
} 