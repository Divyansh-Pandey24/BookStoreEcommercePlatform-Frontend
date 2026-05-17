package com.booknest.wallet.dto;

public class RazorpayOrderRequest {
    private Double amount;
    private Long userId;

    public Double getAmount() { return amount; }
    public void setAmount(Double amount) { this.amount = amount; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
}