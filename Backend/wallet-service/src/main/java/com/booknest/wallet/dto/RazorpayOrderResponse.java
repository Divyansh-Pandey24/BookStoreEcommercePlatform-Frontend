package com.booknest.wallet.dto;

public class RazorpayOrderResponse {

    /*
     * BEFORE: orderId, key
     * AFTER:  razorpayOrderId, keyId
     *
     * React code reads response.razorpayOrderId and response.keyId
     * If field names don't match → React gets undefined → popup fails
     */
    private String razorpayOrderId;
    private String keyId;
    private Double amount;
    private String currency;

    public RazorpayOrderResponse(
            String razorpayOrderId,
            String keyId,
            Double amount,
            String currency) {
        this.razorpayOrderId = razorpayOrderId;
        this.keyId           = keyId;
        this.amount          = amount;
        this.currency        = currency;
    }

    public String getRazorpayOrderId() { return razorpayOrderId; }
    public String getKeyId()           { return keyId; }
    public Double getAmount()          { return amount; }
    public String getCurrency()        { return currency; }
}