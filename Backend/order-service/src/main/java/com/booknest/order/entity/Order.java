package com.booknest.order.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

// Entity representing a customer order and its fulfillment details
@Entity
@Table(name = "orders")
@Data
@NoArgsConstructor
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long orderId;

    // ID of the user who placed the order
    @Column(nullable = false)
    private Long userId;

    // Payment method selected (e.g., COD, WALLET)
    @Column(nullable = false)
    private String paymentMode;

    // Total monetary amount for the order
    @Column(nullable = false)
    private Double totalAmount;

    // Current state of the order in the delivery pipeline
    @Column(nullable = false)
    private String orderStatus = "PLACED";

    // Delivery recipient and contact details
    private String deliveryName;
    private String deliveryMobile;

    // Shipping destination details
    private String deliveryAddress;
    private String deliveryCity;
    private String deliveryPincode;
    private String deliveryState;

    // List of items included in this order
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    // Order lifecycle timestamps
    private LocalDateTime placedAt = LocalDateTime.now();
    private LocalDateTime updatedAt = LocalDateTime.now();
}