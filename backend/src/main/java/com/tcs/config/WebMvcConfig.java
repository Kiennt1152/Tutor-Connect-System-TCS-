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
        // Default location is "uploads" relative to the working directory, e.g. C:\...\backend\
        // uploads when launched from the IDE or jar. If storagePath already starts with "file:" we
        // use it verbatim, otherwise we prepend the file: scheme so Spring resolves it as an
        // absolute filesystem location.
        String location = storagePath.startsWith("file:") ? storagePath : "file:" + storagePath + "/";
        registry.addResourceHandler("/uploads/**").addResourceLocations(location);
    }
}