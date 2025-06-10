package com.pickleball.be.controller;

import com.pickleball.be.dto.user.UserManagementResponse;
import com.pickleball.be.model.User;
import com.pickleball.be.model.UserRole;
import com.pickleball.be.model.UserStatus;
import com.pickleball.be.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final UserService userService;

    @GetMapping
    public ResponseEntity<List<UserManagementResponse>> getAllUsers() {
        List<User> users = userService.getAllUsers();
        List<UserManagementResponse> responses = users.stream()
                .map(UserManagementResponse::fromUser)
                .toList();
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/role/{role}")
    public ResponseEntity<List<UserManagementResponse>> getUsersByRole(@PathVariable UserRole role) {
        List<User> users = userService.getUsersByRole(role);
        List<UserManagementResponse> responses = users.stream()
                .map(UserManagementResponse::fromUser)
                .toList();
        return ResponseEntity.ok(responses);
    }

    @PutMapping("/{userId}/status")
    public ResponseEntity<UserManagementResponse> updateUserStatus(
            @PathVariable Long userId,
            @RequestParam UserStatus status) {
        User updatedUser = userService.updateUserStatus(userId, status);
        return ResponseEntity.ok(UserManagementResponse.fromUser(updatedUser));
    }

    @PutMapping("/{userId}/role")
    public ResponseEntity<UserManagementResponse> updateUserRole(
            @PathVariable Long userId,
            @RequestParam UserRole role) {
        User updatedUser = userService.updateUserRole(userId, role);
        return ResponseEntity.ok(UserManagementResponse.fromUser(updatedUser));
    }
} 