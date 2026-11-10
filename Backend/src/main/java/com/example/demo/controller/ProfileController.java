package com.example.demo.controller;

import com.example.demo.common.ApiResponse;
import com.example.demo.dto.ProfileDtos.ProfileDto;
import com.example.demo.dto.ProfileDtos.ProfileUpdateRequest;
import com.example.demo.service.ProfileService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {
    private final ProfileService service;

    public ProfileController(ProfileService service) { this.service = service; }

    @GetMapping
    public ApiResponse<ProfileDto> get(Authentication authentication) {
        return ApiResponse.ok(service.get(authentication.getName()));
    }

    @PatchMapping
    public ApiResponse<ProfileDto> update(Authentication authentication,
                                           @Valid @RequestBody ProfileUpdateRequest request) {
        return ApiResponse.ok("Cập nhật hồ sơ thành công", service.update(authentication.getName(), request));
    }
}
