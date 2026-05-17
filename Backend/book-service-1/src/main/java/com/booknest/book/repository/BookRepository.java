package com.booknest.book.repository;

import com.booknest.book.entity.Book;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

// Repository for database operations on Book entities
@Repository
public interface BookRepository extends JpaRepository<Book, Long> {

    // Retrieve active books
    List<Book> findByActiveTrue();

    // Retrieve a single active book by ID
    Optional<Book> findByBookIdAndActiveTrue(Long bookId);

    // Filter active books by genre (case-insensitive)
    List<Book> findByGenreIgnoreCaseAndActiveTrue(String genre);

    // Retrieve active featured books
    List<Book> findByFeaturedTrueAndActiveTrue();

    // Filter active books by price range
    List<Book> findByPriceBetweenAndActiveTrue(Double minPrice, Double maxPrice);

    // Check if a book exists with the given ISBN
    boolean existsByIsbn(String isbn);

    // Search active books across multiple fields (MySQL fallback)
    @Query("SELECT b FROM Book b WHERE b.active = true AND (" +
           "LOWER(b.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(b.author) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(b.genre) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(b.description) LIKE LOWER(CONCAT('%', :keyword, '%'))" +
           ")")
    List<Book> searchBooks(@Param("keyword") String keyword);
}