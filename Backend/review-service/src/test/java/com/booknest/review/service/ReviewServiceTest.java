package com.booknest.review.service;

import com.booknest.review.client.BookClient;
import com.booknest.review.client.OrderClient;
import com.booknest.review.dto.ReviewRequest;
import com.booknest.review.dto.ReviewResponse;
import com.booknest.review.entity.Review;
import com.booknest.review.repository.ReviewRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ReviewServiceTest {

    @Mock
    private ReviewRepository reviewRepository;
    @Mock
    private BookClient bookClient;
    @Mock
    private OrderClient orderClient;

    @InjectMocks
    private ReviewServiceImpl reviewService;

    private Review sampleReview;

    @BeforeEach
    void setUp() {
        sampleReview = new Review();
        sampleReview.setReviewId(1L);
        sampleReview.setUserId(1L);
        sampleReview.setBookId(101L);
        sampleReview.setRating(5);
        sampleReview.setComment("Great book!");
        sampleReview.setReviewerName("Test User");
    }

    @Test
    void testGetReviewsByBook() {
        when(reviewRepository.findByBookIdOrderByCreatedAtDesc(101L)).thenReturn(Arrays.asList(sampleReview));
        List<ReviewResponse> reviews = reviewService.getReviewsByBook(101L);
        assertEquals(1, reviews.size());
        assertEquals("Great book!", reviews.get(0).getComment());
    }

    @Test
    void testGetMyReviews() {
        when(reviewRepository.findByUserId(1L)).thenReturn(Arrays.asList(sampleReview));
        List<ReviewResponse> reviews = reviewService.getMyReviews(1L);
        assertEquals(1, reviews.size());
    }

    @Test
    void testDeleteReview_Success() {
        when(reviewRepository.findById(1L)).thenReturn(Optional.of(sampleReview));
        
        reviewService.deleteReview(1L, 1L);
        
        verify(reviewRepository, times(1)).delete(sampleReview);
        verify(bookClient, times(1)).updateRating(anyLong(), anyDouble());
    }

    @Test
    void testDeleteReview_Denied() {
        when(reviewRepository.findById(1L)).thenReturn(Optional.of(sampleReview));
        assertThrows(RuntimeException.class, () -> reviewService.deleteReview(1L, 2L));
    }

    @Test
    void testEditReview_Success() {
        when(reviewRepository.findById(1L)).thenReturn(Optional.of(sampleReview));
        when(reviewRepository.save(any())).thenReturn(sampleReview);
        
        ReviewRequest request = new ReviewRequest();
        request.setRating(4);
        request.setComment("Updated comment");

        ReviewResponse response = reviewService.editReview(1L, 1L, request);
        
        assertEquals(4, response.getRating());
        assertEquals("Updated comment", response.getComment());
    }
}
