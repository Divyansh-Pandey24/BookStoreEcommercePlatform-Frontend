package com.booknest.order.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

/*
 * Matches WalletResponse from wallet-service.
 * Used to check balance before online payment.
 */
@Data
@NoArgsConstructor
public class WalletDto {
    private Long   walletId;
    private Long   userId;
    private Double currentBalance;
}