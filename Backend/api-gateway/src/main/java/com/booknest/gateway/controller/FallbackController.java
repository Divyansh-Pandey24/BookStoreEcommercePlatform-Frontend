package com.booknest.gateway.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

// This controller acts as the central fallback handler within the Spring Cloud Gateway.
@RestController
public class FallbackController {

    // Handles and constructs the HTTP 503 SERVICE_UNAVAILABLE response when downstream microservices fail.
    @RequestMapping("/fallback/service-unavailable")
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public Mono<Map<String, Object>> serviceFallback() {
        // Initialize a local map to hold the structured JSON error response elements.
        Map<String, Object> response = new HashMap<>();
        
        // Define status as error to allow easy client-side error categorisation.
        response.put("status", "error");
        
        // Formulate a user-friendly message explaining the microservice downtime or timeout.
        response.put("message", "The requested service is currently unavailable or taking too long to respond. Please try again later.");
        
        // Set explicit HTTP status code 503 in the payload body matching the ResponseStatus annotation.
        response.put("code", 503);
        
        // Wrap the payload in a non-blocking reactive Mono container for high-throughput Gateway performance.
        return Mono.just(response);
    }
}
