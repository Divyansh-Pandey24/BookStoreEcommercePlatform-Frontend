package com.booknest.review.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

// ============================================================
// FALLBACK for review-service → BOOK-SERVICE calls.
//
// updateRating() is NON-CRITICAL: if BOOK-SERVICE is down,
// the rating update is lost for this cycle but the review
// itself is saved. A batch job or retry could re-sync later.
// We log a warning and return silently — no exception thrown.
// ============================================================
@Component
public class BookClientFallback implements BookClient {

    private static final Logger log = LoggerFactory.getLogger(BookClientFallback.class);

    // Called when BOOK-SERVICE is DOWN or circuit is OPEN.
    // We silently log the failure.
    // BOOK-SERVICE can recalculate ratings from the review table later.
    // Throwing an exception here would prevent the review from being saved —
    // which is worse than having a temporarily stale rating.
    @Override
    public void updateRating(Long bookId, Double averageRating) {
        log.warn("[CIRCUIT BREAKER] BOOK-SERVICE is unavailable. " +
                 "Rating update skipped for bookId={}. averageRating={}. " +
                 "Rating will be stale until BOOK-SERVICE recovers.",
                 bookId, averageRating);
        // Return silently — void method, no value to return
    }
}
