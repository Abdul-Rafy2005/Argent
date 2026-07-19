package com.argent.module.wallet.controller;

import com.argent.common.response.ApiResponse;
import com.argent.common.response.PagedResponse;
import com.argent.module.auth.security.CurrentUserPrincipal;
import com.argent.module.wallet.dto.BalanceHistoryResponse;
import com.argent.module.wallet.dto.BalanceResponse;
import com.argent.module.wallet.entity.BalanceHistory;
import com.argent.module.wallet.service.BalanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/balances")
@RequiredArgsConstructor
public class BalanceController {

    private final BalanceService balanceService;

    @GetMapping("/{walletId}")
    public ApiResponse<BalanceResponse> getBalance(
            @PathVariable UUID walletId,
            Authentication authentication) {
        CurrentUserPrincipal principal = (CurrentUserPrincipal) authentication.getPrincipal();
        return ApiResponse.success(balanceService.getBalanceForWallet(walletId, principal.getOrganizationId(), principal.getEnvironment()));
    }

    @GetMapping("/{walletId}/history")
    public ApiResponse<PagedResponse<BalanceHistoryResponse>> getBalanceHistory(
            @PathVariable UUID walletId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            Authentication authentication) {
        CurrentUserPrincipal principal = (CurrentUserPrincipal) authentication.getPrincipal();
        Page<BalanceHistoryResponse> historyPage = balanceService.getBalanceHistory(walletId, principal.getOrganizationId(), principal.getEnvironment(), page, pageSize);
        PagedResponse<BalanceHistoryResponse> pagedResponse = PagedResponse.of(
                historyPage.getContent(), page, pageSize, historyPage.getTotalElements());
        return ApiResponse.success(pagedResponse, pagedResponse.toMeta());
    }
}
