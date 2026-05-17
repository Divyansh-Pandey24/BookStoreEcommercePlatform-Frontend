package com.booknest.book.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDate;
import java.time.LocalDateTime;

// DTO for book data sent back to the client
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookResponse {

    private Long bookId;
    private String title;
    private String author;
    private String isbn;
    private String genre;
    private String publisher;
    private Double price;
    private Integer stock;
    private Double rating;
    private String description;
    private String coverImageUrl;
    private LocalDate publishedDate;
    private Boolean featured;
    private Boolean active;
    private LocalDateTime createdAt;
    
    // Computed field indicating if copies are available
    private Boolean inStock;
}