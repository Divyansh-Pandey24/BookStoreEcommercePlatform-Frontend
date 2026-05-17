package com.booknest.admin;

import de.codecentric.boot.admin.server.config.EnableAdminServer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

// ============================================================
// WHAT THIS CLASS DOES:
//   @SpringBootApplication  → standard Spring Boot bootstrap
//   @EnableAdminServer      → activates the Admin Server UI and REST API
//                             served at http://localhost:9090
//   @EnableDiscoveryClient  → registers this server with Eureka AND
//                             lets Admin Server auto-discover all other
//                             Eureka-registered services without each
//                             service needing to point back to us.
// ============================================================
@SpringBootApplication
@EnableAdminServer
@EnableDiscoveryClient
public class AdminServerApplication {

    // Bootstrap the Spring Boot application.
    // Admin UI is available immediately after startup at:
    //   http://localhost:9090
    // Login with credentials defined in application.properties.
    public static void main(String[] args) {
        SpringApplication.run(AdminServerApplication.class, args);
    }
}
