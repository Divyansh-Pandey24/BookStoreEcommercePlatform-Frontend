package com.booknest.review.client;

import java.util.List;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import com.booknest.review.dto.OrderResponse;

// Feign client for interacting with the order-service to verify purchases.
// fallback = OrderClientFallback.class:
//   getMyOrders() returns empty list → review blocked (safe: no unverified reviews).
@FeignClient(name = "ORDER-SERVICE", fallback = OrderClientFallback.class)
public interface OrderClient {

    // Retrieve order history for the authenticated user
    @GetMapping("/orders/my")
    List<OrderResponse> getMyOrders(@RequestHeader("X-User-Id") Long userId);
}