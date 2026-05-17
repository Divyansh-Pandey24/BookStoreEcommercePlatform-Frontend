package com.booknest.gateway.filter;

import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

// Global filter for JWT authentication and authorization
@Component
public class JwtAuthFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthFilter.class);

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${gateway.secret}")
    private String gatewaySecret;

    // Paths that do not require authentication
    private static final List<String> PUBLIC_PATHS = List.of(
            "/api/auth/register",
            "/api/auth/login",
            "/api/auth/refresh",
            "/api/auth/forgot-password",
            "/api/auth/reset-password",
            // Book READ-ONLY public endpoints (browsing catalog)
            "/api/books/featured",
            "/api/books/search",
            "/api/books/genre",
            "/api/books/price-range",
            // Individual book detail page (public)
            "/api/books/detail",
            "/user",
            "/uploads",           // Book cover images (local)
            "/ebook-uploads",     // EBook PDFs and cover images (local)
            "/oauth2",
            "/login/oauth2",
            "/api/images",
            "/swagger-ui",
            "/v3/api-docs",
            "/webjars"
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        log.info("API Gateway Routing: {}", path);

        // Inject Gateway Secret into EVERY request
        ServerHttpRequest requestWithSecret = exchange.getRequest().mutate()
                .header("X-Gateway-Secret", gatewaySecret)
                .build();
        ServerWebExchange exchangeWithSecret = exchange.mutate().request(requestWithSecret).build();

        // Allow Swagger and utility paths
        if (path.contains("/v3/api-docs") || path.contains("/swagger-ui") || path.contains("/webjars") || path.contains("-docs/v3/api-docs")) {
            return chain.filter(exchangeWithSecret);
        }

        boolean isPublicPath = PUBLIC_PATHS.stream().anyMatch(publicPath -> path.startsWith(publicPath));

        // Allow GET-only access to /api/books (catalog browsing), /api/books/{id} (book detail), and /api/ebooks/public (ebook catalog)
        String method = exchange.getRequest().getMethod().name();
        if (!isPublicPath && (path.startsWith("/api/books") || path.startsWith("/api/ebooks/public")) && "GET".equals(method)) {
            isPublicPath = true;
        }

        if (isPublicPath) {
            // Forward user headers on public paths if valid token exists
            String authHeader = exchangeWithSecret.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                try {
                    Claims claims = Jwts.parserBuilder()
                        .setSigningKey(Keys.hmacShaKeyFor(jwtSecret.getBytes()))
                        .build().parseClaimsJws(token).getBody();

                    ServerHttpRequest modifiedRequest = exchangeWithSecret.getRequest().mutate()
                        .header("X-User-Id", claims.getSubject())
                        .header("X-User-Role", claims.get("role", String.class))
                        .header("X-User-Email", claims.get("email", String.class))
                        .build();
                    return chain.filter(exchangeWithSecret.mutate().request(modifiedRequest).build());
                } catch (Exception e) {
                    // Ignore invalid token on public paths
                }
            }
            return chain.filter(exchangeWithSecret);
        }

        // Validate JWT token for protected paths
        String authHeader = exchangeWithSecret.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("Gateway rejected request: Missing or Invalid Authorization Header for path: {}", path);
            exchangeWithSecret.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchangeWithSecret.getResponse().setComplete();
        }

        String token = authHeader.substring(7);
        try {
            // Parse and verify JWT claims
            Claims claims = Jwts.parserBuilder()
                .setSigningKey(Keys.hmacShaKeyFor(jwtSecret.getBytes()))
                .build()
                .parseClaimsJws(token)
                .getBody();

            // Forward user details as request headers
            ServerHttpRequest modifiedRequest = exchangeWithSecret.getRequest().mutate()
                .header("X-User-Id", claims.getSubject())
                .header("X-User-Role", claims.get("role", String.class))
                .header("X-User-Email", claims.get("email", String.class))
                .build();

            return chain.filter(exchangeWithSecret.mutate().request(modifiedRequest).build());
        } catch (Exception e) {
            log.warn("Gateway rejected request: JWT verification failed for path: {}. Error: {}", path, e.getMessage());
            exchangeWithSecret.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchangeWithSecret.getResponse().setComplete();
        }
    }

    // Set filter order to run first in the chain
    @Override
    public int getOrder() {
        return -1;
    }
}
