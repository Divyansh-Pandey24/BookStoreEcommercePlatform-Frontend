package com.booknest.cart.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

// Entity representing an individual line item in a shopping cart
@Entity
@Table(name = "cart_items")
@Data
@NoArgsConstructor
public class CartItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long itemId;

    // References to the book and its snapshot details
    private Long bookId;
    private String bookTitle;
    private String coverImageUrl;
    private Double price;
    private Integer quantity;

    // Parent cart reference
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cart_id", nullable = false)
    private Cart cart;
}