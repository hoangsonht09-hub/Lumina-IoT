package com.example.demo.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class ProfileDtos {
    private ProfileDtos() { }

    public record ProfileLinksDto(
            @NotBlank @Size(max = 500) String github,
            @NotBlank @Size(max = 500) String figma,
            @NotBlank @Size(max = 500) String projectDocs,
            @NotBlank @Size(max = 500) String apiDocs
    ) { }

    public record ProfileDto(
            String studentId, String fullName, String email, String organization,
            String practiceRoom, String role, int managedDevices, ProfileLinksDto links
    ) { }

    public record ProfileUpdateRequest(
            @NotBlank @Size(max = 50) String studentId,
            @NotBlank @Size(max = 120) String fullName,
            @NotBlank @Email @Size(max = 160) String email,
            @NotBlank @Size(max = 160) String organization,
            @NotNull @Valid ProfileLinksDto links
    ) { }
}
