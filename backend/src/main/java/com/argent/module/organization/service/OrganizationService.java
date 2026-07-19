package com.argent.module.organization.service;

import com.argent.common.exception.ConflictException;
import com.argent.common.exception.ForbiddenException;
import com.argent.common.exception.NotFoundException;
import com.argent.common.exception.ValidationException;
import com.argent.module.audit.entity.AuditLog;
import com.argent.module.audit.repository.AuditLogRepository;
import com.argent.module.auth.entity.User;
import com.argent.module.auth.repository.UserRepository;
import com.argent.module.organization.dto.AddMemberRequest;
import com.argent.module.organization.dto.CreateOrganizationRequest;
import com.argent.module.organization.dto.MemberResponse;
import com.argent.module.organization.dto.OrganizationResponse;
import com.argent.module.organization.entity.Organization;
import com.argent.module.organization.repository.OrganizationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrganizationService {

    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;
    private final AuditLogRepository auditLogRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public OrganizationResponse create(CreateOrganizationRequest request, UUID userId) {
        if (organizationRepository.existsBySlug(request.slug())) {
            throw new ConflictException("Organization", "slug", request.slug());
        }

        Organization organization = Organization.builder()
                .name(request.name())
                .slug(request.slug())
                .build();
        organization = organizationRepository.save(organization);

        auditLog(AuditLog.builder()
                .organization(organization)
                .entityType("ORGANIZATION")
                .entityId(organization.getId())
                .action("CREATED")
                .performedBy(userId)
                .newState(ofMap("name", organization.getName(), "slug", organization.getSlug()))
                .build());

        return toResponse(organization);
    }

    public OrganizationResponse getById(UUID organizationId, UUID userId) {
        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new NotFoundException("Organization", organizationId.toString()));
        return toResponse(organization);
    }

    public List<OrganizationResponse> list(UUID userId) {
        return organizationRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public MemberResponse addMember(UUID organizationId, AddMemberRequest request, UUID performedBy) {
        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new NotFoundException("Organization", organizationId.toString()));

        User performer = userRepository.findById(performedBy)
                .orElseThrow(() -> new NotFoundException("User", performedBy.toString()));

        if (performer.getRole() != User.Role.OWNER && performer.getRole() != User.Role.ADMIN) {
            throw new ForbiddenException("Only owners and admins can add members");
        }

        if (userRepository.findByEmail(request.email()).isPresent()) {
            throw new ConflictException("User", "email", request.email());
        }

        User member = User.builder()
                .organization(organization)
                .email(request.email())
                .name(request.email().substring(0, request.email().indexOf('@')))
                .passwordHash(passwordEncoder.encode(request.password()))
                .role(request.role())
                .build();
        member = userRepository.save(member);

        auditLog(AuditLog.builder()
                .organization(organization)
                .entityType("USER")
                .entityId(member.getId())
                .action("MEMBER_ADDED")
                .performedBy(performedBy)
                .newState(ofMap("email", member.getEmail(), "role", member.getRole().name()))
                .build());

        return new MemberResponse(
                member.getId(),
                member.getEmail(),
                member.getName(),
                member.getRole(),
                member.getCreatedAt()
        );
    }

    private OrganizationResponse toResponse(Organization organization) {
        return new OrganizationResponse(
                organization.getId(),
                organization.getName(),
                organization.getSlug(),
                organization.getStatus(),
                organization.getCreatedAt(),
                organization.getUpdatedAt()
        );
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
