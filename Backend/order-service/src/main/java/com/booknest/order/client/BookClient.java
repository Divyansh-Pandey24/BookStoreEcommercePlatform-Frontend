package com.booknest.order.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import com.booknest.order.dto.BookDto;

// Feign client for book-service.
// fallback = BookClientFallback.class: handles DOWN/OPEN circuit cases.
// reserveStock() fallback returns false → order rejected (safe for inventory).
// deductMoney() fallback throws → order rejected (safe for payments).
@FeignClient(name = "BOOK-SERVICE", fallback = BookClientFallback.class)
public interface BookClient {

    // Retrieve book details
    @GetMapping("/books/{bookId}")
    BookDto getBookById(@PathVariable Long bookId);

    // Reserve stock for an order
    @PostMapping("/books/{bookId}/reserve")
    Boolean reserveStock(@PathVariable Long bookId, @RequestParam Integer quantity);

    // Release reserved stock
    @PostMapping("/books/{bookId}/release")
    void releaseStock(@PathVariable Long bookId, @RequestParam Integer quantity);
}