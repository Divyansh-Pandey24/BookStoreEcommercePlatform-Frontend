package com.booknest.order.client;

import com.booknest.order.dto.BookDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

// ============================================================
// FALLBACK for order-service → BOOK-SERVICE calls.
//
// THREE methods — each has different fallback safety rules:
//
//   getBookById()   → NON-CRITICAL for display, but order-service
//                     uses it to check stock before placing order.
//                     Returns stub with inStock=false so the order
//                     flow naturally rejects it ("book unavailable").
//
//   reserveStock()  → CRITICAL. If BOOK-SERVICE is down, we MUST
//                     NOT pretend stock was reserved (returns false).
//                     The order placement will then fail — better
//                     than placing an order with no inventory held.
//
//   releaseStock()  → Called on order failure/cancel to undo a
//                     reservation. If BOOK-SERVICE is down we log
//                     a WARNING. This could leave stock incorrectly
//                     reserved — an ops team must reconcile later.
//                     We do NOT throw because we're already in error
//                     recovery path — throwing here makes it worse.
// ============================================================
@Component
public class BookClientFallback implements BookClient {

    private static final Logger log = LoggerFactory.getLogger(BookClientFallback.class);

    // Returns stub with inStock=false.
    // OrderService.placeOrder() will see inStock=false and abort
    // with "book not available" — prevents ghost orders.
    @Override
    public BookDto getBookById(Long bookId) {
        log.warn("[CIRCUIT BREAKER] BOOK-SERVICE unavailable. " +
                 "Returning fallback BookDto for bookId={}", bookId);
        BookDto fallback = new BookDto();
        fallback.setBookId(bookId);
        fallback.setTitle("Service Unavailable");
        fallback.setPrice(0.0);
        fallback.setStock(0);
        // CRITICAL: false means OrderService will reject the order
        fallback.setInStock(false);
        fallback.setActive(false);
        return fallback;
    }

    // Returns false — stock reservation FAILED.
    // OrderService will throw "book not available" and stop the order.
    // This is the SAFE default: better to reject the order than to
    // place it with no inventory actually held.
    @Override
    public Boolean reserveStock(Long bookId, Integer quantity) {
        log.error("[CIRCUIT BREAKER] BOOK-SERVICE unavailable. " +
                  "Cannot reserve stock for bookId={} qty={}. " +
                  "Order will be rejected to prevent overselling.",
                  bookId, quantity);
        // false triggers the "insufficient stock" branch in OrderService
        return false;
    }

    // Called during order failure/rollback to release reserved stock.
    // BOOK-SERVICE is already down — we can't release. Log it for
    // manual reconciliation by ops. Do NOT throw — we're already
    // in an error recovery path and throwing here would mask the
    // original error.
    @Override
    public void releaseStock(Long bookId, Integer quantity) {
        log.error("[CIRCUIT BREAKER] BOOK-SERVICE unavailable. " +
                  "ALERT: Could not release stock for bookId={} qty={}. " +
                  "Manual reconciliation required when service recovers.",
                  bookId, quantity);
        // Return silently — void method
    }
}
