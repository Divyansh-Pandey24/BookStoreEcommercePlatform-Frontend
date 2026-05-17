package com.booknest.eureka;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

// Main entry point for the Eureka Service Registry server
@SpringBootApplication
@EnableEurekaServer
public class EurekaServerApplication {

    // Bootstrap and run the Eureka server application
    public static void main(String[] args) {
        SpringApplication.run(EurekaServerApplication.class, args);
    }
}