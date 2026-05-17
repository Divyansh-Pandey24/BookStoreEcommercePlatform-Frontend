package com.booknest.ebook.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class GatewaySecurityFilter extends OncePerRequestFilter {

    @Value("${gateway.secret}")
    private String gatewaySecret;

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(GatewaySecurityFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();
        log.info("Incoming request to ebook-service: {} {}", request.getMethod(), path);

        // Allow actuator paths for admin server
        if (path.startsWith("/actuator")) {
            filterChain.doFilter(request, response);
            return;
        }

        String requestSecret = request.getHeader("X-Gateway-Secret");
        if (requestSecret == null || !requestSecret.equals(gatewaySecret)) {
            log.warn("Access Denied for path {}: X-Gateway-Secret is missing or invalid. Received: {}", path, requestSecret);
            response.setStatus(HttpStatus.FORBIDDEN.value());
            response.getWriter().write("Access Denied: Direct access is not allowed");
            return;
        }

        filterChain.doFilter(request, response);
    }
}
