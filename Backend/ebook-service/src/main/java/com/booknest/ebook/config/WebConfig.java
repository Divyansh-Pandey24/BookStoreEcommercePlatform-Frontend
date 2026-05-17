package com.booknest.ebook.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${app.upload-dir}")
    private String uploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String path = new java.io.File(uploadDir).getAbsolutePath();
        if (!path.endsWith("/")) {
            path += "/";
        }
        
        registry.addResourceHandler("/ebook-uploads/**")
                .addResourceLocations("file:" + path);
    }
}
