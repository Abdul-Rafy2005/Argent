package com.argent.module.auth.service;

import com.argent.common.exception.ConflictException;
import com.argent.common.exception.NotFoundException;
import com.argent.common.exception.UnauthorizedException;
import com.argent.common.exception.ValidationException;
import com.argent.module.auth.dto.*;
import com.argent.module.auth.entity.ApiKey;
import com.argent.module.auth.entity.User;
import com.argent.module.auth.repository.ApiKeyRepository;
import com.argent.module.auth.repository.UserRepository;
import com.argent.module.auth.security.JwtUtil;
import com.argent.module.audit.entity.AuditLog;
import com.argent.module.audit.repository.AuditLogRepository;
import com.argent.module.organization.entity.Organization;
import com.argent.module.organization.repository.OrganizationRepository;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final ApiKeyRepository apiKeyRepository;
    private final AuditLogRepository auditLogRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.findByEmail(request.email()).isPresent()) {
            throw new ConflictException("User", "email", request.email());
        }

        String slug = generateSlug(request.organizationName());
        if (organizationRepository.existsBySlug(slug)) {
            slug = slug + "-" + UUID.randomUUID().toString().substring(0, 6);
        }

        Organization organization = Organization.builder()
                .name(request.organizationName())
                .slug(slug)
                .build();
        organization = organizationRepository.save(organization);

        User user = User.builder()
                .organization(organization)
                .email(request.email())
                .name(request.name())
                .passwordHash(passwordEncoder.encode(request.password()))
                .role(User.Role.OWNER)
                .build();
        user = userRepository.save(user);

        auditLog(AuditLog.builder()
                .organization(organization)
                .entityType("USER")
                .entityId(user.getId())
                .action("REGISTERED")
                .performedBy(user.getId())
                .newState(ofMap("email", user.getEmail(), "role", user.getRole().name()))
                .build());

        auditLog(AuditLog.builder()
                .organization(organization)
                .entityType("ORGANIZATION")
                .entityId(organization.getId())
                .action("CREATED")
                .performedBy(user.getId())
                .newState(ofMap("name", organization.getName(), "slug", organization.getSlug()))
                .build());

        String accessToken = jwtUtil.generateAccessToken(user);
        String refreshToken = jwtUtil.generateRefreshToken(user);

        return new AuthResponse(
                accessToken,
                refreshToken,
                new AuthResponse.UserInfo(user.getId(), user.getEmail(), user.getName(), user.getRole(), organization.getId())
        );
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new UnauthorizedException("Invalid email or password");
        }

        String accessToken = jwtUtil.generateAccessToken(user);
        String refreshToken = jwtUtil.generateRefreshToken(user);

        auditLog(AuditLog.builder()
                .organization(user.getOrganization())
                .entityType("USER")
                .entityId(user.getId())
                .action("LOGGED_IN")
                .performedBy(user.getId())
                .build());

        return new AuthResponse(
                accessToken,
                refreshToken,
                new AuthResponse.UserInfo(user.getId(), user.getEmail(), user.getName(), user.getRole(), user.getOrganization().getId())
        );
    }

    public AuthResponse refresh(RefreshTokenRequest request) {
        String token = request.refreshToken();

        if (!jwtUtil.isTokenValid(token) || !jwtUtil.isRefreshToken(token)) {
            throw new UnauthorizedException("Invalid refresh token");
        }

        UUID userId = jwtUtil.extractUserId(token);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User", userId.toString()));

        int tokenVersion = jwtUtil.extractTokenVersion(token);
        if (tokenVersion != user.getTokenVersion()) {
            throw new UnauthorizedException("Refresh token has been revoked");
        }

        user.setTokenVersion(user.getTokenVersion() + 1);
        userRepository.save(user);

        String accessToken = jwtUtil.generateAccessToken(user);
        String refreshToken = jwtUtil.generateRefreshToken(user);

        return new AuthResponse(
                accessToken,
                refreshToken,
                new AuthResponse.UserInfo(user.getId(), user.getEmail(), user.getName(), user.getRole(), user.getOrganization().getId())
        );
    }

    public AuthResponse.UserInfo getMe(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User", userId.toString()));
        return new AuthResponse.UserInfo(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getRole(),
                user.getOrganization().getId()
        );
    }

    @Transactional
    public ApiKeyResponse generateApiKey(UUID organizationId, CreateApiKeyRequest request) {
        String rawKey = generateRawApiKey();
        String prefix = rawKey.substring(0, 8);
        String hash = hashKey(rawKey);

        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new NotFoundException("Organization", organizationId.toString()));

        ApiKey apiKey = ApiKey.builder()
                .organization(organization)
                .name(request.name())
                .keyHash(hash)
                .keyPrefix(prefix)
                .environment(request.environment())
                .build();
        apiKey = apiKeyRepository.save(apiKey);

        auditLog(AuditLog.builder()
                .organization(organization)
                .entityType("API_KEY")
                .entityId(apiKey.getId())
                .action("CREATED")
                .newState(ofMap("name", apiKey.getName(), "environment", apiKey.getEnvironment().name()))
                .build());

        return new ApiKeyResponse(
                apiKey.getId(),
                apiKey.getName(),
                apiKey.getKeyPrefix(),
                apiKey.getEnvironment(),
                apiKey.getStatus(),
                apiKey.getExpiresAt(),
                apiKey.getCreatedAt(),
                rawKey
        );
    }

    public List<ApiKeyResponse> listApiKeys(UUID organizationId) {
        return apiKeyRepository.findByOrganizationId(organizationId).stream()
                .map(k -> new ApiKeyResponse(
                        k.getId(),
                        k.getName(),
                        k.getKeyPrefix(),
                        k.getEnvironment(),
                        k.getStatus(),
                        k.getExpiresAt(),
                        k.getCreatedAt(),
                        null
                ))
                .toList();
    }

    @Transactional
    public void revokeApiKey(UUID organizationId, UUID apiKeyId) {
        ApiKey apiKey = apiKeyRepository.findById(apiKeyId)
                .filter(k -> k.getOrganization().getId().equals(organizationId))
                .orElseThrow(() -> new NotFoundException("API Key", apiKeyId.toString()));

        apiKey.setStatus(ApiKey.Status.REVOKED);
        apiKeyRepository.save(apiKey);

        auditLog(AuditLog.builder()
                .organization(apiKey.getOrganization())
                .entityType("API_KEY")
                .entityId(apiKey.getId())
                .action("REVOKED")
                .newState(ofMap("status", "REVOKED"))
                .build());
    }

    private String generateSlug(String name) {
        return name.toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "");
    }

    private String generateRawApiKey() {
        byte[] bytes = new byte[32];
        new java.security.SecureRandom().nextBytes(bytes);
        return "ak_" + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hashKey(String key) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(key.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    private void auditLog(AuditLog auditLog) {
        try {
            auditLogRepository.save(auditLog);
        } catch (Exception e) {
            // Log error but don't fail the main operation
        }
    }

    private static java.util.Map<String, Object> ofMap(String... pairs) {
        java.util.Map<String, Object> map = new java.util.HashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            map.put(pairs[i], pairs[i + 1]);
        }
        return map;
    }
}
