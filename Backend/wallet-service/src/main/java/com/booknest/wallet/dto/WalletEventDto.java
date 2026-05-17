package com.booknest.wallet.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WalletEventDto {
    private Long   userId;
    private String type;
    private String message;
    private String userEmail;      // ← Added
    private String userMobile;     // ← Added
}