package com.argent.module.auth;

import com.argent.common.exception.NotFoundException;
import com.argent.module.auth.dto.CreateApiKeyRequest;
import com.argent.module.auth.dto.ApiKeyResponse;
import com.argent.module.auth.entity.ApiKey;
import com.argent.module.auth.repository.ApiKeyRepository;
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
class ApiKeyServiceTest {

    @Mock
    private ApiKeyRepository apiKeyRepository;

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authService;

    private Organization testOrg;
    private ApiKey testApiKey;

    @BeforeEach
    void setUp() {
        testOrg = Organization.builder()
                .id(UUID.randomUUID())
                .name("Test Org")
                .slug("test-org")
                .build();

        testApiKey = ApiKey.builder()
                .id(UUID.randomUUID())
                .organization(testOrg)
                .name("Test Key")
                .keyHash("hashed-key")
                .keyPrefix("ak_test12")
                .environment(ApiKey.Environment.SANDBOX)
                .status(ApiKey.Status.ACTIVE)
                .build();
    }

    @Test
    void should_generate_api_key() {
        CreateApiKeyRequest request = new CreateApiKeyRequest("Test Key", ApiKey.Environment.SANDBOX);

        when(organizationRepository.findById(testOrg.getId())).thenReturn(Optional.of(testOrg));
        when(apiKeyRepository.save(any(ApiKey.class))).thenReturn(testApiKey);

        ApiKeyResponse response = authService.generateApiKey(testOrg.getId(), request);

        assertThat(response).isNotNull();
        assertThat(response.name()).isEqualTo("Test Key");
        assertThat(response.environment()).isEqualTo(ApiKey.Environment.SANDBOX);
        assertThat(response.status()).isEqualTo(ApiKey.Status.ACTIVE);
        assertThat(response.rawKey()).isNotBlank();

        verify(apiKeyRepository).save(any(ApiKey.class));
        verify(auditLogRepository).save(any());
    }

    @Test
    void should_throw_when_organization_not_found() {
        CreateApiKeyRequest request = new CreateApiKeyRequest("Test Key", ApiKey.Environment.SANDBOX);

        when(organizationRepository.findById(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.generateApiKey(UUID.randomUUID(), request))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void should_revoke_api_key() {
        when(apiKeyRepository.findById(testApiKey.getId())).thenReturn(Optional.of(testApiKey));
        when(apiKeyRepository.save(any(ApiKey.class))).thenReturn(testApiKey);

        authService.revokeApiKey(testOrg.getId(), testApiKey.getId());

        assertThat(testApiKey.getStatus()).isEqualTo(ApiKey.Status.REVOKED);
        verify(apiKeyRepository).save(testApiKey);
    }

    @Test
    void should_throw_when_revoking_nonexistent_key() {
        when(apiKeyRepository.findById(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.revokeApiKey(testOrg.getId(), UUID.randomUUID()))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void should_throw_when_revoking_key_from_different_organization() {
        ApiKey otherOrgKey = ApiKey.builder()
                .id(UUID.randomUUID())
                .organization(Organization.builder().id(UUID.randomUUID()).build())
                .name("Other Key")
                .keyHash("other-hash")
                .keyPrefix("ak_other1")
                .environment(ApiKey.Environment.SANDBOX)
                .status(ApiKey.Status.ACTIVE)
                .build();

        when(apiKeyRepository.findById(otherOrgKey.getId())).thenReturn(Optional.of(otherOrgKey));

        assertThatThrownBy(() -> authService.revokeApiKey(testOrg.getId(), otherOrgKey.getId()))
                .isInstanceOf(NotFoundException.class);
    }
}
