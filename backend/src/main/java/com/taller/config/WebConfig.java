package com.taller.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Configuración web para servir de forma estática las imágenes subidas por los mecánicos
 * en la carpeta local "uploads/" bajo la ruta pública "/uploads/**".
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path uploadDir = Paths.get("uploads").toAbsolutePath().normalize();
        String uploadPath = uploadDir.toUri().toString();
        if (!uploadPath.endsWith("/")) {
            uploadPath += "/";
        }

        Path backendUploadDir = Paths.get("backend", "uploads").toAbsolutePath().normalize();
        String backendUploadPath = backendUploadDir.toUri().toString();
        if (!backendUploadPath.endsWith("/")) {
            backendUploadPath += "/";
        }

        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(uploadPath, backendUploadPath);
    }
}
