package com.dev.vault96.util;

import com.dev.vault96.config.security.jwt.JWTService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JWTUtils {
    private final JWTService jwtService;

    public String extractEmailFromRequest(HttpServletRequest request) {
        String token = extractTokenFromHeader(request);
        if (token == null) {
            return null;
        }
        return jwtService.extractUsername(token);
    }

    public String extractTokenFromHeader(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return null;
        }
        return authHeader.substring(7); // "Bearer " 제거 후 토큰 반환
    }

}
