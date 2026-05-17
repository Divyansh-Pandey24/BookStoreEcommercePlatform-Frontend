package com.booknest.order.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class OrderItemResponse {
    private Long    orderItemId;
    private Long    bookId;
    private String  bookTitle;
    private String  coverImageUrl;
    private Double  price;
    private Integer quantity;
    private Double  subtotal;
}