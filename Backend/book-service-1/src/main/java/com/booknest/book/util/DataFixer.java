package com.booknest.book.util;

import com.booknest.book.entity.Book;
import com.booknest.book.repository.BookRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.springframework.context.annotation.Profile;

@Component
@RequiredArgsConstructor
@Slf4j
@Profile("!test")
// Trigger final deployment with optimized ES and Razorpay keys
public class DataFixer implements CommandLineRunner {

    private final BookRepository bookRepository;
    private final com.booknest.book.service.BookService bookService;

    @Value("${app.upload.dir}")
    private String uploadDir;

    @Override
    public void run(String... args) throws Exception {
        log.info("Starting DataFixer to repair book records and sync search index...");
        
        fixFileNameTypos();
        repairNullUrls();
        
        // Ensure all books are active and sync them to Elasticsearch
        try {
            log.info("Ensuring all books are active...");
            List<com.booknest.book.entity.Book> all = bookRepository.findAll();
            for (com.booknest.book.entity.Book b : all) {
                b.setActive(true);
                // If none are featured, mark first few as featured
                if (b.getBookId() <= 8) b.setFeatured(true);
            }
            bookRepository.saveAll(all);

            log.info("Triggering automatic Elasticsearch synchronization...");
            bookService.syncAllBooksToElasticsearch("ADMIN");
            log.info("Sync complete.");
        } catch (Exception e) {
            log.error("Failed to sync search index on startup: {}", e.getMessage());
        }
        
        log.info("DataFixer completion.");
    }

    private void fixFileNameTypos() {
        log.info("Checking for filename typos (e.g., ..jpg) in {}", uploadDir);
        try {
            File dir = new File(uploadDir);
            File[] files = dir.listFiles();
            if (files == null) return;

            for (File file : files) {
                String name = file.getName();
                if (name.endsWith("..jpg")) {
                    String newName = name.replace("..jpg", ".jpg");
                    Path source = file.toPath();
                    Path target = Paths.get(uploadDir, newName);
                    
                    if (Files.exists(target)) {
                        log.warn("Target file {} already exists. Deleting typo file {}.", newName, name);
                        Files.delete(source);
                    } else {
                        Files.move(source, target);
                        log.info("Renamed {} to {}", name, newName);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error fixing typos: {}", e.getMessage());
        }
    }

    private void repairNullUrls() {
        List<Book> allBooks = bookRepository.findAll();
        log.info("Auditing {} books for missing covers", allBooks.size());

        for (Book book : allBooks) {
            String currentUrl = book.getCoverImageUrl();
            
            // 1. Fix references to ..jpg in DB if any
            if (currentUrl != null && currentUrl.contains("..jpg")) {
                book.setCoverImageUrl(currentUrl.replace("..jpg", ".jpg"));
                bookRepository.save(book);
                log.info("Updated DB record for Book {}: corrected ..jpg to .jpg", book.getBookId());
            }

            // 2. Try to find a match for null URLs
            if (currentUrl == null || currentUrl.isEmpty()) {
                String targetName = "book_" + book.getBookId() + "_";
                File dir = new File(uploadDir);
                File[] matches = dir.listFiles((d, name) -> name.startsWith(targetName) && name.endsWith(".jpg"));
                
                if (matches != null && matches.length > 0) {
                    String matchedFile = "uploads/books/" + matches[0].getName();
                    book.setCoverImageUrl(matchedFile);
                    bookRepository.save(book);
                    log.info("Matched Book {} ({}) to image {}", book.getBookId(), book.getTitle(), matchedFile);
                } else {
                    // Try to match by ISBN if available
                    String isbn = book.getIsbn();
                    if (isbn != null && !isbn.isEmpty()) {
                        File[] isbnMatches = dir.listFiles((d, name) -> name.contains(isbn) && name.endsWith(".jpg"));
                        if (isbnMatches != null && isbnMatches.length > 0) {
                            String matchedFile = "uploads/books/" + isbnMatches[0].getName();
                            book.setCoverImageUrl(matchedFile);
                            bookRepository.save(book);
                            log.info("Matched Book {} via ISBN to image {}", book.getBookId(), matchedFile);
                        }
                    }
                }
            }
        }
    }
}
