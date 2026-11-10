package com.example.demo.service;

import com.example.demo.common.BusinessException;
import com.example.demo.dto.ProfileDtos.ProfileDto;
import com.example.demo.dto.ProfileDtos.ProfileUpdateRequest;
import com.example.demo.mapper.IotMapper;
import com.example.demo.model.UserProfile;
import com.example.demo.repository.UserProfileRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
public class ProfileService {
    private final UserProfileRepository repository;
    private final IotMapper mapper;

    public ProfileService(UserProfileRepository repository, IotMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public ProfileDto get(String userId) {
        return mapper.toProfileDto(find(userId));
    }

    @Transactional
    public ProfileDto update(String userId, ProfileUpdateRequest request) {
        UserProfile profile = find(userId);
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        repository.findByEmailIgnoreCase(email)
                .filter(existing -> !existing.getStudentId().equals(userId))
                .ifPresent(existing -> {
                    throw new BusinessException(HttpStatus.CONFLICT, "EMAIL_ALREADY_EXISTS",
                            "Email đã được sử dụng bởi tài khoản khác");
                });

        profile.setDisplayStudentId(request.studentId().trim());
        profile.setFullName(request.fullName().trim());
        profile.setEmail(email);
        profile.setOrganization(request.organization().trim());
        profile.setGithubUrl(normalizeLink(request.links().github()));
        profile.setFigmaUrl(normalizeLink(request.links().figma()));
        profile.setProjectDocsUrl(normalizeLink(request.links().projectDocs()));
        profile.setApiDocsUrl(normalizeLink(request.links().apiDocs()));
        return mapper.toProfileDto(profile);
    }

    private String normalizeLink(String value) {
        if (value == null || value.isBlank()) return "";
        String link = value.trim();
        String lowerLink = link.toLowerCase(Locale.ROOT);
        if (lowerLink.startsWith("https://") || lowerLink.startsWith("http://")
                || link.startsWith("/") || link.startsWith("#")) {
            return link;
        }
        throw new BusinessException(HttpStatus.BAD_REQUEST, "INVALID_PROFILE_LINK",
                "Liên kết phải bắt đầu bằng http://, https://, / hoặc #");
    }

    private UserProfile find(String userId) {
        return repository.findByUsername(userId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "PROFILE_NOT_FOUND", "Không tìm thấy hồ sơ"));
    }
}
