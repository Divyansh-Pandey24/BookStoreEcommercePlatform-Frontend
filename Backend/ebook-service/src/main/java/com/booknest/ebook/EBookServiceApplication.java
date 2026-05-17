package com.booknest.ebook;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

// Main entry point for the EBook Service microservice
@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
public class EBookServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(EBookServiceApplication.class, args);
    }

}