package com.booknest.ebook.service;

import com.booknest.ebook.client.WalletClient;
import com.booknest.ebook.entity.EBook;
import com.booknest.ebook.entity.EBookPurchase;
import com.booknest.ebook.repository.EBookPurchaseRepository;
import com.booknest.ebook.repository.EBookRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.List;

// This service implements the business logic for managing digital
@Service
public class EBookService {

    /**
     * Repository used to perform SQL CRUD operations on EBook catalog records.
     */
    private final EBookRepository ebookRepository;

    /**
     * Repository used to perform SQL operations on customer digital purchase
     * records.
     */
    private final EBookPurchaseRepository purchaseRepository;

    /**
     * Service used to manage media assets.
     */
    private final CloudinaryService cloudinaryService;

    /**
     * Feign client used to deduct customer wallet balance for digital checkouts.
     */
    private final WalletClient walletClient;

    /**
     * Secret verification key passed in header to validate service requests through
     * the API Gateway.
     */
    @Value("${gateway.secret}")
    private String gatewaySecret;

    // Constructor injection of required catalog, billing, and storage
    public EBookService(EBookRepository ebookRepository, EBookPurchaseRepository purchaseRepository,
            CloudinaryService cloudinaryService, WalletClient walletClient) {
        this.ebookRepository = ebookRepository;
        this.purchaseRepository = purchaseRepository;
        this.cloudinaryService = cloudinaryService;
        this.walletClient = walletClient;
    }

    // Retrieves all active catalog eBooks.
    public List<EBook> getActiveEBooks() {
        return ebookRepository.findByActiveTrue();
    }

    // Retrieves all catalog eBooks regardless of active status.
    public List<EBook> getAllEBooks() {
        return ebookRepository.findAll();
    }

