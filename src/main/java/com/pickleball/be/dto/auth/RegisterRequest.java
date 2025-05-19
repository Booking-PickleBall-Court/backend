package com.pickleball.be.dto.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.pickleball.be.model.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class RegisterRequest {
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    @JsonProperty("email")
    private String email;

    @NotBlank(message = "Password is required")
    @JsonProperty("password")
    private String password;

    @NotBlank(message = "Full name is required")
    @JsonProperty("fullName")
    private String fullName;

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^\\+?[0-9]{10,15}$", message = "Invalid phone number format")
    @JsonProperty("phoneNumber")
    private String phoneNumber;

    @NotNull(message = "Role is required")
    @JsonProperty("role")
    private UserRole role;
} 