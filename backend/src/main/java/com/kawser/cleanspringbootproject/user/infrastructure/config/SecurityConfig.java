package com.kawser.cleanspringbootproject.user.infrastructure.config;

import com.kawser.cleanspringbootproject.user.application.port.out.TokenIssuer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.http.HttpMethod;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

/**
 * Adding spring-boot-starter-security makes Spring Security intercept every
 * route by default, including the game module's REST/WebSocket endpoints
 * that were previously wide open (see game.infrastructure.config.
 * WebCorsConfig/WebSocketConfig) — every one of those has to be re-declared
 * permitAll here explicitly, or the game breaks the moment this dependency
 * lands. CORS itself is still handled by WebCorsConfig's WebMvcConfigurer;
 * .cors(withDefaults()) here only tells Security to let CORS-preflighted
 * requests through the filter chain instead of blocking them.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final TokenIssuer tokenIssuer;
    private final UserDetailsService userDetailsService;
    private final CorsConfigurationSource corsConfigurationSource;

    public SecurityConfig(TokenIssuer tokenIssuer, UserDetailsService userDetailsService,
            CorsConfigurationSource corsConfigurationSource) {
        this.tokenIssuer = tokenIssuer;
        this.userDetailsService = userDetailsService;
        this.corsConfigurationSource = corsConfigurationSource;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // CORS preflight requests never carry the Authorization header,
                        // so PATCH/PUT endpoints like /users/me and /users/me/password
                        // (which the browser always preflights, unlike simple GET/POST)
                        // would otherwise get rejected by anyRequest().authenticated()
                        // below before the actual request is even sent.
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        // user module - public auth endpoints
                        .requestMatchers("/auth/signup", "/auth/login", "/auth/verify-account",
                                "/auth/resend-verification").permitAll()
                        .requestMatchers("/password/request-reset", "/password/reset").permitAll()
                        // game module - unchanged from its pre-Security behavior
                        .requestMatchers("/rooms", "/rooms/daily", "/rooms/*/join").permitAll()
                        .requestMatchers("/ws/**").permitAll()
                        // leaderboard module - public standings, visible to guests too
                        .requestMatchers("/leaderboard/**").permitAll()
                        .requestMatchers("/debug/**").permitAll()
                        // ops/docs
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html").permitAll()
                        .requestMatchers("/actuator/**").permitAll()
                        // Spring's own error dispatch (e.g. a malformed request body that
                        // never reaches a controller) must stay public too, or a client
                        // error gets masked as a 403 instead of surfacing its real status.
                        .requestMatchers("/error").permitAll()
                        .anyRequest().authenticated())
                .addFilterBefore(new JwtAuthenticationFilter(tokenIssuer, userDetailsService),
                        UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }
}
