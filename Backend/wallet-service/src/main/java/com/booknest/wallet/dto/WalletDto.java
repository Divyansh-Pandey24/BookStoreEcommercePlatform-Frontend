package com.booknest.wallet.dto;

public class WalletDto {

    private Long userId;
    private Double currentBalance;

    public WalletDto(Long userId, Double currentBalance) {
        this.userId = userId;
        this.currentBalance = currentBalance;
    }

    public Long getUserId() { return userId; }
    public Double getCurrentBalance() { return currentBalance; }
}