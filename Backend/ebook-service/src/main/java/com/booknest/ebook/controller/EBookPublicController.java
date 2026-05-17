package com.booknest.ebook.controller;

import com.booknest.ebook.entity.EBook;
import com.booknest.ebook.service.EBookService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/ebooks/public")
public class EBookPublicController {

    private final EBookService ebookService;

    public EBookPublicController(EBookService ebookService) {
        this.ebookService = ebookService;
    }

    @GetMapping
    public ResponseEntity<List<EBook>> getActiveEBooks() {
        return ResponseEntity.ok(ebookService.getActiveEBooks());
    }
}
