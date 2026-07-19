package com.argent.module.auth.security;

import com.argent.module.auth.entity.ApiKey;
import com.argent.module.auth.repository.ApiKeyRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

    private final ApiKeyRepository apiKeyRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String apiKey = request.getHeader("X-Api-Key");

        if (apiKey != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            String prefix = apiKey.substring(0, Math.min(8, apiKey.length()));
            String hash = hashKey(apiKey);

            apiKeyRepository.findByKeyPrefixAndStatus(prefix, ApiKey.Status.ACTIVE)
                    .filter(key -> key.getKeyHash().equals(hash))
                    .filter(key -> key.getExpiresAt() == null || key.getExpiresAt().isAfter(java.time.LocalDateTime.now()))
                    .ifPresent(key -> {
                        CurrentUserPrincipal principal = CurrentUserPrincipal.builder()
                                .id(key.getOrganization().getId())
                                .organizationId(key.getOrganization().getId())
                                .environment(key.getEnvironment().name())
                                .build();

                        UsernamePasswordAuthenticationToken authToken =
                                new UsernamePasswordAuthenticationToken(principal, null,
                                        List.of(new SimpleGrantedAuthority("ROLE_API_CLIENT")));
                        SecurityContextHolder.getContext().setAuthentication(authToken);
                    });
        }

        filterChain.doFilter(request, response);
    }

    private String hashKey(String key) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(key.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}
