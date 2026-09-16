package com.example.eventday.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${upload.dir:uploads}")
    private String uploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Serve /uploads/** dari folder lokal uploads/ agar frontend bisa <img src="/uploads/avatars/...">
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + uploadDir + "/");
        // Also expose logos subfolder explicitly
        registry.addResourceHandler("/uploads/logos/**")
                .addResourceLocations("file:" + uploadDir + "/logos/");
    }
}
