package com.booknest.order.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import com.booknest.order.exception.ResourceNotFoundException;
import org.springframework.transaction.annotation.Transactional;
import com.booknest.order.client.BookClient;
import com.booknest.order.client.CartClient;
import com.booknest.order.client.UserClient;
import com.booknest.order.client.WalletClient;
import com.booknest.order.dto.*;
import com.booknest.order.entity.Order;
import com.booknest.order.entity.OrderItem;
import com.booknest.order.event.OrderEventProducer;
import com.booknest.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// This service orchestrates the complex, distributed order placement transaction flow in BookNest.
@Service
@RequiredArgsConstructor
@Slf4j
public class OrderServiceImpl {

    /**
     * Repository used to perform SQL operations on customer orders.
     */
    private final OrderRepository orderRepository;

    /**
     * Feign client to query and clear user shopping carts.
     */
    private final CartClient cartClient;

    /**
     * Feign client to adjust and verify catalog stock levels.
     */
    private final BookClient bookClient;

    /**
     * Feign client to verify balances and process deductions/refunds.
     */
    private final WalletClient walletClient;

    /**
     * Kafka event producer used to publish transactional order status updates.
     */
    private final OrderEventProducer eventProducer;

    /**
     * Feign client to retrieve customer email and mobile details for alert notifications.
     */
    private final UserClient userClient;

    // Helper method to map an Order entity to a formatted OrderResponse DTO.
    private OrderResponse toResponse(Order order) {
        List<OrderItemResponse> itemResponses = new ArrayList<>();
        for (OrderItem item : order.getItems()) {
            OrderItemResponse r = new OrderItemResponse();
            r.setOrderItemId(item.getOrderItemId());
            r.setBookId(item.getBookId());
            r.setBookTitle(item.getBookTitle());
            r.setCoverImageUrl(item.getCoverImageUrl());
            r.setPrice(item.getPrice());
            r.setQuantity(item.getQuantity());
            r.setSubtotal(item.getSubtotal());
            itemResponses.add(r);
        }

        OrderResponse response = new OrderResponse();
        response.setOrderId(order.getOrderId());
        response.setUserId(order.getUserId());
        response.setPaymentMode(order.getPaymentMode());
        response.setTotalAmount(order.getTotalAmount());
        response.setOrderStatus(order.getOrderStatus());
        response.setDeliveryName(order.getDeliveryName());
        response.setDeliveryMobile(order.getDeliveryMobile());
        response.setDeliveryAddress(order.getDeliveryAddress());
        response.setDeliveryCity(order.getDeliveryCity());
        response.setDeliveryPincode(order.getDeliveryPincode());
        response.setDeliveryState(order.getDeliveryState());
        response.setPlacedAt(order.getPlacedAt());
        response.setUpdatedAt(order.getUpdatedAt());
        response.setItems(itemResponses);
        return response;
    }

