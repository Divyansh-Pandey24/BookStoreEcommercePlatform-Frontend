package com.booknest.review.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class ReviewResponse {
    private Long          reviewId;
    private Long          userId;
    private Long          bookId;
    private Integer       rating;
    private String        comment;
    private String        reviewerName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}