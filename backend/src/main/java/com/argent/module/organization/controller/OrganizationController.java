package com.argent.module.organization.controller;

import com.argent.common.response.ApiResponse;
import com.argent.module.auth.security.CurrentUserPrincipal;
import com.argent.module.organization.dto.AddMemberRequest;
import com.argent.module.organization.dto.CreateOrganizationRequest;
import com.argent.module.organization.dto.MemberResponse;
import com.argent.module.organization.dto.OrganizationResponse;
import com.argent.module.organization.service.OrganizationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/organizations")
@RequiredArgsConstructor
public class OrganizationController {

    private final OrganizationService organizationService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<OrganizationResponse> create(
            @Valid @RequestBody CreateOrganizationRequest request,
            Authentication authentication) {
        CurrentUserPrincipal principal = (CurrentUserPrincipal) authentication.getPrincipal();
        return ApiResponse.success(organizationService.create(request, principal.getId()));
    }

    @GetMapping("/{id}")
    public ApiResponse<OrganizationResponse> getById(@PathVariable UUID id, Authentication authentication) {
        CurrentUserPrincipal principal = (CurrentUserPrincipal) authentication.getPrincipal();
        return ApiResponse.success(organizationService.getById(id, principal.getOrganizationId()));
    }

    @GetMapping
    public ApiResponse<List<OrganizationResponse>> list(Authentication authentication) {
        CurrentUserPrincipal principal = (CurrentUserPrincipal) authentication.getPrincipal();
        return ApiResponse.success(organizationService.list(principal.getId()));
    }

    @PostMapping("/{id}/members")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<MemberResponse> addMember(
            @PathVariable UUID id,
            @Valid @RequestBody AddMemberRequest request,
            Authentication authentication) {
        CurrentUserPrincipal principal = (CurrentUserPrincipal) authentication.getPrincipal();
        return ApiResponse.success(organizationService.addMember(id, request, principal.getId()));
    }
}
