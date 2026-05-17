package com.booknest.book.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

// DTO for incoming book data from the client
@Data
public class BookRequest {

    // Title of the book (mandatory)
    @NotBlank(message = "Title is required")
    private String title;

    // Author of the book (mandatory)
    @NotBlank(message = "Author is required")
    private String author;

    // Optional ISBN code
    private String isbn;

    // Genre of the book (mandatory)
    @NotBlank(message = "Genre is required")
    private String genre;

    // Optional publisher name
    private String publisher;

    // Selling price (mandatory, must be positive)
    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.01", message = "Price must be greater than 0")
    private Double price;

    // Inventory count (mandatory, non-negative)
    @NotNull(message = "Stock is required")
    @Min(value = 0, message = "Stock cannot be negative")
    private Integer stock;

    // Long description text
    @Size(max = 2000, message = "Description cannot exceed 2000 characters")
    private String description;

    // Publication date in YYYY-MM-DD format
    private String publishedDate;

    // Flag to mark book as featured on home page
    private Boolean featured = false;
}