package com.tcs.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${tcs.file.storage.path:uploads}")
    private String storagePath;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String absolutePath = java.nio.file.Paths.get(storagePath).toAbsolutePath().normalize().toUri().toString();
        if (!absolutePath.endsWith("/")) {
            absolutePath = absolutePath + "/";
        }
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(absolutePath);
        registry.addResourceHandler("/uploads/public/**")
                .addResourceLocations(absolutePath + "public/");
        registry.addResourceHandler("/uploads/avatars/**")
                .addResourceLocations(absolutePath + "avatars/");
    }
}
