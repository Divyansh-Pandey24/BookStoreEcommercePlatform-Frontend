package com.booknest.ebook.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "ebook_purchases")
@Data
@NoArgsConstructor
public class EBookPurchase {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "ebook_id", nullable = false)
    private EBook ebook;

    @Column(nullable = false)
    private Double amountPaid;

    private LocalDateTime purchasedAt = LocalDateTime.now();
}
