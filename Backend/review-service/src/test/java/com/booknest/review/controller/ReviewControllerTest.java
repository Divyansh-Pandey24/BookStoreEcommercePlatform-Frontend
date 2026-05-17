package com.booknest.review.controller;

import com.booknest.review.dto.ReviewRequest;
import com.booknest.review.dto.ReviewResponse;
import com.booknest.review.exception.GlobalExceptionHandler;
import com.booknest.review.service.ReviewServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReviewController Integration Tests (MockMvc)")
class ReviewControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper = new ObjectMapper();

    @Mock private ReviewServiceImpl reviewService;

    @InjectMocks
    private ReviewController reviewController;

    private ReviewResponse sampleReview;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(reviewController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        sampleReview = new ReviewResponse();
        sampleReview.setReviewId(1L);
        sampleReview.setBookId(100L);
        sampleReview.setUserId(10L);
        sampleReview.setRating(5);
        sampleReview.setComment("Great book!");
    }

    @Test
    @DisplayName("POST /reviews: success → 201 Created")
    void addReview_success() throws Exception {
        ReviewRequest req = new ReviewRequest();
        req.setBookId(100L);
        req.setRating(5);
        req.setComment("Great book!");

        when(reviewService.addReview(eq(10L), anyString(), any(ReviewRequest.class))).thenReturn(sampleReview);

        mockMvc.perform(post("/reviews")
                        .header("X-User-Id", 10L)
                        .header("X-User-Email", "test@booknest.com")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.reviewId").value(1L));
    }

    @Test
    @DisplayName("POST /reviews: user not purchased → 400 Bad Request")
    void addReview_notPurchased_fails() throws Exception {
        ReviewRequest req = new ReviewRequest();
        req.setBookId(100L);
        req.setRating(5);

        when(reviewService.addReview(eq(10L), anyString(), any(ReviewRequest.class)))
                .thenThrow(new RuntimeException("You can only review books you have purchased"));

        mockMvc.perform(post("/reviews")
                        .header("X-User-Id", 10L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("You can only review books you have purchased"));
    }

    @Test
    @DisplayName("GET /reviews/book/{bookId}: success → 200 OK")
    void getBookReviews_success() throws Exception {
        when(reviewService.getReviewsByBook(100L)).thenReturn(List.of(sampleReview));

        mockMvc.perform(get("/reviews/book/100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].reviewId").value(1L));
    }

    @Test
    @DisplayName("DELETE /reviews/{reviewId}: success → 204 No Content")
    void deleteReview_success() throws Exception {
        doNothing().when(reviewService).deleteReview(1L, 10L);

        mockMvc.perform(delete("/reviews/1")
                        .header("X-User-Id", 10L))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("GET /reviews/my: success → 200 OK")
    void getMyReviews_success() throws Exception {
        when(reviewService.getMyReviews(10L)).thenReturn(List.of(sampleReview));

        mockMvc.perform(get("/reviews/my")
                        .header("X-User-Id", 10L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].reviewId").value(1L));
    }

    @Test
    @DisplayName("PUT /reviews/{reviewId}: success → 200 OK")
    void editReview_success() throws Exception {
        ReviewRequest req = new ReviewRequest();
        req.setBookId(100L);
        req.setRating(4);
        req.setComment("Updated comment");

        when(reviewService.editReview(eq(1L), eq(10L), any(ReviewRequest.class))).thenReturn(sampleReview);

        mockMvc.perform(put("/reviews/1")
                        .header("X-User-Id", 10L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reviewId").value(1L));
    }
}
