package com.booknest.order.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

/*
 * Matches CartResponse from cart-service.
 * This is what we get when we call CartClient.getCart().
 */
@Data
@NoArgsConstructor
public class CartDto {
    private Long          cartId;
    private Long          userId;
    private Double        totalPrice;
    private Integer       totalItems;
    private List<CartItemDto> items;
}