    // Retrieves a single eBook catalog record by ID.
    public EBook getEBook(Long id) {
        return ebookRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "EBook not found"));
    }

    /**
     * Configured directory path on the local disk to store physical eBook PDF and
     * cover image files.
     */
    @Value("${app.upload-dir}")
    private String uploadDir;

    // Registers a new digital eBook and saves physical cover/PDF binaries.
    @Transactional
    public EBook createEBook(String title, String author, String description, Double price,
            MultipartFile pdfFile, MultipartFile coverImage) throws IOException {

        // Ensure that local target folder structures are allocated on disk.
        java.io.File directory = new java.io.File(uploadDir);
        if (!directory.exists())
            directory.mkdirs();

        // Save PDF locally to disk, appending timestamp to prevent duplicate name
        // collisions.
        String pdfFileName = System.currentTimeMillis() + "_" + pdfFile.getOriginalFilename();
        java.nio.file.Path pdfPath = java.nio.file.Paths.get(uploadDir, pdfFileName);
        java.nio.file.Files.copy(pdfFile.getInputStream(), pdfPath);
        String pdfUrl = "/ebook-uploads/" + pdfFileName;

        // Save Cover locally if a cover binary was provided.
        String coverUrl = null;
        if (coverImage != null && !coverImage.isEmpty()) {
            String coverFileName = System.currentTimeMillis() + "_" + coverImage.getOriginalFilename();
            java.nio.file.Path coverPath = java.nio.file.Paths.get(uploadDir, coverFileName);
            java.nio.file.Files.copy(coverImage.getInputStream(), coverPath);
            coverUrl = "/ebook-uploads/" + coverFileName;
        }

        EBook ebook = new EBook();
        ebook.setTitle(title);
        ebook.setAuthor(author);
        ebook.setDescription(description);
        ebook.setPrice(price);
        ebook.setPdfUrl(pdfUrl);
        ebook.setCoverImageUrl(coverUrl);

        return ebookRepository.save(ebook);
    }

    // Authenticates and processes customer eBook digital purchases.
    @Transactional
    public void purchaseEBook(Long userId, Long ebookId) {
        // Step 1: Prevent double purchases of identical digital catalog listings.
        if (purchaseRepository.existsByUserIdAndEbookId(userId, ebookId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "EBook already purchased");
        }

        EBook ebook = getEBook(ebookId);

        // Step 2: Invoke the Wallet microservice via Feign to deduct the eBook price
        // from the customer's account.
        try {
            walletClient.deductMoney(gatewaySecret, userId, ebook.getPrice(), null);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Insufficient wallet balance or payment failed");
        }

        // Step 3: Persist a purchase proof record mapping the customer to the eBook.
        EBookPurchase purchase = new EBookPurchase();
        purchase.setUserId(userId);
        purchase.setEbook(ebook);
        purchase.setAmountPaid(ebook.getPrice());
        purchaseRepository.save(purchase);
    }

    // Securely retrieves the digital PDF file path for an authorized
    public String getPdfUrlForUser(Long userId, String role, Long ebookId) {
        // Step 1: Restrict downloads. Access is blocked unless the user is an ADMIN or
        // has a purchase record.
        if (!"ADMIN".equals(role) && !purchaseRepository.existsByUserIdAndEbookId(userId, ebookId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You have not purchased this eBook");
        }
        EBook ebook = getEBook(ebookId);
        return ebook.getPdfUrl();
    }

    // Retrieves all digital purchases in the system.
    public List<EBookPurchase> getPurchases() {
        return purchaseRepository.findAll();
    }

    // Retrieves all digital purchases made by a specific customer.
    public List<EBookPurchase> getUserPurchases(Long userId) {
        return purchaseRepository.findByUserId(userId);
    }

    // Updates an existing digital eBook catalog record, replacing old PDF
    @Transactional
    public EBook updateEBook(Long id, String title, String author, String description, Double price,
            MultipartFile pdfFile, MultipartFile coverImage) throws IOException {
        EBook ebook = getEBook(id);

        if (title != null && !title.isBlank())
            ebook.setTitle(title);
        if (author != null && !author.isBlank())
            ebook.setAuthor(author);
        if (description != null)
            ebook.setDescription(description);
        if (price != null)
            ebook.setPrice(price);

        java.io.File directory = new java.io.File(uploadDir);
        if (!directory.exists())
            directory.mkdirs();

        // Replace PDF if a new PDF binary was provided.
        if (pdfFile != null && !pdfFile.isEmpty()) {
            // Delete old PDF from disk first.
            if (ebook.getPdfUrl() != null && ebook.getPdfUrl().startsWith("/ebook-uploads/")) {
                String oldFileName = ebook.getPdfUrl().replace("/ebook-uploads/", "");
                java.io.File oldFile = new java.io.File(uploadDir, oldFileName);
                if (oldFile.exists())
                    oldFile.delete();
            }
            String pdfFileName = System.currentTimeMillis() + "_" + pdfFile.getOriginalFilename();
            java.nio.file.Path pdfPath = java.nio.file.Paths.get(uploadDir, pdfFileName);
            java.nio.file.Files.copy(pdfFile.getInputStream(), pdfPath);
            ebook.setPdfUrl("/ebook-uploads/" + pdfFileName);
        }

        // Replace cover image if a new cover binary was provided.
        if (coverImage != null && !coverImage.isEmpty()) {
            // Delete old cover image from disk first.
            if (ebook.getCoverImageUrl() != null && ebook.getCoverImageUrl().startsWith("/ebook-uploads/")) {
                String oldFileName = ebook.getCoverImageUrl().replace("/ebook-uploads/", "");
                java.io.File oldFile = new java.io.File(uploadDir, oldFileName);
                if (oldFile.exists())
                    oldFile.delete();
            }
            String coverFileName = System.currentTimeMillis() + "_" + coverImage.getOriginalFilename();
            java.nio.file.Path coverPath = java.nio.file.Paths.get(uploadDir, coverFileName);
            java.nio.file.Files.copy(coverImage.getInputStream(), coverPath);
            ebook.setCoverImageUrl("/ebook-uploads/" + coverFileName);
        }

        return ebookRepository.save(ebook);
    }

    // Deletes a digital eBook, physical files, and associated transaction
    @Transactional
    public void deleteEBook(Long id) {
        EBook ebook = getEBook(id);

        // Delete the physical PDF file from local disk.
        if (ebook.getPdfUrl() != null && ebook.getPdfUrl().startsWith("/ebook-uploads/")) {
            String oldFileName = ebook.getPdfUrl().replace("/ebook-uploads/", "");
            new java.io.File(uploadDir, oldFileName).delete();
        }

        // Delete the physical cover image file from local disk.
        if (ebook.getCoverImageUrl() != null && ebook.getCoverImageUrl().startsWith("/ebook-uploads/")) {
            String oldFileName = ebook.getCoverImageUrl().replace("/ebook-uploads/", "");
            new java.io.File(uploadDir, oldFileName).delete();
        }

        // Delete purchase records first to satisfy Foreign Key constraints.
        purchaseRepository.deleteByEbookId(id);

        // Remove the eBook record.
        ebookRepository.deleteById(id);
    }
}
