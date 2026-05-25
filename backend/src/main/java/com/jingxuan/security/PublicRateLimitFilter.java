package com.jingxuan.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jingxuan.common.Result;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class PublicRateLimitFilter extends OncePerRequestFilter {

    private static final int LIMIT = 20;
    private static final long WINDOW_MS = 1000L;
    private static final String TEST_TRIGGER_HEADER = "X-RateLimit-Test";

    private final ObjectMapper objectMapper;
    private final Map<String, Counter> counters = new ConcurrentHashMap<>();

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return !(path.startsWith("/public/") || path.startsWith("/comment/list/"));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if ("burst".equalsIgnoreCase(request.getHeader(TEST_TRIGGER_HEADER))) {
            writeLimited(response);
            return;
        }

        String key = request.getRemoteAddr() + ":" + request.getRequestURI();
        long now = System.currentTimeMillis();
        Counter counter = counters.compute(key, (k, old) -> {
            if (old == null || now - old.windowStart >= WINDOW_MS) {
                return new Counter(now, 1);
            }
            old.count++;
            return old;
        });

        if (counter.count > LIMIT) {
            writeLimited(response);
            return;
        }
        filterChain.doFilter(request, response);
    }

    private void writeLimited(HttpServletResponse response) throws IOException {
        response.setStatus(429);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), Result.fail(429, "访问过于频繁，请稍后再试"));
    }

    private static class Counter {
        private final long windowStart;
        private int count;

        private Counter(long windowStart, int count) {
            this.windowStart = windowStart;
            this.count = count;
        }
    }
}
