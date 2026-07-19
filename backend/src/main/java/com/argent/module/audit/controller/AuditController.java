package com.argent.module.audit.controller;

import com.argent.common.response.ApiResponse;
import com.argent.common.response.PagedResponse;
import com.argent.module.audit.dto.AuditLogResponse;
import com.argent.module.audit.service.AuditService;
import com.argent.module.auth.security.CurrentUserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/audit-logs")
@RequiredArgsConstructor
public class AuditController {

    private final AuditService auditService;

    @GetMapping("/{id}")
    public ApiResponse<AuditLogResponse> getAuditLog(
            @PathVariable UUID id,
            Authentication authentication) {
        CurrentUserPrincipal principal = (CurrentUserPrincipal) authentication.getPrincipal();
        return ApiResponse.success(auditService.getAuditLog(id, principal.getOrganizationId(), principal.getEnvironment()));
    }

    @GetMapping
    public ApiResponse<PagedResponse<AuditLogResponse>> queryAuditLogs(
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) UUID performedBy,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            Authentication authentication) {
        CurrentUserPrincipal principal = (CurrentUserPrincipal) authentication.getPrincipal();
        Page<AuditLogResponse> auditPage = auditService.queryAuditLogs(
                principal.getOrganizationId(), principal.getEnvironment(), entityType, action, performedBy, startDate, endDate, page, pageSize);
        PagedResponse<AuditLogResponse> pagedResponse = PagedResponse.of(
                auditPage.getContent(), page, pageSize, auditPage.getTotalElements());
        return ApiResponse.success(pagedResponse, pagedResponse.toMeta());
    }
}
