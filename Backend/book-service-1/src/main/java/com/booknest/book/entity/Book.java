package com.booknest.book.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.time.LocalDateTime;

// Entity representing a book in the database
@Entity
@Table(name = "books")
@Data
@NoArgsConstructor
public class Book {

    // Primary key with auto-increment
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long bookId;

    // Title of the book
    @Column(nullable = false)
    private String title;

    // Author of the book
    @Column(nullable = false)
    private String author;

    // Globally unique ISBN code
    @Column(unique = true)
    private String isbn;

    // Genre of the book
    @Column(nullable = false)
    private String genre;

    // Publisher of the book
    private String publisher;

    // Selling price of the book
    @Column(nullable = false)
    private Double price;

    // Number of copies available in stock
    @Column(nullable = false)
    private Integer stock;

    // Average user rating
    private Double rating = 0.0;

    // Detailed description of the book
    @Column(length = 2000)
    private String description;

    // Path to the book cover image file
    private String coverImageUrl;

    // Date when the book was published
    private LocalDate publishedDate;

    // Whether the book is featured on the home page
    private Boolean featured = false;

    // Soft delete status (true = active, false = deleted)
    private Boolean active = true;

    // Timestamp when the record was created
    private LocalDateTime createdAt = LocalDateTime.now();

    // Timestamp when the record was last updated
    private LocalDateTime updatedAt = LocalDateTime.now();
}