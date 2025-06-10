package com.pickleball.be.service;

import com.pickleball.be.dto.auth.LoginRequest;
import com.pickleball.be.dto.auth.RegisterRequest;
import com.pickleball.be.model.User;
import com.pickleball.be.model.UserRole;
import com.pickleball.be.model.UserStatus;

import java.util.List;

public interface UserService {
    User registerUser(RegisterRequest registerRequest);
    String authenticateUser(LoginRequest loginRequest);
    User getCurrentUser();
    User updateUser(User user);
    void deleteUser(Long userId);
    User getUserById(Long userId);
    boolean existsByEmail(String email);
    
    // Admin methods
    List<User> getAllUsers();
    List<User> getUsersByRole(UserRole role);
    User updateUserStatus(Long userId, UserStatus status);
    User updateUserRole(Long userId, UserRole role);
} 