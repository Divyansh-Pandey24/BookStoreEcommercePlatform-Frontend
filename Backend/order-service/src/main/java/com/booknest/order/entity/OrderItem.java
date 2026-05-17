package com.booknest.order.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

// Entity representing an individual line item within an order
@Entity
@Table(name = "order_items")
@Data
@NoArgsConstructor
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long orderItemId;

    // References to the book being purchased
    private Long bookId;
    private String bookTitle;
    private String coverImageUrl;

    // Price snapshot at the time of purchase
    private Double price;
    private Integer quantity;

    // Calculated subtotal (price * quantity)
    private Double subtotal;

    // Parent order reference
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;
}