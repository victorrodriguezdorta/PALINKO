package com.kawser.cleanspringbootproject.game.infrastructure.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
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
 *
 * <p>Also exposes the same origin list as a CorsConfigurationSource bean,
 * which user.infrastructure.config.SecurityConfig wires into the Spring
 * Security filter chain — Security's own CORS handling does not pick up
 * WebMvcConfigurer.addCorsMappings automatically, so both need to agree on
 * one origin list rather than maintaining it twice.
 */
@Configuration
public class WebCorsConfig implements WebMvcConfigurer {

    @Value("${app.cors.allowed-origins:}")
    private String allowedOrigins;

    private List<String> allowedOriginsList() {
        List<String> origins = new ArrayList<>(List.of(
                "http://localhost:5173", "http://127.0.0.1:5173",
                "http://localhost:5174", "http://127.0.0.1:5174"));
        if (StringUtils.hasText(allowedOrigins)) {
            origins.addAll(Arrays.asList(allowedOrigins.split(",")));
        }
        return origins;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // Kept separate from "/**" below for the same reason as the
        // credentialed /ws/** entry in corsConfigurationSource(): a
        // wildcard Allow-Headers stops working once Allow-Credentials is
        // true, so /ws/** (SockJS, credentialed, no Authorization header)
        // and everything else (uncredentialed REST calls that do send
        // Authorization) need their own registrations.
        registry.addMapping("/ws/**")
                .allowedOrigins(allowedOriginsList().toArray(new String[0]))
                .allowedOriginPatterns("https://*.trycloudflare.com")
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowCredentials(true);

        registry.addMapping("/**")
                .allowedOrigins(allowedOriginsList().toArray(new String[0]))
                .allowedOriginPatterns("https://*.trycloudflare.com")
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("Authorization", "Content-Type");
    }

    /**
     * Two separate configurations, not one shared by every route: per the
     * CORS spec, Access-Control-Allow-Headers: "*" is treated literally
     * (not as a wildcard) once Allow-Credentials is true, so a single
     * credentialed config for "/**" would silently stop allowing the
     * Authorization header on every plain REST call (auth/users/rooms) —
     * that's exactly what broke PUT /users/me/password after credentials
     * were enabled for SockJS. /ws/** keeps allowCredentials(true) with a
     * wildcard header list (SockJS doesn't send Authorization, so the
     * wildcard-vs-credentials interaction never bites there); every other
     * route stays uncredentialed (this API uses no cookies) with an
     * explicit header list that includes Authorization.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration restConfiguration = new CorsConfiguration();
        restConfiguration.setAllowedOrigins(allowedOriginsList());
        restConfiguration.addAllowedOriginPattern("https://*.trycloudflare.com");
        restConfiguration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        restConfiguration.setAllowedHeaders(List.of("Authorization", "Content-Type"));

        CorsConfiguration webSocketConfiguration = new CorsConfiguration();
        webSocketConfiguration.setAllowedOrigins(allowedOriginsList());
        webSocketConfiguration.addAllowedOriginPattern("https://*.trycloudflare.com");
        webSocketConfiguration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        webSocketConfiguration.setAllowedHeaders(List.of("*"));
        webSocketConfiguration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/ws/**", webSocketConfiguration);
        source.registerCorsConfiguration("/**", restConfiguration);
        return source;
    }
}
