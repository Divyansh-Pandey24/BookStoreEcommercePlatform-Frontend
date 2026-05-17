package com.booknest.order.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

/*
 * Matches CartResponse.CartItemResponse from cart-service.
 * Feign deserializes the JSON response into this object.
 * We only include fields we actually use in order-service.
 */
@Data
@NoArgsConstructor
public class CartItemDto {
    private Long    itemId;
    private Long    bookId;
    private String  bookTitle;
    private String  coverImageUrl;
    private Double  price;
    private Integer quantity;
    private Double  subtotal;
}