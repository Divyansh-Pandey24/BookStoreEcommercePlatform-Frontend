package com.booknest.ebook.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "ebooks")
@Data
@NoArgsConstructor
public class EBook {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String author;

    @Column(length = 2000)
    private String description;

    @Column(nullable = false)
    private Double price;

    @Column(nullable = false)
    private String pdfUrl;

    private String coverImageUrl;

    private Boolean active = true;

    private LocalDateTime createdAt = LocalDateTime.now();
}
