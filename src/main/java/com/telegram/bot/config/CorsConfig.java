package com.telegram.bot.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * CORS Configuration for Telegram Mini App API
 * Allows requests from Telegram Web App frontend
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
//        registry.addMapping("/**")
//                .allowedOrigins(
//                        "http://localhost:5173",
//                        "http://localhost:5174",
//                        "http://localhost:3000",
//                        "https://localhost:5173",
//                        "https://localhost:5174",
//                        "https://playbuddy.zapto.org",
//                        "http://localhost:3001"
//                )
//                .allowedMethods("*")
//                .allowedHeaders("*")
//                .allowCredentials(true)
//                .maxAge(3600);

        // Also allow from your production Telegram Mini App domain when deployed
        registry.addMapping("/api/**")
                 .allowedOrigins("https://playbuddy.zapto.org")
                 .allowedMethods("*")
                 .allowedHeaders("*")
                 .allowCredentials(true)
                 .maxAge(3600);
    }
}
