package com.booknest.gateway.filter;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;


/**
 * Integration-style tests for JwtAuthFilter using a real Spring context
 * so @Value("${jwt.secret}") is injected properly.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@TestPropertySource(properties = {
    "jwt.secret=ThisIsAVeryLongSecretKeyForTestingPurposes123456789",
    "spring.cloud.gateway.enabled=false",
    "eureka.client.enabled=false",
    "spring.main.web-application-type=reactive"
})
class JwtAuthFilterTest {

    @Autowired
    private JwtAuthFilter jwtAuthFilter;

    private final String validToken = generateValidToken();

    private static String generateValidToken() {
        try {
            String secret = "ThisIsAVeryLongSecretKeyForTestingPurposes123456789";
            byte[] keyBytes = secret.getBytes();
            io.jsonwebtoken.security.Keys.hmacShaKeyFor(keyBytes);
            return io.jsonwebtoken.Jwts.builder()
                    .setSubject("1")
                    .claim("role", "CUSTOMER")
                    .claim("email", "unit@test.com")
                    .setIssuedAt(new java.util.Date())
                    .setExpiration(new java.util.Date(System.currentTimeMillis() + 900_000))
                    .signWith(io.jsonwebtoken.security.Keys.hmacShaKeyFor(keyBytes))
                    .compact();
        } catch (Exception e) {
            return "fallback-token";
        }
    }

    @Test
    void filter_publicLoginPath_passesThrough() {
        MockServerHttpRequest request = MockServerHttpRequest.post("/api/auth/login").build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        Mono<Void> result = jwtAuthFilter.filter(exchange,
                ex -> Mono.empty());

        result.block();
        // Assert: no 401 was set — public path was allowed through
        assertThat(exchange.getResponse().getStatusCode()).isNotEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void filter_publicBooksGetPath_passesThrough() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/books").build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        jwtAuthFilter.filter(exchange, ex -> Mono.empty()).block();

        assertThat(exchange.getResponse().getStatusCode()).isNotEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void filter_securedPathNoToken_returns401() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/orders").build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        jwtAuthFilter.filter(exchange, ex -> Mono.empty()).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void filter_securedPathInvalidToken_returns401() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/orders")
                .header(HttpHeaders.AUTHORIZATION, "Bearer totally-invalid-jwt")
                .build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        jwtAuthFilter.filter(exchange, ex -> Mono.empty()).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void filter_securedPathValidToken_forwardsRequest() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/orders")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken)
                .build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        jwtAuthFilter.filter(exchange, ex -> Mono.empty()).block();

        // Valid token: status NOT 401 (chain.filter was called)
        assertThat(exchange.getResponse().getStatusCode()).isNotEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void filter_securedCartPath_requiresToken() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/cart")
                .build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        jwtAuthFilter.filter(exchange, ex -> Mono.empty()).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
