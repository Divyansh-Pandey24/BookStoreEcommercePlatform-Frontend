package com.booknest.review.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import com.booknest.review.exception.ResourceNotFoundException;
import org.springframework.transaction.annotation.Transactional;
import com.booknest.review.client.BookClient;
import com.booknest.review.client.OrderClient;
import com.booknest.review.dto.*;
import com.booknest.review.entity.Review;
import com.booknest.review.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// This service implements the business logic for managing book
@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewServiceImpl {

    /**
     * Repository used to perform SQL CRUD operations on Review records.
     */
    private final ReviewRepository reviewRepository;

    /**
     * Feign client to propagate average rating updates to the Book catalog
     * microservice.
     */
    private final BookClient bookClient;

    /**
     * Feign client to fetch customer order history to verify purchase status.
     */
    private final OrderClient orderClient;

    // Helper method to map a Review database entity to a formatted
    private ReviewResponse toResponse(Review review) {
        ReviewResponse r = new ReviewResponse();
        r.setReviewId(review.getReviewId());
        r.setUserId(review.getUserId());
        r.setBookId(review.getBookId());
        r.setRating(review.getRating());
        r.setComment(review.getComment());
        r.setReviewerName(review.getReviewerName());
        r.setCreatedAt(review.getCreatedAt());
        r.setUpdatedAt(review.getUpdatedAt());
        return r;
    }

    // Checks if a customer has purchased a book before allowing a review.
    private boolean hasPurchasedBook(Long userId, Long bookId) {
        try {
            List<OrderResponse> orders = orderClient.getMyOrders(userId);
            return orders.stream()
                    .filter(order -> !"CANCELLED".equals(order.getOrderStatus()))
                    .filter(order -> order.getItems() != null)
                    .flatMap(order -> order.getItems().stream())
                    .anyMatch(item -> bookId.equals(item.getBookId()));
        } catch (Exception e) {
            log.error("Purchase verification failed for user {}: {}", userId, e.getMessage());
            // Fallback: Default to true if the order microservice is down to ensure review
            // service resilience.
            return true;
        }
    }

    // Recalculates and updates the average rating of a book.
    private void updateBookRating(Long bookId) {
        try {
            Double avg = reviewRepository.findAverageRatingByBookId(bookId);
            double newRating = (avg != null) ? avg : 0.0;

            // Push updated average rating to Book catalog microservice.
            bookClient.updateRating(bookId, newRating);
            log.info("Updated book rating: bookId={}, rating={}", bookId, newRating);
        } catch (Exception e) {
            log.error("Failed to update book rating for book {}: {}", bookId, e.getMessage());
        }
    }

    // Submits a new review for a book.
    @Transactional
    public ReviewResponse addReview(Long userId, String reviewerName, ReviewRequest request) {
        // Step 1: Prevent duplicate reviews for the same book by the same user.
        if (reviewRepository.existsByUserIdAndBookId(userId, request.getBookId())) {
            throw new RuntimeException("You have already reviewed this book.");
        }

        // Step 2: Enforce verified purchase constraints.
        if (!hasPurchasedBook(userId, request.getBookId())) {
            throw new RuntimeException("Purchase required to review this book.");
        }

        Review review = new Review();
        review.setUserId(userId);
        review.setBookId(request.getBookId());
        review.setRating(request.getRating());
        review.setComment(request.getComment());
        review.setReviewerName(reviewerName);

        Review saved = reviewRepository.save(review);
        log.info("New review saved for book: {} by user: {}", request.getBookId(), userId);

        // Step 3: Recalculate average book rating.
        updateBookRating(request.getBookId());
        return toResponse(saved);
    }

    // Edits an existing review.
    @Transactional
    public ReviewResponse editReview(Long reviewId, Long userId, ReviewRequest request) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found with id: " + reviewId));

        // Enforce ownership validation.
        if (!review.getUserId().equals(userId)) {
            throw new RuntimeException("Editing denied: user does not own this review.");
        }

        review.setRating(request.getRating());
        review.setComment(request.getComment());
        review.setUpdatedAt(LocalDateTime.now());

        Review updated = reviewRepository.save(review);
        log.info("Review updated: reviewId={}", reviewId);

        // Propagate updated book rating average.
        updateBookRating(review.getBookId());
        return toResponse(updated);
    }

    // Deletes a review.
    @Transactional
    public void deleteReview(Long reviewId, Long userId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found with id: " + reviewId));

        // Enforce ownership validation.
        if (!review.getUserId().equals(userId)) {
            throw new RuntimeException("Deletion denied: user does not own this review.");
        }

        Long bookId = review.getBookId();
        reviewRepository.delete(review);
        log.info("Review deleted: reviewId={}", reviewId);

        // Propagate rating average updates.
        updateBookRating(bookId);
    }

    // Retrieves all active reviews for a book, sorted by most recent.
    public List<ReviewResponse> getReviewsByBook(Long bookId) {
        log.info("Fetching reviews for book: {}", bookId);
        return reviewRepository.findByBookIdOrderByCreatedAtDesc(bookId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // Retrieves all reviews submitted by a specific user.
    public List<ReviewResponse> getMyReviews(Long userId) {
        log.info("Fetching reviews by user: {}", userId);
        return reviewRepository.findByUserId(userId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }
}
