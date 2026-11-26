package com.taller.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Configuración web para:
 * 1. Servir de forma estática las imágenes subidas por los mecánicos en "uploads/".
 * 2. Servir los archivos estáticos del frontend directamente desde la carpeta "frontend/"
 *    (en desarrollo local) o desde "classpath:/static/" (al compilar / empaquetar en JAR).
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

        // Rutas a la carpeta frontend para desarrollo local sin duplicar archivos
        Path frontendDir = Paths.get("..", "frontend").toAbsolutePath().normalize();
        String frontendPath = frontendDir.toUri().toString();
        if (!frontendPath.endsWith("/")) {
            frontendPath += "/";
        }

        Path frontendLocalDir = Paths.get("frontend").toAbsolutePath().normalize();
        String frontendLocalPath = frontendLocalDir.toUri().toString();
        if (!frontendLocalPath.endsWith("/")) {
            frontendLocalPath += "/";
        }

        registry.addResourceHandler("/**")
                .addResourceLocations(
                        frontendPath,
                        frontendLocalPath,
                        "classpath:/static/"
                );
    }

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        // Redirigir la raíz "/" a "/index.html"
        registry.addViewController("/").setViewName("forward:/index.html");
    }
}
