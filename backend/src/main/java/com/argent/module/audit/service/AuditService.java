package com.argent.module.audit.service;

import com.argent.common.exception.NotFoundException;
import com.argent.module.audit.dto.AuditLogResponse;
import com.argent.module.audit.entity.AuditLog;
import com.argent.module.audit.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    public AuditLogResponse getAuditLog(UUID auditLogId, UUID organizationId, String environment) {
        AuditLog auditLog = auditLogRepository.findById(auditLogId)
                .orElseThrow(() -> new NotFoundException("AuditLog", auditLogId.toString()));
        if (!auditLog.getOrganization().getId().equals(organizationId)) {
            throw new com.argent.common.exception.ForbiddenException("Audit log does not belong to this organization");
        }
        if (environment != null && auditLog.getEnvironment() != null && !environment.equals(auditLog.getEnvironment())) {
            throw new com.argent.common.exception.EnvironmentMismatchException(auditLog.getEnvironment(), environment);
        }
        return toResponse(auditLog);
    }

    public Page<AuditLogResponse> queryAuditLogs(
            UUID organizationId,
            String environment,
            String entityType,
            String action,
            UUID performedBy,
            LocalDateTime startDate,
            LocalDateTime endDate,
            int page,
            int pageSize) {

        Pageable pageable = PageRequest.of(page, pageSize, Sort.by(Sort.Direction.DESC, "createdAt"));

        if (startDate == null) {
            startDate = LocalDateTime.now().minusDays(30);
        }
        if (endDate == null) {
            endDate = LocalDateTime.now();
        }

        return auditLogRepository.findByFilters(
                organizationId, environment, entityType, action, performedBy, startDate, endDate, pageable
        ).map(this::toResponse);
    }

    private AuditLogResponse toResponse(AuditLog auditLog) {
        return new AuditLogResponse(
                auditLog.getId(),
                auditLog.getOrganization().getId(),
                auditLog.getEntityType(),
                auditLog.getEntityId(),
                auditLog.getAction(),
                auditLog.getPerformedBy(),
                auditLog.getPreviousState(),
                auditLog.getNewState(),
                auditLog.getIpAddress(),
                auditLog.getUserAgent(),
                auditLog.getCreatedAt()
        );
    }
}
