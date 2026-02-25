package com.springbootangular.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Value("${cors.allowed-origins:http://localhost:4200}")
    private String[] allowedOrigins;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins(allowedOrigins)
                .allowedMethods("DELETE", "GET", "HEAD", "OPTIONS", "POST", "PUT")
                .allowedHeaders("Origin", "X-Requested-With", "Content-Type", "Accept", "Key", "Authorization")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
