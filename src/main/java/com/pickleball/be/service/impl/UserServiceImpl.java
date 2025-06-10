package com.pickleball.be.service.impl;

import com.pickleball.be.dto.auth.LoginRequest;
import com.pickleball.be.dto.auth.RegisterRequest;
import com.pickleball.be.model.User;
import com.pickleball.be.model.UserRole;
import com.pickleball.be.model.UserStatus;
import com.pickleball.be.repository.UserRepository;
import com.pickleball.be.security.JwtTokenProvider;
import com.pickleball.be.service.UserService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;

    public UserServiceImpl(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            JwtTokenProvider tokenProvider) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.tokenProvider = tokenProvider;
    }

    @Override
    public User registerUser(RegisterRequest registerRequest) {
        if (userRepository.existsByEmail(registerRequest.getEmail())) {
            throw new RuntimeException("Email is already taken!");
        }

        User user = new User();
        user.setEmail(registerRequest.getEmail());
        user.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
        user.setFullName(registerRequest.getFullName());
        user.setPhoneNumber(registerRequest.getPhoneNumber());
        user.setRole(registerRequest.getRole());

        return userRepository.save(user);
    }

    @Override
    public String authenticateUser(LoginRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(
                loginRequest.getEmail(),
                loginRequest.getPassword()
            )
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        return tokenProvider.generateToken(authentication);
    }

    @Override
    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        System.out.println("Current principal: " + authentication.getName());
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new UsernameNotFoundException("Current user not found"));
    }

    @Override
    public User updateUser(User user) {
        User existingUser = getUserById(user.getId());
        existingUser.setFullName(user.getFullName());
        existingUser.setPhoneNumber(user.getPhoneNumber());
        return userRepository.save(existingUser);
    }

    @Override
    public void deleteUser(Long userId) {
        userRepository.deleteById(userId);
    }

    @Override
    public User getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));
    }

    @Override
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    @Override
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @Override
    public List<User> getUsersByRole(UserRole role) {
        return userRepository.findByRole(role);
    }

    @Override
    public User updateUserStatus(Long userId, UserStatus status) {
        User user = getUserById(userId);
        user.setStatus(status);
        return userRepository.save(user);
    }

    @Override
    public User updateUserRole(Long userId, UserRole role) {
        User user = getUserById(userId);
        user.setRole(role);
        return userRepository.save(user);
    }
} 