    // Handles the distributed, step-by-step order placement and payment deduction flow.
    @Transactional
    public OrderResponse placeOrder(Long userId, PlaceOrderRequest request) {
        // Step 1: Validate payment method options.
        String mode = request.getPaymentMode().toUpperCase();
        if (!mode.equals("COD") && !mode.equals("WALLET")) {
            throw new RuntimeException("Invalid payment mode. Use COD or WALLET.");
        }

        // Step 2: Fetch the customer's current shopping cart.
        CartDto cart;
        try {
            cart = cartClient.getCart(userId);
        } catch (Exception e) {
            log.error("Cart fetch failed for user {}: {}", userId, e.getMessage());
            throw new RuntimeException("Could not fetch cart.");
        }

        if (cart.getItems() == null || cart.getItems().isEmpty()) {
            throw new RuntimeException("Cart is empty.");
        }

        double totalAmount = cart.getTotalPrice();

        // Step 3: Perform pre-order stock level checks.
        for (CartItemDto cartItem : cart.getItems()) {
            BookDto book;
            try {
                book = bookClient.getBookById(cartItem.getBookId());
            } catch (Exception e) {
                log.error("Book fetch failed for id {}: {}", cartItem.getBookId(), e.getMessage());
                throw new RuntimeException("Could not verify stock for '" + cartItem.getBookTitle() + "'.");
            }
            if (book.getStock() == null || book.getStock() < cartItem.getQuantity()) {
                throw new RuntimeException("Insufficient stock for '" + cartItem.getBookTitle() + "'.");
            }
        }

        // Step 4: Handle WALLET billing and deductions early in the flow.
        if (mode.equals("WALLET")) {
            WalletDto wallet;
            try {
                wallet = walletClient.getWallet(userId);
            } catch (Exception e) {
                throw new RuntimeException("Wallet access failed.");
            }

            if (wallet.getCurrentBalance() < totalAmount) {
                throw new RuntimeException("Insufficient wallet balance. Required: ₹" + totalAmount);
            }

            try {
                walletClient.deductMoney(userId, totalAmount);
                log.info("Wallet deducted for user {}", userId);
            } catch (Exception e) {
                log.error("Wallet deduction failed: {}", e.getMessage());
                throw new RuntimeException("Payment failed.");
            }
        }

        // Step 5: Reserve physical book stock, implementing rollback mechanisms on any failures.
        List<CartItemDto> reservedItems = new ArrayList<>();
        try {
            for (CartItemDto cartItem : cart.getItems()) {
                Boolean reserved = bookClient.reserveStock(cartItem.getBookId(), cartItem.getQuantity());
                if (reserved == null || !reserved) {
                    throw new RuntimeException("Stock reservation failed for '" + cartItem.getBookTitle() + "'.");
                }
                reservedItems.add(cartItem);
            }
        } catch (Exception e) {
            // COMPENSATING TRANSACTION: If any reservation step fails, release already reserved items and refund wallet money.
            log.warn("Rolling back stock reservation for {} items.", reservedItems.size());
            for (CartItemDto reserved : reservedItems) {
                try {
                    bookClient.releaseStock(reserved.getBookId(), reserved.getQuantity());
                } catch (Exception releaseEx) {
                    log.error("Critical: Stock release failed for book {}", reserved.getBookId());
                }
            }
            if (mode.equals("WALLET")) {
                try {
                    walletClient.addMoney(userId, totalAmount);
                } catch (Exception refundEx) {
                    log.error("Critical: Wallet refund failed for user {}", userId);
                }
            }
            throw new RuntimeException(e.getMessage());
        }

        // Step 6: Create and configure database Order entity structures.
        Order order = new Order();
        order.setUserId(userId);
        order.setPaymentMode(mode);
        order.setTotalAmount(totalAmount);
        order.setOrderStatus("PLACED");
        order.setDeliveryName(request.getDeliveryName());
        order.setDeliveryMobile(request.getDeliveryMobile());
        order.setDeliveryAddress(request.getDeliveryAddress());
        order.setDeliveryCity(request.getDeliveryCity());
        order.setDeliveryPincode(request.getDeliveryPincode());
        order.setDeliveryState(request.getDeliveryState());

        for (CartItemDto cartItem : cart.getItems()) {
            OrderItem item = new OrderItem();
            item.setBookId(cartItem.getBookId());
            item.setBookTitle(cartItem.getBookTitle());
            item.setCoverImageUrl(cartItem.getCoverImageUrl());
            item.setPrice(cartItem.getPrice());
            item.setQuantity(cartItem.getQuantity());
            item.setSubtotal(cartItem.getSubtotal());
            item.setOrder(order);
            order.getItems().add(item);
        }

        Order saved;
        try {
            saved = orderRepository.save(order);
        } catch (Exception e) {
            // COMPENSATING TRANSACTION: Full rollback on database save failures.
            log.error("Order save failed. Initiating full rollback.");
            for (CartItemDto cartItem : cart.getItems()) {
                try {
                    bookClient.releaseStock(cartItem.getBookId(), cartItem.getQuantity());
                } catch (Exception ex) {
                    log.error("Rollback failed for book {}", cartItem.getBookId());
                }
            }
            if (mode.equals("WALLET")) {
                try {
                    walletClient.addMoney(userId, totalAmount);
                } catch (Exception ex) {
                    log.error("Rollback failed for wallet user {}", userId);
                }
            }
            throw new RuntimeException("Finalizing order failed.");
        }

        log.info("Order saved: {}", saved.getOrderId());
        
        // Step 7: Publish status event to Kafka to trigger async notifications/receipts.
        publishEvent(userId, "ORDER_PLACED", "Order #" + saved.getOrderId() + " placed.", saved.getOrderId());

        // Step 8: Clear the user's cart asynchronously.
        try {
            cartClient.clearCart(userId);
        } catch (Exception e) {
            log.warn("Async cart clear failed for user {}", userId);
        }

        return toResponse(saved);
    }

    // Retrieves historical order listings for a specific user.
    public List<OrderResponse> getMyOrders(Long userId) {
        return orderRepository.findByUserIdOrderByPlacedAtDesc(userId).stream()
                .map(this::toResponse).toList();
    }

