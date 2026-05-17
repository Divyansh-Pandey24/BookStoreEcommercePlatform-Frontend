package com.booknest.ebook.controller;

import com.booknest.ebook.entity.EBook;
import com.booknest.ebook.entity.EBookPurchase;
import com.booknest.ebook.service.EBookService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/ebooks/admin")
public class EBookAdminController {

    private final EBookService ebookService;

    public EBookAdminController(EBookService ebookService) {
        this.ebookService = ebookService;
    }

    private void checkAdminRole(String role) {
        if (!"ADMIN".equals(role)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access Denied: Admins only");
        }
    }

    @PostMapping(consumes = "multipart/form-data")
    public ResponseEntity<EBook> createEBook(
            @RequestHeader("X-User-Role") String role,
            @RequestParam("title") String title,
            @RequestParam("author") String author,
            @RequestParam("description") String description,
            @RequestParam("price") Double price,
            @RequestParam("pdfFile") MultipartFile pdfFile,
            @RequestParam(value = "coverImage", required = false) MultipartFile coverImage) throws Exception {
        
        checkAdminRole(role);
        return ResponseEntity.ok(ebookService.createEBook(title, author, description, price, pdfFile, coverImage));
    }

    @GetMapping("/purchases")
    public ResponseEntity<List<EBookPurchase>> getAllPurchases(@RequestHeader(value = "X-User-Role", required = false) String role) {
        checkAdminRole(role);
        return ResponseEntity.ok(ebookService.getPurchases());
    }

    @GetMapping
    public ResponseEntity<List<EBook>> getAllEBooks(@RequestHeader(value = "X-User-Role", required = false) String role) {
        checkAdminRole(role);
        return ResponseEntity.ok(ebookService.getAllEBooks());
    }

    @PutMapping(value = "/{id}", consumes = "multipart/form-data")
    public ResponseEntity<EBook> updateEBook(
            @RequestHeader("X-User-Role") String role,
            @PathVariable Long id,
            @RequestParam(value = "title", required = false) String title,
            @RequestParam(value = "author", required = false) String author,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "price", required = false) Double price,
            @RequestParam(value = "pdfFile", required = false) MultipartFile pdfFile,
            @RequestParam(value = "coverImage", required = false) MultipartFile coverImage) throws Exception {

        checkAdminRole(role);
        return ResponseEntity.ok(ebookService.updateEBook(id, title, author, description, price, pdfFile, coverImage));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEBook(
            @RequestHeader("X-User-Role") String role,
            @PathVariable Long id) {
        checkAdminRole(role);
        ebookService.deleteEBook(id);
        return ResponseEntity.noContent().build();
    }
}
