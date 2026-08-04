package com.kawser.cleanspringbootproject.game.infrastructure.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Allows the Vite dev frontend to call the plain REST endpoints (room
 * create/join) from the browser. Separate from the STOMP endpoint's
 * allowed origins in WebSocketConfig, which only cover the WebSocket
 * handshake, not regular HTTP requests.
 */
@Configuration
public class WebCorsConfig implements WebMvcConfigurer {

    @Value("${app.cors.allowed-origins:}")
    private String allowedOrigins;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        List<String> origins = new ArrayList<>(List.of(
                "http://localhost:5173", "http://127.0.0.1:5173",
                "http://localhost:5174", "http://127.0.0.1:5174"));
        if (StringUtils.hasText(allowedOrigins)) {
            origins.addAll(Arrays.asList(allowedOrigins.split(",")));
        }

        registry.addMapping("/**")
                .allowedOrigins(origins.toArray(new String[0]))
                .allowedOriginPatterns("https://*.trycloudflare.com")
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS");
    }
}
