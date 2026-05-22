package com.booknest.book.controller;

import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.booknest.book.dto.BookRequest;
import com.booknest.book.dto.BookResponse;
import com.booknest.book.service.BookService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.security.access.prepost.PreAuthorize;

// Controller handling book-related operations
@RestController
@RequestMapping("/books")
@RequiredArgsConstructor
@Slf4j
public class BookController {

    private final BookService bookService;

    // Fetches the list of all books in the database catalog.
    @GetMapping
    public ResponseEntity<List<BookResponse>> getAllBooks() {
        log.info("Fetching all books");
        return ResponseEntity.ok(bookService.getAllBooks());
    }

    // Fetches detailed metadata of a specific catalog book.
    @GetMapping("/{bookId}")
    public ResponseEntity<BookResponse> getBookById(@PathVariable Long bookId) {
        log.info("Fetching book with ID: {}", bookId);
        return ResponseEntity.ok(bookService.getBookById(bookId));
    }

    // Searches the book catalog using fuzzy logic on titles, authors, and genres.
    @GetMapping("/search")
    public ResponseEntity<List<BookResponse>> searchBooks(@RequestParam(required = false) String keyword) {
        log.info("Searching books with keyword: {}", keyword);
        return ResponseEntity.ok(bookService.searchBooks(keyword));
    }

    // Filters the book catalog by category/genre.
    @GetMapping("/genre/{genre}")
    public ResponseEntity<List<BookResponse>> getByGenre(@PathVariable String genre) {
        log.info("Filtering books by genre: {}", genre);
        return ResponseEntity.ok(bookService.getBooksByGenre(genre));
    }

    // Retrieves featured spotlight books marked for homepage promotion.
    @GetMapping("/featured")
    public ResponseEntity<List<BookResponse>> getFeatured() {
        log.info("Fetching featured books");
        return ResponseEntity.ok(bookService.getFeaturedBooks());
    }

    // Filters catalog items within minimum and maximum pricing boundaries.
    @GetMapping("/price-range")
    public ResponseEntity<List<BookResponse>> getByPriceRange(@RequestParam Double min, @RequestParam Double max) {
        log.info("Filtering books by price: {}-{}", min, max);
        return ResponseEntity.ok(bookService.getBooksByPriceRange(min, max));
    }

    // Adds a new book to the database catalog.
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<BookResponse> addBook(
            @Valid @RequestBody BookRequest request,
            @RequestHeader(value = "X-User-Role", required = false) String role) {
        log.info("Adding new book: {}", request.getTitle());
        return ResponseEntity.status(201).body(bookService.addBook(request, role));
    }

    // Updates metadata properties of an existing catalog book.
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{bookId}")
    public ResponseEntity<BookResponse> updateBook(
            @PathVariable Long bookId,
            @Valid @RequestBody BookRequest request,
            @RequestHeader(value = "X-User-Role", required = false) String role) {
        log.info("Updating book with ID: {}", bookId);
        return ResponseEntity.ok(bookService.updateBook(bookId, request, role));
    }

    // Deletes a book, executing a soft deletion so historical orders are not broken.
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{bookId}")
    public ResponseEntity<Void> deleteBook(
            @PathVariable Long bookId,
            @RequestHeader(value = "X-User-Role", required = false) String role) {
        log.info("Deleting book with ID: {}", bookId);
        bookService.deleteBook(bookId, role);
        return ResponseEntity.noContent().build();
    }

    // Updates the stock levels of a specific catalog book.
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{bookId}/stock")
    public ResponseEntity<BookResponse> updateStock(
            @PathVariable Long bookId,
            @RequestParam Integer quantity,
            @RequestHeader(value = "X-User-Role", required = false) String role) {
        log.info("Updating stock for book: {} by {}", bookId, quantity);
        return ResponseEntity.ok(bookService.updateStock(bookId, quantity, role));
    }

    // Toggles whether a book is highlighted as featured.
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{bookId}/featured")
    public ResponseEntity<BookResponse> toggleFeatured(
            @PathVariable Long bookId,
            @RequestHeader(value = "X-User-Role", required = false) String role) {
        log.info("Toggling featured status for book: {}", bookId);
        return ResponseEntity.ok(bookService.toggleFeatured(bookId, role));
    }

    // Uploads a book cover image, integrating with cloud file APIs.
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/upload-image/{bookId}")
    public ResponseEntity<BookResponse> uploadCover(
            @PathVariable Long bookId,
            @RequestParam("file") MultipartFile file,
            @RequestHeader(value = "X-User-Role", required = false) String role) {
        log.info("Dedicated image upload for book: {}", bookId);
        return ResponseEntity.ok(bookService.uploadCoverImage(bookId, file, role));
    }

    // Checks if the book has enough stock for purchase.
    @GetMapping("/{bookId}/check-stock")
    public ResponseEntity<Boolean> checkStock(@PathVariable Long bookId, @RequestParam Integer quantity) {
        log.info("Checking stock for ID {}: requested {}", bookId, quantity);
        return ResponseEntity.ok(bookService.checkStock(bookId, quantity));
    }

    // Reserves stock inventory during order orchestrations.
    @PostMapping("/{bookId}/reserve")
    public ResponseEntity<Boolean> reserveStock(@PathVariable Long bookId, @RequestParam Integer quantity) {
        log.info("Reserving stock for ID {}: quantity {}", bookId, quantity);
        return ResponseEntity.ok(bookService.reserveStock(bookId, quantity));
    }

    // Releases reserved stock back to active inventory if order checkout fails.
    @PostMapping("/{bookId}/release")
    public ResponseEntity<Void> releaseStock(@PathVariable Long bookId, @RequestParam Integer quantity) {
        log.info("Releasing stock for ID {}: quantity {}", bookId, quantity);
        bookService.releaseStock(bookId, quantity);
        return ResponseEntity.noContent().build();
    }

    // Updates the average star rating of a book.
    @PatchMapping("/{bookId}/rating")
    public ResponseEntity<Void> updateRating(@PathVariable Long bookId, @RequestParam Double averageRating) {
        log.info("Updating rating for book {}: {}", bookId, averageRating);
        bookService.updateRating(bookId, averageRating);
        return ResponseEntity.ok().build();
    }

    // Re-synchronizes the local database catalog items with Elasticsearch search indexes.
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/sync-elasticsearch")
    public ResponseEntity<String> syncToElasticsearch(@RequestHeader(value = "X-User-Role", required = false) String role) {
        log.info("Triggering search index sync");
        return ResponseEntity.ok(bookService.syncAllBooksToElasticsearch(role));
    }
}
