package com.booknest.book.config;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Cloudinary Configuration for Book Service.
 *
 * Cloudinary is a cloud-based media management platform.
 * Instead of saving book cover images to local disk (which breaks
 * on multi-instance deployments, Docker volumes, and PaaS platforms),
 * we upload images directly to Cloudinary.
 *
 * Benefits:
 *   - Images are globally accessible via CDN-backed URLs
 *   - Automatic image optimization (WebP conversion, resizing)
 *   - No server disk space consumed
 *   - Images persist across server restarts and redeployments
 *
 * Credentials are loaded from application.properties to keep secrets
 * out of source code. In production, use environment variables.
 *
 * Setup:
 *   1. Sign up at https://cloudinary.com (free tier: 25GB storage)
 *   2. Go to Dashboard → copy cloud_name, api_key, api_secret
 *   3. Add them to book-service application.properties
 */
@Configuration
public class CloudinaryConfig {

    @Value("${cloudinary.cloud-name}")
    private String cloudName;

    @Value("${cloudinary.api-key}")
    private String apiKey;

    @Value("${cloudinary.api-secret}")
    private String apiSecret;

    /**
     * Creates and configures the Cloudinary client bean.
     * This single bean is injected into ImageStorageService.
     */
    @Bean
    public Cloudinary cloudinary() {
        return new Cloudinary(ObjectUtils.asMap(
                "cloud_name", cloudName,
                "api_key",    apiKey,
                "api_secret", apiSecret,
                "secure",     true,   // Always use https:// URLs
                "timeout",    120000,   // Increase timeout to 120s
                "connection_timeout", 120000 // Connection timeout to 120s
        ));
    }
}
