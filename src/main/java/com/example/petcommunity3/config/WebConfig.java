package com.example.petcommunity3.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Value("${petcommunity.upload.path:uploads}")
    private String uploadPath;

    @Value("${petcommunity.upload.url-prefix:/uploads}")
    private String urlPrefix;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path path = Paths.get(uploadPath);
        if (!path.isAbsolute()) {
            path = Paths.get("").toAbsolutePath().resolve(path);
        }

        String pattern = normalizeUrlPrefix() + "/**";
        String location = path.normalize().toUri().toString();
        registry.addResourceHandler(pattern).addResourceLocations(location);
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(false)
                .maxAge(3600);
        registry.addMapping(normalizeUrlPrefix() + "/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(false)
                .maxAge(3600);
    }

    private String normalizeUrlPrefix() {
        String prefix = urlPrefix == null || urlPrefix.isBlank() ? "/uploads" : urlPrefix.trim();
        return prefix.startsWith("/") ? prefix : "/" + prefix;
    }
}
