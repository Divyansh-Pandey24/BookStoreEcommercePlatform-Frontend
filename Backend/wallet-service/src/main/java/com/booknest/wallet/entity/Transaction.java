package com.booknest.wallet.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

// Entity representing an individual wallet transaction (Credit/Debit)
@Entity
@Data
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;
    private Double amount;
    private String type;
    private String status;
    private Long orderId;
    private LocalDateTime createdAt;
}