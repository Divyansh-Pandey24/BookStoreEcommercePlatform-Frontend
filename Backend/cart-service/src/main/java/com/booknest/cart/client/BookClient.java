package com.booknest.cart.client;

import com.booknest.cart.dto.BookResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

// Feign client for cross-service communication with the book-service.
// fallback = BookClientFallback.class:
//   When BOOK-SERVICE is DOWN or the circuit OPENS (too many failures),
//   Resilience4j automatically calls BookClientFallback.getBookById()
//   instead of making a real HTTP call. No code change needed in CartService.
//   Requires feign.circuitbreaker.enabled=true in application.properties.
@FeignClient(name = "BOOK-SERVICE", fallback = BookClientFallback.class)
public interface BookClient {

    // Retrieve detailed book information from the book-service
    @GetMapping("/books/{bookId}")
    BookResponse getBookById(@PathVariable Long bookId);
}