package com.argent.module.ledger.controller;

import com.argent.common.response.ApiResponse;
import com.argent.common.response.PagedResponse;
import com.argent.module.auth.security.CurrentUserPrincipal;
import com.argent.module.ledger.dto.LedgerEntryResponse;
import com.argent.module.ledger.dto.ReconciliationResponse;
import com.argent.module.ledger.service.LedgerEntryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/ledger")
@RequiredArgsConstructor
public class LedgerController {

    private final LedgerEntryService ledgerEntryService;

    @GetMapping("/entries")
    public ApiResponse<PagedResponse<LedgerEntryResponse>> listEntries(
            @RequestParam(required = false) UUID accountId,
            @RequestParam(required = false) UUID transactionId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            Authentication authentication) {

        CurrentUserPrincipal principal = (CurrentUserPrincipal) authentication.getPrincipal();
        Page<LedgerEntryResponse> entries = ledgerEntryService.listEntries(
                principal.getOrganizationId(), accountId, transactionId,
                startDate, endDate, principal.getEnvironment(), page, pageSize);

        PagedResponse<LedgerEntryResponse> pagedResponse = PagedResponse.of(
                entries.getContent(), page, pageSize, entries.getTotalElements());
        return ApiResponse.success(pagedResponse, pagedResponse.toMeta());
    }

    @GetMapping("/entries/{id}")
    public ApiResponse<LedgerEntryResponse> getEntry(
            @PathVariable UUID id,
            Authentication authentication) {
        CurrentUserPrincipal principal = (CurrentUserPrincipal) authentication.getPrincipal();
        return ApiResponse.success(ledgerEntryService.getEntry(id, principal.getOrganizationId(), principal.getEnvironment()));
    }

    @GetMapping("/reconcile")
    public ApiResponse<ReconciliationResponse> reconcile(
            @RequestParam UUID accountId,
            Authentication authentication) {
        CurrentUserPrincipal principal = (CurrentUserPrincipal) authentication.getPrincipal();
        return ApiResponse.success(ledgerEntryService.reconcile(accountId, principal.getOrganizationId()));
    }
}
