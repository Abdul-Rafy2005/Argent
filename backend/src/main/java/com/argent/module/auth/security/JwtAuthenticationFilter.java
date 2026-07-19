package com.argent.module.auth.security;

import com.argent.module.auth.entity.User;
import com.argent.module.auth.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");

        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);

            if (jwtUtil.isTokenValid(token) && !jwtUtil.isRefreshToken(token)) {
                UUID userId = jwtUtil.extractUserId(token);

                if (SecurityContextHolder.getContext().getAuthentication() == null) {
                    userRepository.findById(userId).ifPresent(user -> {
                        CurrentUserPrincipal principal = CurrentUserPrincipal.builder()
                                .id(user.getId())
                                .organizationId(user.getOrganization().getId())
                                .email(user.getEmail())
                                .password(user.getPasswordHash())
                                .role(user.getRole().name())
                                .build();

                        UsernamePasswordAuthenticationToken authToken =
                                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
                        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(authToken);
                    });
                }
            }
        }

        filterChain.doFilter(request, response);
    }
}
