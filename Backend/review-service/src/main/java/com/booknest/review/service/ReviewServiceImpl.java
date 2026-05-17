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

// Service implementation for managing book reviews, ratings, and purchase verification
@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewServiceImpl {

    private final ReviewRepository reviewRepository;
    private final BookClient bookClient;
    private final OrderClient orderClient;

    // Convert Review entity to ReviewResponse DTO
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

    // Verify if a user has purchased a specific book before allowing a review
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
            return true; // Fallback to allow review if service is down
        }
    }

    // Recalculate and update the average rating for a book in the book-service
    private void updateBookRating(Long bookId) {
        try {
            Double avg = reviewRepository.findAverageRatingByBookId(bookId);
            double newRating = (avg != null) ? avg : 0.0;
            bookClient.updateRating(bookId, newRating);
            log.info("Updated book rating: bookId={}, rating={}", bookId, newRating);
        } catch (Exception e) {
            log.error("Failed to update book rating for book {}: {}", bookId, e.getMessage());
        }
    }

    // Add a new review after verifying purchase and ensuring no duplicate reviews exist
    @Transactional
    public ReviewResponse addReview(Long userId, String reviewerName, ReviewRequest request) {
        if (reviewRepository.existsByUserIdAndBookId(userId, request.getBookId())) {
            throw new RuntimeException("You have already reviewed this book.");
        }
        
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
        updateBookRating(request.getBookId());
        return toResponse(saved);
    }

    // Edit an existing review owned by the user
    @Transactional
    public ReviewResponse editReview(Long reviewId, Long userId, ReviewRequest request) {
        Review review = reviewRepository.findById(reviewId)
            .orElseThrow(() -> new ResourceNotFoundException("Review not found with id: " + reviewId));

        if (!review.getUserId().equals(userId)) {
            throw new RuntimeException("Editing denied: user does not own this review.");
        }

        review.setRating(request.getRating());
        review.setComment(request.getComment());
        review.setUpdatedAt(LocalDateTime.now());

        Review updated = reviewRepository.save(review);
        log.info("Review updated: reviewId={}", reviewId);
        updateBookRating(review.getBookId());
        return toResponse(updated);
    }

    // Delete a review owned by the user
    @Transactional
    public void deleteReview(Long reviewId, Long userId) {
        Review review = reviewRepository.findById(reviewId)
            .orElseThrow(() -> new ResourceNotFoundException("Review not found with id: " + reviewId));

        if (!review.getUserId().equals(userId)) {
            throw new RuntimeException("Deletion denied: user does not own this review.");
        }

        Long bookId = review.getBookId();
        reviewRepository.delete(review);
        log.info("Review deleted: reviewId={}", reviewId);
        updateBookRating(bookId);
    }

    // Retrieve all reviews for a specific book ordered by most recent
    public List<ReviewResponse> getReviewsByBook(Long bookId) {
        log.info("Fetching reviews for book: {}", bookId);
        return reviewRepository.findByBookIdOrderByCreatedAtDesc(bookId).stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
    }

    // Retrieve all reviews written by a specific user
    public List<ReviewResponse> getMyReviews(Long userId) {
        log.info("Fetching reviews by user: {}", userId);
        return reviewRepository.findByUserId(userId).stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
    }
}