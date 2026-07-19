package com.argent.module.transaction.controller;

import com.argent.common.response.ApiResponse;
import com.argent.common.response.PagedResponse;
import com.argent.module.auth.security.CurrentUserPrincipal;
import com.argent.module.transaction.dto.*;
import com.argent.module.transaction.entity.Transaction;
import com.argent.module.transaction.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    @PostMapping("/deposit")
    public ResponseEntity<ApiResponse<TransactionResponse>> deposit(
            @AuthenticationPrincipal CurrentUserPrincipal principal,
            @Valid @RequestBody DepositRequest request) {
        TransactionResponse response = transactionService.deposit(
                principal.getOrganizationId(), principal.getEnvironment(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response));
    }

    @PostMapping("/withdraw")
    public ResponseEntity<ApiResponse<TransactionResponse>> withdrawal(
            @AuthenticationPrincipal CurrentUserPrincipal principal,
            @Valid @RequestBody WithdrawalRequest request) {
        TransactionResponse response = transactionService.withdrawal(
                principal.getOrganizationId(), principal.getEnvironment(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response));
    }

    @PostMapping("/transfer")
    public ResponseEntity<ApiResponse<TransactionResponse>> transfer(
            @AuthenticationPrincipal CurrentUserPrincipal principal,
            @Valid @RequestBody TransferRequest request) {
        TransactionResponse response = transactionService.transfer(
                principal.getOrganizationId(), principal.getEnvironment(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response));
    }

    @PostMapping("/refund")
    public ResponseEntity<ApiResponse<TransactionResponse>> refund(
            @AuthenticationPrincipal CurrentUserPrincipal principal,
            @Valid @RequestBody RefundRequest request) {
        TransactionResponse response = transactionService.refund(
                principal.getOrganizationId(), principal.getEnvironment(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response));
    }

    @PostMapping("/adjust")
    public ResponseEntity<ApiResponse<TransactionResponse>> adjustment(
            @AuthenticationPrincipal CurrentUserPrincipal principal,
            @Valid @RequestBody AdjustmentRequest request) {
        TransactionResponse response = transactionService.adjustment(
                principal.getOrganizationId(), principal.getEnvironment(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TransactionResponse>> getById(
            @AuthenticationPrincipal CurrentUserPrincipal principal,
            @PathVariable UUID id) {
        TransactionResponse response = transactionService.getById(principal.getOrganizationId(), id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<TransactionResponse>>> list(
            @AuthenticationPrincipal CurrentUserPrincipal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<TransactionResponse> results = transactionService.list(principal.getOrganizationId(), page, size);
        return ResponseEntity.ok(ApiResponse.success(toPagedResponse(results)));
    }

    @GetMapping("/type/{type}")
    public ResponseEntity<ApiResponse<PagedResponse<TransactionResponse>>> listByType(
            @AuthenticationPrincipal CurrentUserPrincipal principal,
            @PathVariable Transaction.Type type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<TransactionResponse> results = transactionService.listByType(
                principal.getOrganizationId(), type, page, size);
        return ResponseEntity.ok(ApiResponse.success(toPagedResponse(results)));
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<ApiResponse<PagedResponse<TransactionResponse>>> listByStatus(
            @AuthenticationPrincipal CurrentUserPrincipal principal,
            @PathVariable Transaction.Status status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<TransactionResponse> results = transactionService.listByStatus(
                principal.getOrganizationId(), status, page, size);
        return ResponseEntity.ok(ApiResponse.success(toPagedResponse(results)));
    }

    @GetMapping("/date-range")
    public ResponseEntity<ApiResponse<PagedResponse<TransactionResponse>>> listByDateRange(
            @AuthenticationPrincipal CurrentUserPrincipal principal,
            @RequestParam java.time.LocalDateTime start,
            @RequestParam java.time.LocalDateTime end,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<TransactionResponse> results = transactionService.listByDateRange(
                principal.getOrganizationId(), start, end, page, size);
        return ResponseEntity.ok(ApiResponse.success(toPagedResponse(results)));
    }

    @GetMapping("/wallet/{walletId}")
    public ResponseEntity<ApiResponse<PagedResponse<TransactionResponse>>> listByWalletId(
            @AuthenticationPrincipal CurrentUserPrincipal principal,
            @PathVariable UUID walletId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<TransactionResponse> results = transactionService.listByWalletId(
                principal.getOrganizationId(), walletId, page, size);
        return ResponseEntity.ok(ApiResponse.success(toPagedResponse(results)));
    }

    private PagedResponse<TransactionResponse> toPagedResponse(Page<TransactionResponse> page) {
        return PagedResponse.<TransactionResponse>builder()
                .content(page.getContent())
                .page(page.getNumber())
                .pageSize(page.getSize())
                .total(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .hasNext(page.hasNext())
                .hasPrevious(page.hasPrevious())
                .build();
    }
}
