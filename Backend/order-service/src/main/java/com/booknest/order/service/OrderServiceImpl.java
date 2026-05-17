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

// Service implementation for managing order lifecycle and cross-service orchestration
@Service
@RequiredArgsConstructor
@Slf4j
public class OrderServiceImpl {

    private final OrderRepository orderRepository;
    private final CartClient cartClient;
    private final BookClient bookClient;
    private final WalletClient walletClient;
    private final OrderEventProducer eventProducer;
    private final UserClient userClient;

    // Convert Order entity to OrderResponse DTO
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

    // Process order placement with distributed transaction management
    @Transactional
    public OrderResponse placeOrder(Long userId, PlaceOrderRequest request) {
        // Validate payment method
        String mode = request.getPaymentMode().toUpperCase();
        if (!mode.equals("COD") && !mode.equals("WALLET")) {
            throw new RuntimeException("Invalid payment mode. Use COD or WALLET.");
        }

        // Fetch customer cart
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

        // Perform pre-order stock validation
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

        // Handle wallet deduction if selected
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

        // Reserve stock with compensating rollback logic
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
            // Compensating transaction: release reserved stock and refund wallet if applicable
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

        // Prepare and save order record
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
            // Full system rollback on database failure
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
        publishEvent(userId, "ORDER_PLACED", "Order #" + saved.getOrderId() + " placed.", saved.getOrderId());

        try {
            cartClient.clearCart(userId);
        } catch (Exception e) {
            log.warn("Async cart clear failed for user {}", userId);
        }

        return toResponse(saved);
    }

    // Retrieve user-specific order history
    public List<OrderResponse> getMyOrders(Long userId) {
        return orderRepository.findByUserIdOrderByPlacedAtDesc(userId).stream()
                .map(this::toResponse).toList();
    }

    // Find specific order with authorization checks
    public OrderResponse getOrderById(Long orderId, Long userId, String role) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found."));
        if (!"ADMIN".equals(role) && !order.getUserId().equals(userId)) {
            throw new RuntimeException("Unauthorized access.");
        }
        return toResponse(order);
    }

    // Fetch all orders in the system (Admin only)
    public List<OrderResponse> getAllOrders(String role) {
        if (!"ADMIN".equals(role)) throw new RuntimeException("Admin access required.");
        return orderRepository.findAll().stream().map(this::toResponse).toList();
    }

    // Filter orders by status (Admin only)
    public List<OrderResponse> getOrdersByStatus(String status, String role) {
        if (!"ADMIN".equals(role)) throw new RuntimeException("Admin access required.");
        return orderRepository.findByOrderStatus(status.toUpperCase()).stream().map(this::toResponse).toList();
    }

    // Update order status with side effects like refunding and stock release
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
        publishEvent(updated.getUserId(), status, "Order status updated to " + status, orderId);
        return toResponse(updated);
    }

    // Cancel order by customer with refund and stock release
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


        if ("WALLET".equals(order.getPaymentMode())) {
            try {
                walletClient.addMoney(userId, order.getTotalAmount());
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

        order.setOrderStatus("CANCELLED");
        order.setUpdatedAt(LocalDateTime.now());
        Order updated = orderRepository.save(order);
        publishEvent(userId, "CANCELLED", "Order #" + orderId + " cancelled.", orderId);
        return toResponse(updated);
    }

    // Publish order event to Kafka
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

