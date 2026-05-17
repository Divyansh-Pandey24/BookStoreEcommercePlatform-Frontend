package com.booknest.review.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

// Entity representing a user's review and rating for a specific book
@Entity
@Table(
    name = "reviews",
    uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "book_id"})
)
@Data
@NoArgsConstructor
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long reviewId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "book_id", nullable = false)
    private Long bookId;

    // Rating value from 1 to 5
    @Column(nullable = false)
    private Integer rating;

    // Optional text comment for the review
    @Column(length = 1000)
    private String comment;

    // Display name of the reviewer (often from email)
    private String reviewerName;

    // Review lifecycle timestamps
    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime updatedAt = LocalDateTime.now();
}