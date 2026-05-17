package com.booknest.gateway.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

/**
 * Fallback Controller for the API Gateway.
 * When a microservice is down or too slow, the CircuitBreaker filter
 * redirects the request here.
 */
@RestController
public class FallbackController {

    @RequestMapping("/fallback/service-unavailable")
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public Mono<Map<String, Object>> serviceFallback() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "error");
        response.put("message", "The requested service is currently unavailable or taking too long to respond. Please try again later.");
        response.put("code", 503);
        return Mono.just(response);
    }
}
