package com.booknest.order.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// Configuration for generating Swagger OpenAPI documentation
@Configuration
@OpenAPIDefinition(
    info = @Info(
        title = "Order Service API", 
        description = "Handles order lifecycle and orchestration",
        version = "v1"
    ),
    security = @SecurityRequirement(name = "bearerAuth")
)
public class SwaggerConfig {

    // Define security schemes and global server configuration for Swagger UI
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .servers(List.of(new Server().url("http://localhost:8080/api").description("API Gateway")))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")));
    }
}

