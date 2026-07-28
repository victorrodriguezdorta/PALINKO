package com.kawser.cleanspringbootproject.game.infrastructure.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Allows the Vite dev frontend to call the plain REST endpoints (room
 * create/join) from the browser. Separate from the STOMP endpoint's
 * allowed origins in WebSocketConfig, which only cover the WebSocket
 * handshake, not regular HTTP requests.
 */
@Configuration
public class WebCorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins(
                        "http://localhost:5173", "http://127.0.0.1:5173",
                        "http://localhost:5174", "http://127.0.0.1:5174")
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS");
    }
}
