package com.booknest.ebook.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "WALLET-SERVICE", path = "/wallet")
public interface WalletClient {

    @PostMapping("/{userId}/deduct")
    ResponseEntity<Void> deductMoney(
            @RequestHeader("X-Gateway-Secret") String secret,
            @PathVariable("userId") Long userId,
            @RequestParam("amount") Double amount,
            @RequestParam(value = "orderId", required = false) Long orderId
    );
}
