package com.booknest.auth.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class GatewaySecretFilter extends OncePerRequestFilter {

    @Value("${gateway.secret}")
    private String expectedSecret;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // Allow Swagger and OAuth2 endpoints to bypass Gateway Secret check
        String path = request.getRequestURI();
        if (path.contains("/v3/api-docs") || path.contains("/swagger-ui") || path.contains("/login/oauth2") || path.contains("/oauth2") || path.contains("/actuator")) {
            filterChain.doFilter(request, response);
            return;
        }

        String providedSecret = request.getHeader("X-Gateway-Secret");

        // 1. Prevent Header Spoofing
        if (providedSecret == null || !providedSecret.equals(expectedSecret)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("Access Denied: Missing or invalid Gateway Secret");
            return;
        }

        filterChain.doFilter(request, response);
    }
}
