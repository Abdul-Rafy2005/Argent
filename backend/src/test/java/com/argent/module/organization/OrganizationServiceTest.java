package com.argent.module.organization;

import com.argent.common.exception.ConflictException;
import com.argent.common.exception.ForbiddenException;
import com.argent.common.exception.NotFoundException;
import com.argent.module.audit.repository.AuditLogRepository;
import com.argent.module.auth.entity.User;
import com.argent.module.auth.repository.UserRepository;
import com.argent.module.organization.dto.AddMemberRequest;
import com.argent.module.organization.dto.CreateOrganizationRequest;
import com.argent.module.organization.dto.MemberResponse;
import com.argent.module.organization.dto.OrganizationResponse;
import com.argent.module.organization.entity.Organization;
import com.argent.module.organization.repository.OrganizationRepository;
import com.argent.module.organization.service.OrganizationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrganizationServiceTest {

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private OrganizationService organizationService;

    private Organization testOrg;
    private User ownerUser;
    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        testOrg = Organization.builder()
                .id(UUID.randomUUID())
                .name("Test Org")
                .slug("test-org")
                .build();
        ownerUser = User.builder()
                .id(userId)
                .organization(testOrg)
                .email("owner@test.com")
                .name("Owner")
                .passwordHash("hashed")
                .role(User.Role.OWNER)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    void should_create_organization() {
        CreateOrganizationRequest request = new CreateOrganizationRequest("Test Org", "test-org");

        when(organizationRepository.existsBySlug("test-org")).thenReturn(false);
        when(organizationRepository.save(any(Organization.class))).thenReturn(testOrg);

        OrganizationResponse response = organizationService.create(request, userId);

        assertThat(response).isNotNull();
        assertThat(response.name()).isEqualTo("Test Org");
        assertThat(response.slug()).isEqualTo("test-org");
        assertThat(response.status()).isEqualTo(Organization.Status.ACTIVE);

        verify(organizationRepository).save(any(Organization.class));
        verify(auditLogRepository).save(any());
    }

    @Test
    void should_throw_when_slug_already_exists() {
        CreateOrganizationRequest request = new CreateOrganizationRequest("Test Org", "test-org");

        when(organizationRepository.existsBySlug("test-org")).thenReturn(true);

        assertThatThrownBy(() -> organizationService.create(request, userId))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void should_get_organization_by_id() {
        when(organizationRepository.findById(testOrg.getId())).thenReturn(Optional.of(testOrg));

        OrganizationResponse response = organizationService.getById(testOrg.getId(), userId);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(testOrg.getId());
        assertThat(response.name()).isEqualTo("Test Org");
    }

    @Test
    void should_throw_when_organization_not_found() {
        when(organizationRepository.findById(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> organizationService.getById(UUID.randomUUID(), userId))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void should_add_member_as_owner() {
        AddMemberRequest request = new AddMemberRequest("newuser@test.com", User.Role.DEVELOPER, "password123");

        when(organizationRepository.findById(testOrg.getId())).thenReturn(Optional.of(testOrg));
        when(userRepository.findById(userId)).thenReturn(Optional.of(ownerUser));
        when(userRepository.findByEmail("newuser@test.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("password123")).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(UUID.randomUUID());
            user.setCreatedAt(LocalDateTime.now());
            return user;
        });

        MemberResponse response = organizationService.addMember(testOrg.getId(), request, userId);

        assertThat(response).isNotNull();
        assertThat(response.email()).isEqualTo("newuser@test.com");
        assertThat(response.role()).isEqualTo(User.Role.DEVELOPER);
        verify(userRepository).save(any(User.class));
        verify(auditLogRepository).save(any());
    }

    @Test
    void should_add_member_as_admin() {
        User adminUser = User.builder()
                .id(UUID.randomUUID())
                .organization(testOrg)
                .email("admin@test.com")
                .name("Admin")
                .passwordHash("hashed")
                .role(User.Role.ADMIN)
                .build();

        AddMemberRequest request = new AddMemberRequest("newuser@test.com", User.Role.DEVELOPER, "password123");

        when(organizationRepository.findById(testOrg.getId())).thenReturn(Optional.of(testOrg));
        when(userRepository.findById(adminUser.getId())).thenReturn(Optional.of(adminUser));
        when(userRepository.findByEmail("newuser@test.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("password123")).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(UUID.randomUUID());
            user.setCreatedAt(LocalDateTime.now());
            return user;
        });

        MemberResponse response = organizationService.addMember(testOrg.getId(), request, adminUser.getId());

        assertThat(response).isNotNull();
        assertThat(response.email()).isEqualTo("newuser@test.com");
    }

    @Test
    void should_reject_developer_from_adding_members() {
        User devUser = User.builder()
                .id(UUID.randomUUID())
                .organization(testOrg)
                .email("dev@test.com")
                .name("Dev")
                .passwordHash("hashed")
                .role(User.Role.DEVELOPER)
                .build();

        AddMemberRequest request = new AddMemberRequest("newuser@test.com", User.Role.DEVELOPER, "password123");

        when(organizationRepository.findById(testOrg.getId())).thenReturn(Optional.of(testOrg));
        when(userRepository.findById(devUser.getId())).thenReturn(Optional.of(devUser));

        assertThatThrownBy(() -> organizationService.addMember(testOrg.getId(), request, devUser.getId()))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void should_throw_when_member_email_already_exists() {
        AddMemberRequest request = new AddMemberRequest("existing@test.com", User.Role.DEVELOPER, "password123");

        when(organizationRepository.findById(testOrg.getId())).thenReturn(Optional.of(testOrg));
        when(userRepository.findById(userId)).thenReturn(Optional.of(ownerUser));
        when(userRepository.findByEmail("existing@test.com")).thenReturn(Optional.of(new User()));

        assertThatThrownBy(() -> organizationService.addMember(testOrg.getId(), request, userId))
                .isInstanceOf(ConflictException.class);
    }
}
