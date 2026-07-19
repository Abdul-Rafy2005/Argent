package com.argent.module.auth.security;

import org.springframework.security.core.Authentication;

import java.util.UUID;

public class CurrentUser {
    private CurrentUser() {}

    public static UUID getId(Authentication authentication) {
        if (authentication.getPrincipal() instanceof CurrentUserPrincipal principal) {
            return principal.getId();
        }
        return null;
    }

    public static UUID getOrganizationId(Authentication authentication) {
        if (authentication.getPrincipal() instanceof CurrentUserPrincipal principal) {
            return principal.getOrganizationId();
        }
        return null;
    }

    public static String getEnvironment(Authentication authentication) {
        if (authentication.getPrincipal() instanceof CurrentUserPrincipal principal) {
            return principal.getEnvironment();
        }
        return null;
    }
}
