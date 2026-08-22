package com.kawser.cleanspringbootproject.user.infrastructure.config;

import com.kawser.cleanspringbootproject.user.application.port.out.TokenIssuer;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

/**
 * Populates the SecurityContext from a Bearer JWT, if present and valid.
 * Fixes a NullPointerException present in the reference implementation this
 * module is based on: its token validator returned "" (not null/Optional)
 * for an invalid token, and its filter used the lookup result without a
 * null check, so an invalid/tampered token could crash the request instead
 * of simply leaving it unauthenticated. Here, both the token subject
 * (Optional<String>) and the loaded UserDetails are checked explicitly
 * before touching the SecurityContext.
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final TokenIssuer tokenIssuer;
    private final UserDetailsService userDetailsService;

    public JwtAuthenticationFilter(TokenIssuer tokenIssuer, UserDetailsService userDetailsService) {
        this.tokenIssuer = tokenIssuer;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String token = extractToken(request);

        if (token != null) {
            Optional<String> subject = tokenIssuer.validateToken(token);
            if (subject.isPresent()) {
                authenticate(subject.get(), request);
            }
        }

        filterChain.doFilter(request, response);
    }

    private void authenticate(String username, HttpServletRequest request) {
        UserDetails userDetails;
        try {
            userDetails = userDetailsService.loadUserByUsername(username);
        } catch (UsernameNotFoundException e) {
            return;
        }
        if (userDetails == null) {
            return;
        }

        var authentication = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities());
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            return null;
        }
        return header.substring("Bearer ".length());
    }
}
