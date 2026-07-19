package com.argent.module.auth.controller;

import com.argent.common.exception.ForbiddenException;
import com.argent.common.response.ApiResponse;
import com.argent.module.auth.dto.CreateApiKeyRequest;
import com.argent.module.auth.dto.ApiKeyResponse;
import com.argent.module.auth.entity.User;
import com.argent.module.auth.repository.UserRepository;
import com.argent.module.auth.security.CurrentUserPrincipal;
import com.argent.module.auth.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/api-keys")
@RequiredArgsConstructor
public class ApiKeyController {

    private final AuthService authService;
    private final UserRepository userRepository;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ApiKeyResponse> create(
            @Valid @RequestBody CreateApiKeyRequest request,
            Authentication authentication) {
        CurrentUserPrincipal principal = (CurrentUserPrincipal) authentication.getPrincipal();
        checkApiManagementAccess(principal.getId());

        ApiKeyResponse response = authService.generateApiKey(principal.getOrganizationId(), request);
        return ApiResponse.success(response);
    }

    @GetMapping
    public ApiResponse<List<ApiKeyResponse>> list(Authentication authentication) {
        CurrentUserPrincipal principal = (CurrentUserPrincipal) authentication.getPrincipal();
        return ApiResponse.success(authService.listApiKeys(principal.getOrganizationId()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revoke(@PathVariable UUID id, Authentication authentication) {
        CurrentUserPrincipal principal = (CurrentUserPrincipal) authentication.getPrincipal();
        checkApiManagementAccess(principal.getId());
        authService.revokeApiKey(principal.getOrganizationId(), id);
    }

    private void checkApiManagementAccess(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ForbiddenException("User not found"));

        if (user.getRole() != User.Role.OWNER && user.getRole() != User.Role.ADMIN) {
            throw new ForbiddenException("Only owners and admins can manage API keys");
        }
    }
}
