package com.argent.module.reporting.controller;

import com.argent.common.response.ApiResponse;
import com.argent.common.response.PagedResponse;
import com.argent.module.auth.security.CurrentUserPrincipal;
import com.argent.module.reporting.dto.DailyVolumeResponse;
import com.argent.module.reporting.dto.TransactionReportResponse;
import com.argent.module.reporting.dto.WalletGrowthResponse;
import com.argent.module.reporting.service.ReportingService;
import com.argent.module.reporting.service.StatementExportService;
import com.argent.module.transaction.entity.Transaction;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class ReportingController {

    private final ReportingService reportingService;
    private final StatementExportService statementExportService;

    @GetMapping("/api/v1/reports/daily-volume")
    public ApiResponse<List<DailyVolumeResponse>> getDailyVolume(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            Authentication authentication) {
        CurrentUserPrincipal principal = (CurrentUserPrincipal) authentication.getPrincipal();
        return ApiResponse.success(reportingService.getDailyVolume(principal.getOrganizationId(), principal.getEnvironment(), startDate, endDate));
    }

    @GetMapping("/api/v1/reports/wallet-growth")
    public ApiResponse<List<WalletGrowthResponse>> getWalletGrowth(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            Authentication authentication) {
        CurrentUserPrincipal principal = (CurrentUserPrincipal) authentication.getPrincipal();
        return ApiResponse.success(reportingService.getWalletGrowth(principal.getOrganizationId(), principal.getEnvironment(), startDate, endDate));
    }

    @GetMapping("/api/v1/reports/transactions")
    public ApiResponse<PagedResponse<TransactionReportResponse>> getTransactions(
            @RequestParam(required = false) Transaction.Type type,
            @RequestParam(required = false) Transaction.Status status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            Authentication authentication) {
        CurrentUserPrincipal principal = (CurrentUserPrincipal) authentication.getPrincipal();
        Page<TransactionReportResponse> txPage = reportingService.getTransactions(
                principal.getOrganizationId(), principal.getEnvironment(), type, status, startDate, endDate, page, pageSize);
        PagedResponse<TransactionReportResponse> pagedResponse = PagedResponse.of(
                txPage.getContent(), page, pageSize, txPage.getTotalElements());
        return ApiResponse.success(pagedResponse, pagedResponse.toMeta());
    }

    @GetMapping("/api/v1/statements")
    public ResponseEntity<String> exportStatement(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            Authentication authentication) {
        CurrentUserPrincipal principal = (CurrentUserPrincipal) authentication.getPrincipal();
        String csv = statementExportService.exportCsv(principal.getOrganizationId(), principal.getEnvironment(), startDate, endDate);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDispositionFormData("attachment", "statement.csv");
        return ResponseEntity.ok().headers(headers).body(csv);
    }
}
