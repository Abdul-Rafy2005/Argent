package com.argent.module.wallet.controller;

import com.argent.common.response.ApiResponse;
import com.argent.common.response.PagedResponse;
import com.argent.module.auth.security.CurrentUser;
import com.argent.module.auth.security.CurrentUserPrincipal;
import com.argent.module.wallet.dto.CreateWalletRequest;
import com.argent.module.wallet.dto.UpdateWalletRequest;
import com.argent.module.wallet.dto.WalletResponse;
import com.argent.module.wallet.entity.Wallet;
import com.argent.module.wallet.service.WalletService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/wallets")
@RequiredArgsConstructor
public class WalletController {

    private final WalletService walletService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<WalletResponse> createWallet(
            @Valid @RequestBody CreateWalletRequest request,
            Authentication authentication) {
        CurrentUserPrincipal principal = (CurrentUserPrincipal) authentication.getPrincipal();
        Wallet.Environment environment = principal.getEnvironment() != null
                ? Wallet.Environment.valueOf(principal.getEnvironment())
                : Wallet.Environment.SANDBOX;
        return ApiResponse.success(walletService.createWallet(request, principal.getOrganizationId(), environment));
    }

    @GetMapping("/{id}")
    public ApiResponse<WalletResponse> getWallet(
            @PathVariable UUID id,
            Authentication authentication) {
        CurrentUserPrincipal principal = (CurrentUserPrincipal) authentication.getPrincipal();
        return ApiResponse.success(walletService.getWallet(id, principal.getOrganizationId(), principal.getEnvironment()));
    }

    @GetMapping
    public ApiResponse<PagedResponse<WalletResponse>> listWallets(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String environment,
            Authentication authentication) {
        CurrentUserPrincipal principal = (CurrentUserPrincipal) authentication.getPrincipal();
        Page<WalletResponse> walletPage = walletService.listWallets(principal.getOrganizationId(), page, pageSize, principal.getEnvironment() != null ? principal.getEnvironment() : environment);
        PagedResponse<WalletResponse> pagedResponse = PagedResponse.of(
                walletPage.getContent(), page, pageSize, walletPage.getTotalElements());
        return ApiResponse.success(pagedResponse, pagedResponse.toMeta());
    }

    @PatchMapping("/{id}")
    public ApiResponse<WalletResponse> updateWallet(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateWalletRequest request,
            Authentication authentication) {
        CurrentUserPrincipal principal = (CurrentUserPrincipal) authentication.getPrincipal();
        return ApiResponse.success(walletService.updateWallet(id, request, principal.getOrganizationId(), principal.getEnvironment()));
    }

    @PostMapping("/{id}/freeze")
    public ApiResponse<WalletResponse> freezeWallet(
            @PathVariable UUID id,
            Authentication authentication) {
        CurrentUserPrincipal principal = (CurrentUserPrincipal) authentication.getPrincipal();
        return ApiResponse.success(walletService.freezeWallet(id, principal.getOrganizationId(), principal.getEnvironment()));
    }

    @PostMapping("/{id}/unfreeze")
    public ApiResponse<WalletResponse> unfreezeWallet(
            @PathVariable UUID id,
            Authentication authentication) {
        CurrentUserPrincipal principal = (CurrentUserPrincipal) authentication.getPrincipal();
        return ApiResponse.success(walletService.unfreezeWallet(id, principal.getOrganizationId(), principal.getEnvironment()));
    }

    @PostMapping("/{id}/close")
    public ApiResponse<WalletResponse> closeWallet(
            @PathVariable UUID id,
            Authentication authentication) {
        CurrentUserPrincipal principal = (CurrentUserPrincipal) authentication.getPrincipal();
        return ApiResponse.success(walletService.closeWallet(id, principal.getOrganizationId(), principal.getEnvironment()));
    }
}
