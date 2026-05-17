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

@Service
public class EBookService {

    private final EBookRepository ebookRepository;
    private final EBookPurchaseRepository purchaseRepository;
    private final CloudinaryService cloudinaryService;
    private final WalletClient walletClient;

    @Value("${gateway.secret}")
    private String gatewaySecret;

    public EBookService(EBookRepository ebookRepository, EBookPurchaseRepository purchaseRepository,
                        CloudinaryService cloudinaryService, WalletClient walletClient) {
        this.ebookRepository = ebookRepository;
        this.purchaseRepository = purchaseRepository;
        this.cloudinaryService = cloudinaryService;
        this.walletClient = walletClient;
    }

    public List<EBook> getActiveEBooks() {
        return ebookRepository.findByActiveTrue();
    }

    public List<EBook> getAllEBooks() {
        return ebookRepository.findAll();
    }

    public EBook getEBook(Long id) {
        return ebookRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "EBook not found"));
    }

    @Value("${app.upload-dir}")
    private String uploadDir;

    @Transactional
    public EBook createEBook(String title, String author, String description, Double price,
                             MultipartFile pdfFile, MultipartFile coverImage) throws IOException {
        
        java.io.File directory = new java.io.File(uploadDir);
        if (!directory.exists()) directory.mkdirs();

        // Save PDF locally
        String pdfFileName = System.currentTimeMillis() + "_" + pdfFile.getOriginalFilename();
        java.nio.file.Path pdfPath = java.nio.file.Paths.get(uploadDir, pdfFileName);
        java.nio.file.Files.copy(pdfFile.getInputStream(), pdfPath);
        String pdfUrl = "/ebook-uploads/" + pdfFileName;

        // Save Cover locally if exists
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

    @Transactional
    public void purchaseEBook(Long userId, Long ebookId) {
        if (purchaseRepository.existsByUserIdAndEbookId(userId, ebookId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "EBook already purchased");
        }

        EBook ebook = getEBook(ebookId);
        
        try {
            walletClient.deductMoney(gatewaySecret, userId, ebook.getPrice(), null);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Insufficient wallet balance or payment failed");
        }

        EBookPurchase purchase = new EBookPurchase();
        purchase.setUserId(userId);
        purchase.setEbook(ebook);
        purchase.setAmountPaid(ebook.getPrice());
        purchaseRepository.save(purchase);
    }

    public String getPdfUrlForUser(Long userId, String role, Long ebookId) {
        if (!"ADMIN".equals(role) && !purchaseRepository.existsByUserIdAndEbookId(userId, ebookId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You have not purchased this eBook");
        }
        EBook ebook = getEBook(ebookId);
        return ebook.getPdfUrl();
    }

    public List<EBookPurchase> getPurchases() {
        return purchaseRepository.findAll();
    }

    public List<EBookPurchase> getUserPurchases(Long userId) {
        return purchaseRepository.findByUserId(userId);
    }

    @Transactional
    public EBook updateEBook(Long id, String title, String author, String description, Double price,
                             MultipartFile pdfFile, MultipartFile coverImage) throws IOException {
        EBook ebook = getEBook(id);

        if (title != null && !title.isBlank()) ebook.setTitle(title);
        if (author != null && !author.isBlank()) ebook.setAuthor(author);
        if (description != null) ebook.setDescription(description);
        if (price != null) ebook.setPrice(price);

        java.io.File directory = new java.io.File(uploadDir);
        if (!directory.exists()) directory.mkdirs();

        // Replace PDF if a new one was provided
        if (pdfFile != null && !pdfFile.isEmpty()) {
            // Delete old local file if it exists
            if (ebook.getPdfUrl() != null && ebook.getPdfUrl().startsWith("/ebook-uploads/")) {
                String oldFileName = ebook.getPdfUrl().replace("/ebook-uploads/", "");
                java.io.File oldFile = new java.io.File(uploadDir, oldFileName);
                if (oldFile.exists()) oldFile.delete();
            }
            String pdfFileName = System.currentTimeMillis() + "_" + pdfFile.getOriginalFilename();
            java.nio.file.Path pdfPath = java.nio.file.Paths.get(uploadDir, pdfFileName);
            java.nio.file.Files.copy(pdfFile.getInputStream(), pdfPath);
            ebook.setPdfUrl("/ebook-uploads/" + pdfFileName);
        }

        // Replace cover image if a new one was provided
        if (coverImage != null && !coverImage.isEmpty()) {
            // Delete old local file if it exists
            if (ebook.getCoverImageUrl() != null && ebook.getCoverImageUrl().startsWith("/ebook-uploads/")) {
                String oldFileName = ebook.getCoverImageUrl().replace("/ebook-uploads/", "");
                java.io.File oldFile = new java.io.File(uploadDir, oldFileName);
                if (oldFile.exists()) oldFile.delete();
            }
            String coverFileName = System.currentTimeMillis() + "_" + coverImage.getOriginalFilename();
            java.nio.file.Path coverPath = java.nio.file.Paths.get(uploadDir, coverFileName);
            java.nio.file.Files.copy(coverImage.getInputStream(), coverPath);
            ebook.setCoverImageUrl("/ebook-uploads/" + coverFileName);
        }

        return ebookRepository.save(ebook);
    }

    @Transactional
    public void deleteEBook(Long id) {
        EBook ebook = getEBook(id);

        // Delete local PDF file
        if (ebook.getPdfUrl() != null && ebook.getPdfUrl().startsWith("/ebook-uploads/")) {
            String oldFileName = ebook.getPdfUrl().replace("/ebook-uploads/", "");
            new java.io.File(uploadDir, oldFileName).delete();
        }

        // Delete local cover image
        if (ebook.getCoverImageUrl() != null && ebook.getCoverImageUrl().startsWith("/ebook-uploads/")) {
            String oldFileName = ebook.getCoverImageUrl().replace("/ebook-uploads/", "");
            new java.io.File(uploadDir, oldFileName).delete();
        }

        // Delete all purchases for this ebook first (FK constraint)
        purchaseRepository.deleteByEbookId(id);
        ebookRepository.deleteById(id);
    }
}
