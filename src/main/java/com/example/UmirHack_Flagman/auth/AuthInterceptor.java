package com.example.UmirHack_Flagman.auth;

import java.util.UUID;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import com.example.UmirHack_Flagman.common.ApiException;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class AuthInterceptor implements HandlerInterceptor {
    public static final String USER_ID_ATTR = "auth.userId";
    public static final String ROLE_ATTR = "auth.role";
    private static final String BEARER = "Bearer ";

    private final JwtService jwtService;

    public AuthInterceptor(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if ("OPTIONS".equals(request.getMethod())) {
            return true;
        }

        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith(BEARER)) {
            throw unauthorized();
        }

        try {
            Claims claims = jwtService.parse(header.substring(BEARER.length()));
            request.setAttribute(USER_ID_ATTR, UUID.fromString(claims.getSubject()));
            request.setAttribute(ROLE_ATTR, claims.get("role", String.class));
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            throw unauthorized();
        }
    }

    private static ApiException unauthorized() {
        return new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Требуется действительный access token");
    }
}
