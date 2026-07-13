package com.tcs.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Serves uploaded media (avatar, verification docs) from the local storage path under the public
 * URL prefix /uploads. The mapping uses the same tcs.file.storage.path property the upload
 * service writes to, so the URL stored in media_files.file_url resolves to a real file at request
 * time.
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("${tcs.file.storage.path:uploads}")
    private String storagePath;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Resolve the storage path to an absolute file: URI so the /uploads/** mapping works
        // regardless of the process working directory. Honour an explicit file: prefix if present.
        String location = storagePath.startsWith("file:")
                ? storagePath
                : java.nio.file.Paths.get(storagePath).toAbsolutePath().normalize().toUri().toString();
        registry.addResourceHandler("/uploads/**").addResourceLocations(location);
    }
}