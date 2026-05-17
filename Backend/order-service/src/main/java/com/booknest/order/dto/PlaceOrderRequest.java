package com.booknest.order.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/*
 * What customer sends when placing an order.
 *
 * paymentMode: "COD" or "WALLET"
 * Delivery address fields: snapshot saved to order
 *
 * We do NOT receive cart items here.
 * We fetch them from Cart Service using Feign.
 * Customer just tells us: how to pay and where to deliver.
 */
@Data
public class PlaceOrderRequest {

    /*
     * Payment mode — only two valid values.
     * We validate this in the service layer.
     */
    @NotBlank(message = "Payment mode is required")
    private String paymentMode; // "COD" or "WALLET"

    @NotBlank(message = "Full name is required")
    private String deliveryName;

    @NotBlank(message = "Mobile number is required")
    private String deliveryMobile;

    @NotBlank(message = "Address is required")
    private String deliveryAddress;

    @NotBlank(message = "City is required")
    private String deliveryCity;

    @NotBlank(message = "Pincode is required")
    private String deliveryPincode;

    @NotBlank(message = "State is required")
    private String deliveryState;
}