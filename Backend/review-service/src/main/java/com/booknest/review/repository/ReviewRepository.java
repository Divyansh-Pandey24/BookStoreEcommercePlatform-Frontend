package com.booknest.review.repository;

import com.booknest.review.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

// Repository for database operations on Review entities
@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    // Retrieve all reviews for a book, most recent first
    List<Review> findByBookIdOrderByCreatedAtDesc(Long bookId);

    // Retrieve all reviews written by a specific user
    List<Review> findByUserId(Long userId);

    // Retrieve a specific user's review for a book
    Optional<Review> findByUserIdAndBookId(Long userId, Long bookId);

    // Check if a user has already reviewed a specific book
    boolean existsByUserIdAndBookId(Long userId, Long bookId);

    // Calculate the average rating for a specific book
    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.bookId = :bookId")
    Double findAverageRatingByBookId(@Param("bookId") Long bookId);

    // Count the total number of reviews for a book
    long countByBookId(Long bookId);
}