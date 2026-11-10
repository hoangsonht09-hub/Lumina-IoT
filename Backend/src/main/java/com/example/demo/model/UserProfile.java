package com.example.demo.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "users")
public class UserProfile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long databaseId;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(name = "student_code", nullable = false, length = 30)
    private String studentCode;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(length = 100)
    private String organization;

    @Column(name = "practice_room", length = 100)
    private String practiceRoom;

    @Column(name = "class_name", length = 50)
    private String className;

    @Column(length = 50)
    private String role;

    @Column(name = "managed_devices")
    private int managedDevices;

    @Column(name = "avatar_url", length = 255)
    private String avatarUrl;

    @Column(name = "github_url", length = 500)
    private String githubUrl;

    @Column(name = "figma_url", length = 500)
    private String figmaUrl;

    @Column(name = "postman_url", length = 500)
    private String postmanUrl;

    @Column(name = "report_url", length = 500)
    private String reportUrl;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @OneToMany(mappedBy = "user")
    private List<ActionLog> actions = new ArrayList<>();

    protected UserProfile() { }

    public UserProfile(String username, String fullName, String email, String organization,
                       String practiceRoom, String role, int managedDevices, String passwordHash) {
        this.username = username;
        this.studentCode = username;
        this.fullName = fullName;
        this.email = email;
        this.organization = organization;
        this.practiceRoom = practiceRoom;
        this.className = practiceRoom;
        this.role = role;
        this.managedDevices = managedDevices;
        this.passwordHash = passwordHash;
        this.createdAt = OffsetDateTime.now();
    }

    public Long getDatabaseId() { return databaseId; }
    public String getStudentId() { return username; }
    public String getUsername() { return username; }
    public String getDisplayStudentId() { return studentCode; }
    public String getStudentCode() { return studentCode; }
    public String getFullName() { return fullName; }
    public String getEmail() { return email; }
    public String getOrganization() { return organization; }
    public String getPracticeRoom() { return practiceRoom; }
    public String getClassName() { return className; }
    public String getRole() { return role; }
    public int getManagedDevices() { return managedDevices; }
    public String getPasswordHash() { return passwordHash; }
    public String getAvatarUrl() { return avatarUrl; }
    public String getGithubUrl() { return githubUrl; }
    public String getFigmaUrl() { return figmaUrl; }
    public String getProjectDocsUrl() { return reportUrl; }
    public String getApiDocsUrl() { return postmanUrl; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public List<ActionLog> getActions() { return actions; }
    public void setDisplayStudentId(String studentCode) { this.studentCode = studentCode; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public void setEmail(String email) { this.email = email; }
    public void setOrganization(String organization) { this.organization = organization; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public void setGithubUrl(String githubUrl) { this.githubUrl = githubUrl; }
    public void setFigmaUrl(String figmaUrl) { this.figmaUrl = figmaUrl; }
    public void setProjectDocsUrl(String reportUrl) { this.reportUrl = reportUrl; }
    public void setApiDocsUrl(String postmanUrl) { this.postmanUrl = postmanUrl; }
}
