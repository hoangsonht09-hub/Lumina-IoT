package com.example.demo.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public final class AuthDtos {
    private AuthDtos() { }

    public record LoginRequest(
            @NotBlank @Email String email,
            @NotBlank String password
    ) { }

    public record UserDto(String id, String fullName, String email, String role) { }

    public record LoginResponse(String accessToken, long expiresIn, UserDto user) { }
}
