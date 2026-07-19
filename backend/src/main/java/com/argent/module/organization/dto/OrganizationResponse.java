package com.argent.module.organization.dto;

import com.argent.module.organization.entity.Organization;

import java.time.LocalDateTime;
import java.util.UUID;

public record OrganizationResponse(
    UUID id,
    String name,
    String slug,
    Organization.Status status,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
