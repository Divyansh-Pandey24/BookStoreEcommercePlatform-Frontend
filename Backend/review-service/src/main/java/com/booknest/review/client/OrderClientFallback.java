package com.booknest.review.client;

import com.booknest.review.dto.OrderResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

// ============================================================
// FALLBACK for review-service → ORDER-SERVICE calls.
//
// getMyOrders() is used to verify a user has purchased the book
// before they can leave a review. If ORDER-SERVICE is down,
// we return an empty list — which means the purchase check will
// FAIL and the review will be blocked.
//
// WHY block instead of allow?
//   Allowing unverified reviews would corrupt the review system.
//   Better to temporarily show "cannot verify purchase" than to
//   let anyone post a review. This is the safe default.
// ============================================================
@Component
public class OrderClientFallback implements OrderClient {

    private static final Logger log = LoggerFactory.getLogger(OrderClientFallback.class);

    // Called when ORDER-SERVICE is DOWN or circuit is OPEN.
    // Returns empty list → the review service will see "no orders"
    // and reject the review with a 400 "purchase not verified" message.
    @Override
    public List<OrderResponse> getMyOrders(Long userId) {
        log.warn("[CIRCUIT BREAKER] ORDER-SERVICE is unavailable. " +
                 "Cannot verify purchase for userId={}. " +
                 "Returning empty order list — review will be blocked.",
                 userId);
        // Collections.emptyList() is immutable and null-safe
        return Collections.emptyList();
    }
}
