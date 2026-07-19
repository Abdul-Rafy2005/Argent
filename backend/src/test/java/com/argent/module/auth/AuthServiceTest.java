package com.argent.module.auth;

import com.argent.common.exception.ConflictException;
import com.argent.common.exception.UnauthorizedException;
import com.argent.module.auth.dto.*;
import com.argent.module.auth.entity.ApiKey;
import com.argent.module.auth.entity.User;
import com.argent.module.auth.repository.ApiKeyRepository;
import com.argent.module.auth.repository.UserRepository;
import com.argent.module.auth.security.JwtUtil;
import com.argent.module.auth.service.AuthService;
import com.argent.module.audit.repository.AuditLogRepository;
import com.argent.module.organization.entity.Organization;
import com.argent.module.organization.repository.OrganizationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private ApiKeyRepository apiKeyRepository;

    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private AuthService authService;

    private User testUser;
    private Organization testOrg;

    @BeforeEach
    void setUp() {
        testOrg = Organization.builder()
                .id(UUID.randomUUID())
                .name("Test Org")
                .slug("test-org")
                .build();

        testUser = User.builder()
                .id(UUID.randomUUID())
                .organization(testOrg)
                .email("test@example.com")
                .name("Test User")
                .passwordHash("hashed-password")
                .role(User.Role.OWNER)
                .tokenVersion(0)
                .build();
    }

    @Test
    void should_register_user_and_create_organization() {
        RegisterRequest request = new RegisterRequest("test@example.com", "password123", "Test User", "Test Org");

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.empty());
        when(organizationRepository.existsBySlug("test-org")).thenReturn(false);
        when(organizationRepository.save(any(Organization.class))).thenReturn(testOrg);
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(passwordEncoder.encode("password123")).thenReturn("hashed-password");
        when(jwtUtil.generateAccessToken(testUser)).thenReturn("access-token");
        when(jwtUtil.generateRefreshToken(testUser)).thenReturn("refresh-token");

        AuthResponse response = authService.register(request);

        assertThat(response).isNotNull();
        assertThat(response.accessToken()).isEqualTo("access-token");
        assertThat(response.refreshToken()).isEqualTo("refresh-token");
        assertThat(response.user().email()).isEqualTo("test@example.com");
        assertThat(response.user().role()).isEqualTo(User.Role.OWNER);

        verify(userRepository).save(any(User.class));
        verify(organizationRepository).save(any(Organization.class));
        verify(auditLogRepository, atLeastOnce()).save(any());
    }

    @Test
    void should_throw_when_registering_duplicate_email() {
        RegisterRequest request = new RegisterRequest("test@example.com", "password123", "Test User", "Test Org");

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void should_login_with_valid_credentials() {
        LoginRequest request = new LoginRequest("test@example.com", "password123");

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("password123", "hashed-password")).thenReturn(true);
        when(jwtUtil.generateAccessToken(testUser)).thenReturn("access-token");
        when(jwtUtil.generateRefreshToken(testUser)).thenReturn("refresh-token");

        AuthResponse response = authService.login(request);

        assertThat(response).isNotNull();
        assertThat(response.accessToken()).isEqualTo("access-token");
        assertThat(response.refreshToken()).isEqualTo("refresh-token");
    }

    @Test
    void should_throw_on_login_with_wrong_password() {
        LoginRequest request = new LoginRequest("test@example.com", "wrong-password");

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("wrong-password", "hashed-password")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void should_throw_on_login_with_nonexistent_email() {
        LoginRequest request = new LoginRequest("nonexistent@example.com", "password123");

        when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void should_refresh_token_successfully() {
        RefreshTokenRequest request = new RefreshTokenRequest("refresh-token");

        when(jwtUtil.isTokenValid("refresh-token")).thenReturn(true);
        when(jwtUtil.isRefreshToken("refresh-token")).thenReturn(true);
        when(jwtUtil.extractUserId("refresh-token")).thenReturn(testUser.getId());
        when(jwtUtil.extractTokenVersion("refresh-token")).thenReturn(0);
        when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
        when(jwtUtil.generateAccessToken(testUser)).thenReturn("new-access-token");
        when(jwtUtil.generateRefreshToken(testUser)).thenReturn("new-refresh-token");

        AuthResponse response = authService.refresh(request);

        assertThat(response).isNotNull();
        assertThat(response.accessToken()).isEqualTo("new-access-token");
        assertThat(response.refreshToken()).isEqualTo("new-refresh-token");
        verify(userRepository).save(argThat(user -> user.getTokenVersion() == 1));
    }

    @Test
    void should_throw_on_invalid_refresh_token() {
        RefreshTokenRequest request = new RefreshTokenRequest("invalid-token");

        when(jwtUtil.isTokenValid("invalid-token")).thenReturn(false);

        assertThatThrownBy(() -> authService.refresh(request))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void should_throw_when_refresh_token_version_mismatch() {
        RefreshTokenRequest request = new RefreshTokenRequest("old-refresh-token");

        when(jwtUtil.isTokenValid("old-refresh-token")).thenReturn(true);
        when(jwtUtil.isRefreshToken("old-refresh-token")).thenReturn(true);
        when(jwtUtil.extractUserId("old-refresh-token")).thenReturn(testUser.getId());
        when(jwtUtil.extractTokenVersion("old-refresh-token")).thenReturn(0);

        testUser.setTokenVersion(1);
        when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));

        assertThatThrownBy(() -> authService.refresh(request))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("revoked");
    }

    @Test
    void should_invalidate_old_refresh_token_after_rotation() {
        RefreshTokenRequest request1 = new RefreshTokenRequest("refresh-token-1");

        when(jwtUtil.isTokenValid("refresh-token-1")).thenReturn(true);
        when(jwtUtil.isRefreshToken("refresh-token-1")).thenReturn(true);
        when(jwtUtil.extractUserId("refresh-token-1")).thenReturn(testUser.getId());
        when(jwtUtil.extractTokenVersion("refresh-token-1")).thenReturn(0);
        when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
        when(jwtUtil.generateAccessToken(testUser)).thenReturn("new-access-token");
        when(jwtUtil.generateRefreshToken(testUser)).thenReturn("new-refresh-token");

        authService.refresh(request1);

        verify(userRepository).save(argThat(user -> user.getTokenVersion() == 1));

        RefreshTokenRequest request2 = new RefreshTokenRequest("refresh-token-1");
        when(jwtUtil.isTokenValid("refresh-token-1")).thenReturn(true);
        when(jwtUtil.isRefreshToken("refresh-token-1")).thenReturn(true);
        when(jwtUtil.extractUserId("refresh-token-1")).thenReturn(testUser.getId());
        when(jwtUtil.extractTokenVersion("refresh-token-1")).thenReturn(0);

        when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));

        assertThatThrownBy(() -> authService.refresh(request2))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("revoked");
    }
}
