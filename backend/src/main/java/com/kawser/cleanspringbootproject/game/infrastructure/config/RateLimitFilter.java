package com.kawser.cleanspringbootproject.game.infrastructure.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Caps every client IP to a fixed number of requests per UTC day, as a
 * cheap first line of defense against request-flooding abuse — there is no
 * login/session to key on (see Player's per-room, ephemeral identity), so
 * the client's source IP is the only identity axis available this early in
 * the request pipeline. Counters live purely in memory (a single backend
 * instance, same as InMemoryRoomRepository) and are keyed by (IP, date) so
 * yesterday's entries are simply never touched again rather than requiring
 * an explicit reset sweep.
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private final int maxRequestsPerDay;
    private final ConcurrentHashMap<String, AtomicInteger> requestCounts = new ConcurrentHashMap<>();

    public RateLimitFilter(@Value("${game.rate-limit.max-requests-per-day}") int maxRequestsPerDay) {
        this.maxRequestsPerDay = maxRequestsPerDay;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String key = clientIp(request) + "|" + LocalDate.now(ZoneOffset.UTC);
        int count = requestCounts.computeIfAbsent(key, ignored -> new AtomicInteger()).incrementAndGet();
        if (count > maxRequestsPerDay) {
            response.setStatus(429); // 429 Too Many Requests
            response.setContentType("application/json");
            response.getWriter().write("{\"message\":\"Daily request limit exceeded\"}");
            return;
        }
        filterChain.doFilter(request, response);
    }

    /**
     * Prefers X-Forwarded-For's first hop over getRemoteAddr() since the
     * app typically sits behind a tunnel/proxy (see WebCorsConfig's
     * trycloudflare.com origin) that would otherwise make every client
     * look like the same proxy IP.
     */
    private String clientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
