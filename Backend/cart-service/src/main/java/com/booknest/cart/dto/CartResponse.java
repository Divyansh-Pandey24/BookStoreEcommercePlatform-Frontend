package com.booknest.cart.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;

/*
 * What we send back to React when they ask to see the cart.
 * Contains the cart info AND list of all items inside.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CartResponse {

    private Long   cartId;
    private Long   userId;
    private Double totalPrice;
    private Integer totalItems;    // total number of items
    private LocalDateTime updatedAt;

    // All items inside this cart
    private List<CartItemResponse> items;

    /*
     * Inner class — defined inside CartResponse
     * because it is only used here.
     * Represents one item row in the cart.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CartItemResponse {
        private Long    itemId;
        private Long    bookId;
        private String  bookTitle;
        private String  coverImageUrl;
        private Double  price;
        private Integer quantity;
        private Double  subtotal;  // price × quantity
    }
}