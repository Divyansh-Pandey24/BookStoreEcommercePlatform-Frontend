package com.booknest.cart.client;

import com.booknest.cart.dto.BookResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

// ============================================================
// WHAT THIS CLASS DOES:
//   This is the FALLBACK implementation for BookClient.
//   When BOOK-SERVICE is DOWN or takes too long to respond,
//   Resilience4j calls this class instead of hanging the caller.
//
//   HOW IT GETS ACTIVATED:
//     1. BookClient.getBookById() fails 5 times in a row
//     2. Circuit OPENS → Resilience4j stops calling BOOK-SERVICE
//     3. Every call goes straight to this fallback instantly
//     4. After 10 seconds (waitDurationInOpenState) Resilience4j
//        tries one real call (HALF-OPEN). If it succeeds → CLOSED.
//
//   WHY return a stub instead of throwing?
//     Cart browsing is NON-CRITICAL. A user can still see their
//     cart even if book metadata is temporarily unavailable.
//     Returning a "Service Unavailable" placeholder is better UX
//     than a 500 error page.
// ============================================================
@Component
public class BookClientFallback implements BookClient {

    private static final Logger log = LoggerFactory.getLogger(BookClientFallback.class);

    // Called when BOOK-SERVICE is DOWN or circuit is OPEN.
    // Returns a safe placeholder so the cart can still render.
    // The caller (CartService) must check if inStock == false
    // to handle this gracefully in business logic.
    @Override
    public BookResponse getBookById(Long bookId) {
        log.warn("[CIRCUIT BREAKER] BOOK-SERVICE is unavailable. " +
                 "Returning fallback BookResponse for bookId={}", bookId);
        BookResponse fallback = new BookResponse();
        // bookId is preserved so the caller knows which book failed
        fallback.setBookId(bookId);
        // "Service Unavailable" title signals degraded data to the frontend
        fallback.setTitle("Service Unavailable");
        fallback.setPrice(0.0);
        fallback.setStock(0);
        // inStock=false ensures the UI blocks add-to-cart actions
        fallback.setInStock(false);
        // active=false signals this is a degraded fallback, not a real book
        fallback.setActive(false);
        return fallback;
    }
}
