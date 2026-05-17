package com.booknest.review.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class ReviewRequest {

    @NotNull(message = "Book ID is required")
    private Long bookId;

    /*
     * @Min(1) @Max(5): rating must be between 1 and 5.
     * Spring rejects anything outside this range.
     */
    @NotNull(message = "Rating is required")
    @Min(value = 1, message = "Rating must be at least 1")
    @Max(value = 5, message = "Rating cannot exceed 5")
    private Integer rating;

    @Size(max = 1000, message = "Comment too long")
    private String comment;
}