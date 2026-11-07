package com.apartment.maintenance.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${app.upload-dir:uploads}")
    private String uploadDir;

    /** Lets the React dev server call the API from a different port. */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins(
                        "http://localhost:3000",   // create-react-app
                        "http://localhost:5173"    // vite
                )
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*");
    }

    /** Serves locally saved photos at /photos/<file> while S3 is turned off. */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String folder = Paths.get(uploadDir).toAbsolutePath().normalize().toString();
        registry.addResourceHandler("/photos/**")
                .addResourceLocations("file:" + folder + "/");
    }
}
