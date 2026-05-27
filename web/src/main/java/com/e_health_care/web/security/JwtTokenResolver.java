package com.e_health_care.web.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;

import java.util.Arrays;
import java.util.Optional;

/**
 * Lấy JWT từ cookie (theo role) hoặc header Authorization: Bearer.
 */
public final class JwtTokenResolver {

    private JwtTokenResolver() {
    }

    public static Optional<String> resolve(HttpServletRequest request, String... cookieNames) {
        Optional<String> bearer = resolveBearer(request);
        if (bearer.isPresent()) {
            return bearer;
        }
        if (request.getCookies() == null) {
            return Optional.empty();
        }
        return Arrays.stream(request.getCookies())
                .filter(cookie -> Arrays.asList(cookieNames).contains(cookie.getName()))
                .map(Cookie::getValue)
                .filter(value -> value != null && !value.isBlank())
                .findFirst();
    }

    private static Optional<String> resolveBearer(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7).trim();
            if (!token.isEmpty()) {
                return Optional.of(token);
            }
        }
        return Optional.empty();
    }
}
