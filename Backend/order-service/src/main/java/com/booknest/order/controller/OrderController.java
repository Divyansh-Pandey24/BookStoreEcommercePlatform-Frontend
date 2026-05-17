package com.booknest.order.controller;

import com.booknest.order.dto.OrderResponse;
import com.booknest.order.dto.PlaceOrderRequest;
import com.booknest.order.service.OrderServiceImpl;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

// Controller for managing customer and admin order operations
@Slf4j
@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderServiceImpl orderService;

    // Place a new order for the authenticated user
    @PostMapping("/place")
    public ResponseEntity<OrderResponse> placeOrder(@RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody PlaceOrderRequest request) {
        log.info("Placing order for user: {}", userId);
        return ResponseEntity.status(201).body(orderService.placeOrder(userId, request));
    }

    // Retrieve order history for the authenticated user
    @GetMapping("/my")
    public ResponseEntity<List<OrderResponse>> getMyOrders(@RequestHeader("X-User-Id") Long userId) {
        log.info("Fetching order history for user: {}", userId);
        return ResponseEntity.ok(orderService.getMyOrders(userId));
    }

    // Retrieve details for a specific order
    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponse> getOrderById(@PathVariable Long orderId,
            @RequestHeader("X-User-Id") Long userId, @RequestHeader("X-User-Role") String role) {
        log.info("Fetching details for order: {}", orderId);
        return ResponseEntity.ok(orderService.getOrderById(orderId, userId, role));
    }

    // Cancel an existing order
    @DeleteMapping("/{orderId}/cancel")
    public ResponseEntity<OrderResponse> cancelOrder(@PathVariable Long orderId,
            @RequestHeader("X-User-Id") Long userId) {
        log.info("Cancelling order: {}", orderId);
        return ResponseEntity.ok(orderService.cancelOrder(orderId, userId));
    }

    // Admin: Retrieve all orders in the system
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/all")
    public ResponseEntity<List<OrderResponse>> getAllOrders(@RequestHeader("X-User-Role") String role) {
        log.info("Admin: Fetching all orders");
        return ResponseEntity.ok(orderService.getAllOrders(role));
    }

    // Admin: Filter orders by status
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/status/{status}")
    public ResponseEntity<List<OrderResponse>> getByStatus(@PathVariable String status,
            @RequestHeader("X-User-Role") String role) {
        log.info("Admin: Fetching orders with status: {}", status);
        return ResponseEntity.ok(orderService.getOrdersByStatus(status, role));
    }

    // Admin: Update the status of an order
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/admin/{orderId}/status")
    public ResponseEntity<OrderResponse> updateStatus(@PathVariable Long orderId,
            @RequestBody java.util.Map<String, String> body, @RequestHeader("X-User-Role") String role) {
        String newStatus = body.get("status");
        log.info("Admin: Updating order {} to status {}", orderId, newStatus);
        return ResponseEntity.ok(orderService.updateOrderStatus(orderId, newStatus, role));
    }
}
