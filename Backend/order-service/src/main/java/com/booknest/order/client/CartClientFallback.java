package com.booknest.order.client;

import com.booknest.order.dto.CartDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;

// ============================================================
// FALLBACK for order-service → CART-SERVICE calls.
//
//   getCart()   → Returns an empty CartDto.
//                 OrderService will see totalItems=0 and throw
//                 "cart is empty" — blocking the order.
//                 Better than placing an order against phantom data.
//
//   clearCart() → Called AFTER order is placed to remove cart items.
//                 If CART-SERVICE is down, the cart won't be cleared.
//                 We log a warning — the user will still have items
//                 in their cart even though the order went through.
//                 This is stale UI data, not a financial error.
//                 A cart-cleanup job or next login can fix it.
// ============================================================
@Component
public class CartClientFallback implements CartClient {

    private static final Logger log = LoggerFactory.getLogger(CartClientFallback.class);

    // Returns empty CartDto — OrderService sees 0 items and aborts.
    // Prevents placing orders against unknown cart contents.
    @Override
    public CartDto getCart(Long userId) {
        log.warn("[CIRCUIT BREAKER] CART-SERVICE unavailable. " +
                 "Returning empty CartDto for userId={}. " +
                 "Order will be rejected (empty cart).", userId);
        CartDto fallback = new CartDto();
        fallback.setUserId(userId);
        fallback.setTotalPrice(0.0);
        fallback.setTotalItems(0);
        // Empty list — OrderService will throw "cart is empty"
        fallback.setItems(Collections.emptyList());
        return fallback;
    }

    // Cart clear after order placement — non-blocking failure.
    // Log it and move on. Order is already placed and paid.
    @Override
    public void clearCart(Long userId) {
        log.warn("[CIRCUIT BREAKER] CART-SERVICE unavailable. " +
                 "ALERT: Cart NOT cleared for userId={}. " +
                 "User's cart may show stale items.", userId);
        // Return silently — void method, cart staleness is acceptable
    }
}
