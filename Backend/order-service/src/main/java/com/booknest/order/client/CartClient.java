package com.booknest.order.client;

import com.booknest.order.dto.CartDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;

// Feign client for cross-service communication with the cart-service.
// fallback = CartClientFallback.class:
//   getCart() returns empty cart → order rejected (safe: no ghost orders).
//   clearCart() logs warning → stale cart is acceptable after order placed.
@FeignClient(name = "CART-SERVICE", fallback = CartClientFallback.class)
public interface CartClient {

    // Retrieve the shopping cart for a specific user
    @GetMapping("/cart")
    CartDto getCart(@RequestHeader("X-User-Id") Long userId);

    // Clear all items from a user's shopping cart
    @DeleteMapping("/cart/clear")
    void clearCart(@RequestHeader("X-User-Id") Long userId);
}