package com.booknest.book.service;

import com.booknest.book.entity.Book;
import com.booknest.book.repository.BookRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * Migration Script: Local Images to Cloudinary.
 *
 * This runner executes automatically when the book-service starts.
 * It checks all books in the database. If a book has a local image path
 * (e.g. "uploads/books/..."), it looks for that file on the local disk,
 * uploads it to Cloudinary, and updates the database with the new URL.
 *
 * Once all old images are migrated, this script becomes a fast no-op.
 */
import org.springframework.context.annotation.Profile;

@Component
@RequiredArgsConstructor
@Slf4j
@Profile("!test")
public class CloudinaryMigrationRunner implements CommandLineRunner {

    private final BookRepository bookRepository;
    private final ImageStorageService imageStorageService;

    // The root directory of the project, where the "uploads/books" folder lives.
    // By default, if the app is run from Backend/book-service-1, the uploads might be in "uploads/books"
    // or relative to the parent. We default to "uploads/books/".
    @Value("${app.upload.dir:uploads/books/}")
    private String uploadDir;

    @Override
    public void run(String... args) {
        log.info("Starting Cloudinary Image Migration Check...");

        List<Book> allBooks = bookRepository.findAll();
        int migratedCount = 0;

        for (Book book : allBooks) {
            String currentUrl = book.getCoverImageUrl();

            // Check if the book has a cover image and if it's NOT a Cloudinary URL yet
            if (currentUrl != null && !currentUrl.isEmpty() && !currentUrl.contains("cloudinary.com")) {
                log.info("Found legacy local image path for Book ID {}: {}", book.getBookId(), currentUrl);

                // Attempt to locate the physical file on disk
                // currentUrl might be "uploads/books/some_image.jpg"
                File imageFile = new File(currentUrl);

                // If it doesn't exist, try resolving it against the user's workspace
                if (!imageFile.exists() && currentUrl.startsWith("uploads")) {
                    // Try looking one level up (if running from within book-service-1 dir)
                    imageFile = new File("../" + currentUrl);
                }

                if (imageFile.exists() && imageFile.isFile()) {
                    try {
                        // 1. Upload the physical file to Cloudinary
                        String newCloudinaryUrl = imageStorageService.saveImageFromFile(imageFile);

                        // 2. Update the Book entity with the new URL
                        book.setCoverImageUrl(newCloudinaryUrl);
                        bookRepository.save(book);

                        migratedCount++;
                        log.info("Successfully migrated Book ID {} to Cloudinary URL.", book.getBookId());

                    } catch (Exception e) {
                        log.error("Failed to migrate image for Book ID {}: {}", book.getBookId(), e.getMessage());
                    }
                } else {
                    log.warn("Could not find physical file for Book ID {}: {}. Migration skipped.", book.getBookId(), imageFile.getAbsolutePath());
                }
            }
        }

        if (migratedCount > 0) {
            log.info("Cloudinary Migration Complete! Successfully migrated {} images.", migratedCount);
        } else {
            log.info("Cloudinary Migration Check Complete. No images needed migration.");
        }
    }
}
