package com.booknest.cart.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/*
 * What customer sends when adding a book to cart.
 * They only need to tell us: which book and how many.
 * We fetch book details (title, price) from Book Service.
 */
@Data
public class AddToCartRequest {

    @NotNull(message = "Book ID is required")
    private Long bookId;

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity;
}