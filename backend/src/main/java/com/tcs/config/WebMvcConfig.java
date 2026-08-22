package com.tcs.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Serves public uploaded media (avatars) from the local storage path under the URL prefix
 * /uploads/public. Private files (CCCD, licenses, verification docs) are served through
 * FileAccessController with authentication and authorization checks.
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final String storagePath;
    private final MaintenanceModeInterceptor maintenanceModeInterceptor;

    public WebMvcConfig(
            @Value("${tcs.file.storage.path:uploads}") String storagePath,
            MaintenanceModeInterceptor maintenanceModeInterceptor) {
        this.storagePath = storagePath;
        this.maintenanceModeInterceptor = maintenanceModeInterceptor;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String basePath = java.nio.file.Paths.get(storagePath).toAbsolutePath().normalize().toUri().toString();
        if (!basePath.endsWith("/")) {
            basePath += "/";
        }
        registry.addResourceHandler("/uploads/**").addResourceLocations(basePath);
        registry.addResourceHandler("/uploads/public/**").addResourceLocations(basePath + "public/");
        registry.addResourceHandler("/uploads/avatars/**").addResourceLocations(basePath + "avatars/");
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(maintenanceModeInterceptor);
    }
}