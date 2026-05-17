package com.booknest.review.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

// Feign client for updating book ratings in the book-service.
// fallback = BookClientFallback.class:
//   updateRating() silently skips — review is saved, rating update is deferred.
@FeignClient(name = "BOOK-SERVICE", fallback = BookClientFallback.class)
public interface BookClient {

    // Update the average rating for a specific book
    @PatchMapping("/books/{bookId}/rating")
    void updateRating(@PathVariable Long bookId, @RequestParam Double averageRating);
}