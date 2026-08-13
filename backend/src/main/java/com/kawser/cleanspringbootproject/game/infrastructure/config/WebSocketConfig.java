package com.kawser.cleanspringbootproject.game.infrastructure.config;

import com.kawser.cleanspringbootproject.game.infrastructure.adapter.in.stomp.StompAuthChannelInterceptor;
import com.kawser.cleanspringbootproject.secretmode.infrastructure.adapter.in.stomp.SecretStompAuthChannelInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Both game modes share this single physical /ws STOMP endpoint and broker:
 * secretmode does not get its own @EnableWebSocketMessageBroker
 * configuration (Spring only cleanly supports one), so its auth is wired in
 * here as a second ChannelInterceptor alongside the original game's. Each
 * interceptor cheaply ignores CONNECTs meant for the other mode by checking
 * the room-code header's prefix (see SecretStompAuthChannelInterceptor's
 * javadoc) before touching its own room repository.
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final StompAuthChannelInterceptor authChannelInterceptor;
    private final SecretStompAuthChannelInterceptor secretAuthChannelInterceptor;

    @Value("${app.cors.allowed-origins:}")
    private String allowedOrigins;

    public WebSocketConfig(
            StompAuthChannelInterceptor authChannelInterceptor,
            SecretStompAuthChannelInterceptor secretAuthChannelInterceptor) {
        this.authChannelInterceptor = authChannelInterceptor;
        this.secretAuthChannelInterceptor = secretAuthChannelInterceptor;
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        List<String> origins = new ArrayList<>(List.of(
                "http://localhost:5173", "http://127.0.0.1:5173",
                "http://localhost:5174", "http://127.0.0.1:5174"));
        if (StringUtils.hasText(allowedOrigins)) {
            origins.addAll(Arrays.asList(allowedOrigins.split(",")));
        }

        registry.addEndpoint("/ws")
                .setAllowedOrigins(origins.toArray(new String[0]))
                .setAllowedOriginPatterns("https://*.trycloudflare.com")
                .withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic", "/queue");
        registry.setApplicationDestinationPrefixes("/app");
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(authChannelInterceptor, secretAuthChannelInterceptor);
    }
}
