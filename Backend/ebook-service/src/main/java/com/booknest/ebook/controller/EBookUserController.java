package com.booknest.ebook.controller;

import com.booknest.ebook.entity.EBookPurchase;
import com.booknest.ebook.service.EBookService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/ebooks/user")
public class EBookUserController {

    private final EBookService ebookService;

    public EBookUserController(EBookService ebookService) {
        this.ebookService = ebookService;
    }

    @GetMapping("/{ebookId}")
    public ResponseEntity<com.booknest.ebook.entity.EBook> getEBook(@PathVariable Long ebookId) {
        return ResponseEntity.ok(ebookService.getEBook(ebookId));
    }

    @PostMapping("/{ebookId}/purchase")
    public ResponseEntity<Map<String, String>> purchaseEBook(@RequestHeader("X-User-Id") Long userId, @PathVariable Long ebookId) {
        ebookService.purchaseEBook(userId, ebookId);
        return ResponseEntity.ok(Map.of("message", "EBook purchased successfully"));
    }

    @GetMapping("/{ebookId}/read")
    public ResponseEntity<Map<String, String>> readEBook(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @PathVariable Long ebookId) {
        
        if (userId == null) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.UNAUTHORIZED, "User ID missing");
        }
        
        String pdfUrl = ebookService.getPdfUrlForUser(userId, role, ebookId);
        return ResponseEntity.ok(Map.of("pdfUrl", pdfUrl));
    }

    @GetMapping("/purchases")
    public ResponseEntity<List<EBookPurchase>> getMyPurchases(@RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(ebookService.getUserPurchases(userId));
    }
}
