package com.booknest.order.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

/*
 * Matches BookResponse from book-service.
 * Used to check stock before placing order.
 */
@Data
@NoArgsConstructor
public class BookDto {
    private Long    bookId;
    private String  title;
    private Double  price;
    private Integer stock;
    private Boolean inStock;
    private Boolean active;
}