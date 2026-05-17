package com.booknest.review.controller;

import com.booknest.review.dto.ReviewRequest;
import com.booknest.review.dto.ReviewResponse;
import com.booknest.review.service.ReviewServiceImpl;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

// Controller for handling book reviews and ratings
@Slf4j
@RestController
@RequestMapping("/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewServiceImpl reviewService;

    // Create a new review for a specific book
    @PostMapping
    public ResponseEntity<ReviewResponse> addReview(@RequestHeader("X-User-Id") Long userId, @RequestHeader(value = "X-User-Email", required = false) String email, @Valid @RequestBody ReviewRequest request) {
        log.info("Adding review - User ID: {}, Book ID: {}", userId, request.getBookId());
        String reviewerName = email != null ? email : "Anonymous";
        return ResponseEntity.status(201).body(reviewService.addReview(userId, reviewerName, request));
    }

    // Update an existing review by ID
    @PutMapping("/{reviewId}")
    public ResponseEntity<ReviewResponse> editReview(@PathVariable Long reviewId, @RequestHeader("X-User-Id") Long userId, @Valid @RequestBody ReviewRequest request) {
        log.info("Updating review ID: {} for user: {}", reviewId, userId);
        return ResponseEntity.ok(reviewService.editReview(reviewId, userId, request));
    }

    // Remove a review by ID
    @DeleteMapping("/{reviewId}")
    public ResponseEntity<Void> deleteReview(@PathVariable Long reviewId, @RequestHeader("X-User-Id") Long userId) {
        log.info("Deleting review ID: {} for user: {}", reviewId, userId);
        reviewService.deleteReview(reviewId, userId);
        return ResponseEntity.noContent().build();
    }

    // List all reviews for a specific book ID
    @GetMapping("/book/{bookId}")
    public ResponseEntity<List<ReviewResponse>> getBookReviews(@PathVariable Long bookId) {
        log.info("Fetching reviews for book ID: {}", bookId);
        return ResponseEntity.ok(reviewService.getReviewsByBook(bookId));
    }

    // List all reviews written by the authenticated user
    @GetMapping("/my")
    public ResponseEntity<List<ReviewResponse>> getMyReviews(@RequestHeader("X-User-Id") Long userId) {
        log.info("Fetching reviews for user ID: {}", userId);
        return ResponseEntity.ok(reviewService.getMyReviews(userId));
    }
}