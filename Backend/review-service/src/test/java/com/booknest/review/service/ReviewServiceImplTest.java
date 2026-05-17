package com.booknest.review.service;

import com.booknest.review.client.BookClient;
import com.booknest.review.client.OrderClient;
import com.booknest.review.dto.OrderItemDto;
import com.booknest.review.dto.OrderResponse;
import com.booknest.review.dto.ReviewRequest;
import com.booknest.review.dto.ReviewResponse;
import com.booknest.review.entity.Review;
import com.booknest.review.repository.ReviewRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReviewServiceImpl Unit Tests")
class ReviewServiceImplTest {

    @Mock private ReviewRepository reviewRepository;
    @Mock private BookClient bookClient;
    @Mock private OrderClient orderClient;

    @InjectMocks
    private ReviewServiceImpl reviewService;

    private Review sampleReview;
    private ReviewRequest reviewRequest;
    private OrderResponse orderWithBook;

    @BeforeEach
    void setUp() {
        sampleReview = new Review();
        sampleReview.setReviewId(1L);
        sampleReview.setUserId(10L);
        sampleReview.setBookId(100L);
        sampleReview.setRating(4);
        sampleReview.setComment("Great book!");
        sampleReview.setReviewerName("test@booknest.com");

        reviewRequest = new ReviewRequest();
        reviewRequest.setBookId(100L);
        reviewRequest.setRating(4);
        reviewRequest.setComment("Great book!");

        OrderItemDto orderItem = new OrderItemDto();
        orderItem.setBookId(100L);

        orderWithBook = new OrderResponse();
        orderWithBook.setOrderStatus("DELIVERED");
        orderWithBook.setItems(List.of(orderItem));
    }

    // ─────────────────────────── ADD REVIEW ───────────────────────────

    @Test
    @DisplayName("addReview: not reviewed yet, has purchased → saved and book rating updated")
    void addReview_success() {
        when(reviewRepository.existsByUserIdAndBookId(10L, 100L)).thenReturn(false);
        when(orderClient.getMyOrders(10L)).thenReturn(List.of(orderWithBook));
        when(reviewRepository.save(any(Review.class))).thenReturn(sampleReview);
        when(reviewRepository.findAverageRatingByBookId(100L)).thenReturn(4.0);
        doNothing().when(bookClient).updateRating(100L, 4.0);

        ReviewResponse response = reviewService.addReview(10L, "test@booknest.com", reviewRequest);

        assertThat(response.getReviewId()).isEqualTo(1L);
        assertThat(response.getRating()).isEqualTo(4);
        verify(reviewRepository).save(any(Review.class));
        verify(bookClient).updateRating(100L, 4.0);
    }

    @Test
    @DisplayName("addReview: already reviewed → throws RuntimeException")
    void addReview_alreadyReviewed_throws() {
        when(reviewRepository.existsByUserIdAndBookId(10L, 100L)).thenReturn(true);

        assertThatThrownBy(() -> reviewService.addReview(10L, "test@booknest.com", reviewRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("already reviewed");
    }

    @Test
    @DisplayName("addReview: has not purchased → throws RuntimeException")
    void addReview_notPurchased_throws() {
        when(reviewRepository.existsByUserIdAndBookId(10L, 100L)).thenReturn(false);
        // Order that doesn't contain bookId=100
        OrderResponse orderWithoutBook = new OrderResponse();
        orderWithoutBook.setOrderStatus("DELIVERED");
        orderWithoutBook.setItems(List.of()); // empty items
        when(orderClient.getMyOrders(10L)).thenReturn(List.of(orderWithoutBook));

        assertThatThrownBy(() -> reviewService.addReview(10L, "test@booknest.com", reviewRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Purchase required");
    }

    @Test
    @DisplayName("addReview: order service down → allows review gracefully (failsafe)")
    void addReview_orderServiceDown_allowsReview() {
        when(reviewRepository.existsByUserIdAndBookId(10L, 100L)).thenReturn(false);
        when(orderClient.getMyOrders(10L)).thenThrow(new RuntimeException("Order service down"));
        when(reviewRepository.save(any(Review.class))).thenReturn(sampleReview);
        when(reviewRepository.findAverageRatingByBookId(100L)).thenReturn(4.0);
        doNothing().when(bookClient).updateRating(100L, 4.0);

        // hasPurchasedBook catches exception → returns true → review proceeds
        ReviewResponse response = reviewService.addReview(10L, "test@booknest.com", reviewRequest);

        assertThat(response).isNotNull();
        verify(reviewRepository).save(any(Review.class));
    }

    // ─────────────────────────── EDIT REVIEW ───────────────────────────

    @Test
    @DisplayName("editReview: own review → updated and book rating recalculated")
    void editReview_success() {
        when(reviewRepository.findById(1L)).thenReturn(Optional.of(sampleReview));
        when(reviewRepository.save(any(Review.class))).thenReturn(sampleReview);
        when(reviewRepository.findAverageRatingByBookId(100L)).thenReturn(4.5);
        doNothing().when(bookClient).updateRating(100L, 4.5);

        ReviewResponse response = reviewService.editReview(1L, 10L, reviewRequest);

        assertThat(response).isNotNull();
        verify(reviewRepository).save(any(Review.class));
        verify(bookClient).updateRating(100L, 4.5);
    }

    @Test
    @DisplayName("editReview: review not found → throws RuntimeException")
    void editReview_notFound_throws() {
        when(reviewRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reviewService.editReview(99L, 10L, reviewRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Review not found");
    }

    @Test
    @DisplayName("editReview: editing someone else's review → throws RuntimeException")
    void editReview_notOwner_throws() {
        when(reviewRepository.findById(1L)).thenReturn(Optional.of(sampleReview));

        assertThatThrownBy(() -> reviewService.editReview(1L, 99L, reviewRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Editing denied");
    }

    // ─────────────────────────── DELETE REVIEW ───────────────────────────

    @Test
    @DisplayName("deleteReview: own review → deleted and book rating updated")
    void deleteReview_success() {
        when(reviewRepository.findById(1L)).thenReturn(Optional.of(sampleReview));
        doNothing().when(reviewRepository).delete(sampleReview);
        when(reviewRepository.findAverageRatingByBookId(100L)).thenReturn(3.5);
        doNothing().when(bookClient).updateRating(100L, 3.5);

        reviewService.deleteReview(1L, 10L);

        verify(reviewRepository).delete(sampleReview);
        verify(bookClient).updateRating(100L, 3.5);
    }

    @Test
    @DisplayName("deleteReview: not owner → throws RuntimeException")
    void deleteReview_notOwner_throws() {
        when(reviewRepository.findById(1L)).thenReturn(Optional.of(sampleReview));

        assertThatThrownBy(() -> reviewService.deleteReview(1L, 99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Deletion denied");
    }

    // ─────────────────────────── GET REVIEWS ───────────────────────────

    @Test
    @DisplayName("getReviewsByBook: returns list of reviews for given book")
    void getReviewsByBook_returnsList() {
        when(reviewRepository.findByBookIdOrderByCreatedAtDesc(100L))
                .thenReturn(List.of(sampleReview));

        List<ReviewResponse> result = reviewService.getReviewsByBook(100L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getBookId()).isEqualTo(100L);
    }

    @Test
    @DisplayName("getMyReviews: returns list of reviews written by user")
    void getMyReviews_returnsList() {
        when(reviewRepository.findByUserId(10L)).thenReturn(List.of(sampleReview));

        List<ReviewResponse> result = reviewService.getMyReviews(10L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getUserId()).isEqualTo(10L);
    }
}
