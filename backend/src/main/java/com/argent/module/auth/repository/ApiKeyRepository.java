package com.argent.module.auth.repository;

import com.argent.module.auth.entity.ApiKey;
import com.argent.module.organization.entity.Organization;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ApiKeyRepository extends JpaRepository<ApiKey, UUID> {
    Optional<ApiKey> findByKeyPrefixAndStatus(String keyPrefix, ApiKey.Status status);
    List<ApiKey> findByOrganizationIdAndStatus(UUID organizationId, ApiKey.Status status);
    List<ApiKey> findByOrganizationId(UUID organizationId);
    List<ApiKey> findByOrganizationAndStatus(Organization organization, ApiKey.Status status);
}