    // Retrieves details of a specific order, verifying owner/admin permissions.
    public OrderResponse getOrderById(Long orderId, Long userId, String role) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found."));
        if (!"ADMIN".equals(role) && !order.getUserId().equals(userId)) {
            throw new RuntimeException("Unauthorized access.");
        }
        return toResponse(order);
    }

    // Retrieves all active order transactions (Admin only).
    public List<OrderResponse> getAllOrders(String role) {
        if (!"ADMIN".equals(role)) throw new RuntimeException("Admin access required.");
        return orderRepository.findAll().stream().map(this::toResponse).toList();
    }

    // Filters orders by order status (Admin only).
    public List<OrderResponse> getOrdersByStatus(String status, String role) {
        if (!"ADMIN".equals(role)) throw new RuntimeException("Admin access required.");
        return orderRepository.findByOrderStatus(status.toUpperCase()).stream().map(this::toResponse).toList();
    }

    // Updates the status of an order (Admin only).
    @Transactional
    public OrderResponse updateOrderStatus(Long orderId, String newStatus, String role) {
        if (!"ADMIN".equals(role)) throw new RuntimeException("Admin access required.");
        if (!List.of("CONFIRMED", "DISPATCHED", "DELIVERED", "CANCELLED").contains(newStatus.toUpperCase())) {
            throw new RuntimeException("Invalid status update.");
        }

        String status = newStatus.toUpperCase();
        Order order = orderRepository.findById(orderId).orElseThrow(() -> new ResourceNotFoundException("Order not found."));
        
        if ("DELIVERED".equals(order.getOrderStatus()) && !newStatus.equals("DELIVERED")) {
            throw new RuntimeException("Delivered orders cannot be modified.");
        }

        // COMPENSATING TRANSACTION: If transitions to CANCELLED, refund the wallet and release reserved stock.
        if ("CANCELLED".equals(status) && !order.getOrderStatus().equals("CANCELLED")) {
            if ("WALLET".equals(order.getPaymentMode())) {
                try {
                    walletClient.addMoney(order.getUserId(), order.getTotalAmount());
                } catch (Exception e) {
                    throw new RuntimeException("Refund failed.");
                }
            }
            for (OrderItem item : order.getItems()) {
                try {
                    bookClient.releaseStock(item.getBookId(), item.getQuantity());
                } catch (Exception e) {
                    log.warn("Stock release failed for book {}", item.getBookId());
                }
            }
        }

        order.setOrderStatus(status);
        order.setUpdatedAt(LocalDateTime.now());
        Order updated = orderRepository.save(order);
        
        // Publish status event to Kafka.
        publishEvent(updated.getUserId(), status, "Order status updated to " + status, orderId);
        return toResponse(updated);
    }

    // Cancels a pending order (Customer self-service).
    @Transactional
    public OrderResponse cancelOrder(Long orderId, Long userId) {
        Order order = orderRepository.findById(orderId).orElseThrow(() -> new ResourceNotFoundException("Order not found."));
        if (!order.getUserId().equals(userId)) throw new RuntimeException("Unauthorized access.");
        if ("CANCELLED".equals(order.getOrderStatus())) {
            throw new RuntimeException("Order is already cancelled.");
        }

        if (List.of("DISPATCHED", "DELIVERED").contains(order.getOrderStatus())) {
            throw new RuntimeException("Cannot cancel order after dispatch.");
        }

        // COMPENSATING TRANSACTION: Refund WALLET payment if applicable.
        if ("WALLET".equals(order.getPaymentMode())) {
            try {
                walletClient.addMoney(userId, order.getTotalAmount());
            } catch (Exception e) {
                throw new RuntimeException("Refund failed.");
            }
        }
        
        // COMPENSATING TRANSACTION: Release reserved book stock back to catalog inventory.
        for (OrderItem item : order.getItems()) {
            try {
                bookClient.releaseStock(item.getBookId(), item.getQuantity());
            } catch (Exception e) {
                log.warn("Stock release failed for book {}", item.getBookId());
            }
        }

        order.setOrderStatus("CANCELLED");
        order.setUpdatedAt(LocalDateTime.now());
        Order updated = orderRepository.save(order);
        
        // Publish cancellation status event to Kafka.
        publishEvent(userId, "CANCELLED", "Order #" + orderId + " cancelled.", orderId);
        return toResponse(updated);
    }

    // Fetches customer contact profiles and publishes order status events to Kafka.
    private void publishEvent(Long userId, String type, String message, Long orderId) {
        String email = "", mobile = "";
        try {
            UserProfileDto user = userClient.getUserProfile(userId);
            email = user.getEmail();
            mobile = user.getMobile();
        } catch (Exception e) {
            log.warn("User detail fetch failed for notification");
        }
        eventProducer.sendOrderEvent(new OrderEventDto(userId, type, message, orderId, email, mobile));
    }
}

