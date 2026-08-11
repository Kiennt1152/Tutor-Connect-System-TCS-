package com.tcs.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Serves public uploaded media (avatars) from the local storage path under the URL prefix
 * /uploads/public. Private files (CCCD, licenses, verification docs) are served through
 * FileAccessController with authentication and authorization checks.
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("${tcs.file.storage.path:uploads}")
    private String storagePath;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Only serve public uploads (avatars). Private files are behind /api/files/private/{id}.
        String publicPath = storagePath.startsWith("file:") ? storagePath : "file:" + storagePath + "/public/";
        registry.addResourceHandler("/uploads/public/**").addResourceLocations(publicPath);
    }
}