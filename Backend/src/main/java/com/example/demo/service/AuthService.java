package com.example.demo.service;

import com.example.demo.common.BusinessException;
import com.example.demo.config.AuthProperties;
import com.example.demo.dto.AuthDtos.LoginRequest;
import com.example.demo.dto.AuthDtos.LoginResponse;
import com.example.demo.dto.AuthDtos.UserDto;
import com.example.demo.model.UserProfile;
import com.example.demo.repository.UserProfileRepository;
import com.example.demo.security.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    private final UserProfileRepository profileRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthProperties authProperties;

    public AuthService(UserProfileRepository profileRepository, PasswordEncoder passwordEncoder,
                       JwtService jwtService, AuthProperties authProperties) {
        this.profileRepository = profileRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authProperties = authProperties;
    }

    public LoginResponse login(LoginRequest request) {
        UserProfile profile = profileRepository.findByEmailIgnoreCase(request.email())
                .orElseThrow(this::invalidCredentials);
        if (!passwordEncoder.matches(request.password(), profile.getPasswordHash())) {
            throw invalidCredentials();
        }
        return new LoginResponse(jwtService.createToken(profile), authProperties.tokenTtlSeconds(),
                new UserDto(profile.getStudentId(), profile.getFullName(), profile.getEmail(), profile.getRole()));
    }

    private BusinessException invalidCredentials() {
        return new BusinessException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS",
                "Email hoặc mật khẩu không đúng");
    }
}
