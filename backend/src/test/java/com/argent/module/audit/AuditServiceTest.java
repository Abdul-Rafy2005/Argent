package com.argent.module.audit;

import com.argent.common.exception.ForbiddenException;
import com.argent.common.exception.NotFoundException;
import com.argent.module.audit.dto.AuditLogResponse;
import com.argent.module.audit.entity.AuditLog;
import com.argent.module.audit.repository.AuditLogRepository;
import com.argent.module.audit.service.AuditService;
import com.argent.module.organization.entity.Organization;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditServiceTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @InjectMocks
    private AuditService auditService;

    private Organization org;
    private AuditLog auditLog;

    @BeforeEach
    void setUp() {
        org = Organization.builder().id(UUID.randomUUID()).build();
        auditLog = AuditLog.builder()
                .id(UUID.randomUUID())
                .organization(org)
                .entityType("WALLET")
                .entityId(UUID.randomUUID())
                .action("CREATED")
                .performedBy(UUID.randomUUID())
                .previousState(null)
                .newState(Map.of("label", "Test Wallet"))
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    void should_get_audit_log_by_id() {
        when(auditLogRepository.findById(auditLog.getId())).thenReturn(Optional.of(auditLog));

        AuditLogResponse response = auditService.getAuditLog(auditLog.getId(), org.getId(), null);

        assertThat(response).isNotNull();
        assertThat(response.entityType()).isEqualTo("WALLET");
        assertThat(response.action()).isEqualTo("CREATED");
    }

    @Test
    void should_throw_when_audit_log_not_found() {
        when(auditLogRepository.findById(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> auditService.getAuditLog(UUID.randomUUID(), org.getId(), null))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void should_throw_when_audit_log_wrong_organization() {
        Organization otherOrg = Organization.builder().id(UUID.randomUUID()).build();
        AuditLog otherLog = AuditLog.builder()
                .id(UUID.randomUUID())
                .organization(otherOrg)
                .entityType("WALLET")
                .entityId(UUID.randomUUID())
                .action("CREATED")
                .createdAt(LocalDateTime.now())
                .build();

        when(auditLogRepository.findById(otherLog.getId())).thenReturn(Optional.of(otherLog));

        assertThatThrownBy(() -> auditService.getAuditLog(otherLog.getId(), org.getId(), null))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void should_query_audit_logs_with_filters() {
        Page<AuditLog> page = new PageImpl<>(List.of(auditLog));
        when(auditLogRepository.findByFilters(
                eq(org.getId()), isNull(), eq("WALLET"), eq("CREATED"), isNull(),
                any(LocalDateTime.class), any(LocalDateTime.class), any(PageRequest.class)))
                .thenReturn(page);

        Page<AuditLogResponse> result = auditService.queryAuditLogs(
                org.getId(), null, "WALLET", "CREATED", null, null, null, 0, 20);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).entityType()).isEqualTo("WALLET");
    }

    @Test
    void should_return_empty_for_no_matches() {
        Page<AuditLog> emptyPage = new PageImpl<>(List.of());
        when(auditLogRepository.findByFilters(
                eq(org.getId()), isNull(), any(), any(), isNull(),
                any(LocalDateTime.class), any(LocalDateTime.class), any(PageRequest.class)))
                .thenReturn(emptyPage);

        Page<AuditLogResponse> result = auditService.queryAuditLogs(
                org.getId(), null, "NONEXISTENT", null, null, null, null, 0, 20);

        assertThat(result.getContent()).isEmpty();
    }
}
