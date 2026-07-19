package com.argent.module.audit.repository;

import com.argent.module.audit.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.UUID;

public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

    Page<AuditLog> findByOrganizationId(UUID organizationId, Pageable pageable);

    Page<AuditLog> findByOrganizationIdAndEntityType(UUID organizationId, String entityType, Pageable pageable);

    Page<AuditLog> findByOrganizationIdAndAction(UUID organizationId, String action, Pageable pageable);

    Page<AuditLog> findByOrganizationIdAndPerformedBy(UUID organizationId, UUID performedBy, Pageable pageable);

    @Query("SELECT a FROM AuditLog a WHERE a.organization.id = :orgId AND a.createdAt BETWEEN :startDate AND :endDate")
    Page<AuditLog> findByOrganizationIdAndDateRange(
        @Param("orgId") UUID organizationId,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate,
        Pageable pageable
    );

    @Query("SELECT a FROM AuditLog a WHERE a.organization.id = :orgId AND (:env IS NULL OR a.environment = :env OR a.environment IS NULL) AND (:entityType IS NULL OR a.entityType = :entityType) AND (:action IS NULL OR a.action = :action) AND (:performedBy IS NULL OR a.performedBy = :performedBy) AND a.createdAt BETWEEN :startDate AND :endDate")
    Page<AuditLog> findByFilters(
        @Param("orgId") UUID organizationId,
        @Param("env") String environment,
        @Param("entityType") String entityType,
        @Param("action") String action,
        @Param("performedBy") UUID performedBy,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate,
        Pageable pageable
    );
}
