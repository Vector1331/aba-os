package com.aba.os.abaosserver.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class SecurityUtil {

    private final JwtTokenProvider jwtTokenProvider;

    public SecurityUtil(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }

    public UUID getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getCredentials() != null) {
            String token = (String) authentication.getCredentials();
            return jwtTokenProvider.getUserIdFromToken(token);
        }
        throw new IllegalStateException("인증 정보를 찾을 수 없습니다.");
    }

    public UUID getCurrentCenterId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getCredentials() != null) {
            String token = (String) authentication.getCredentials();
            String centerId = jwtTokenProvider.parseClaims(token).get("centerId", String.class);
            return UUID.fromString(centerId);
        }
        throw new IllegalStateException("인증 정보를 찾을 수 없습니다.");
    }

    public String getCurrentUserRole() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getCredentials() != null) {
            String token = (String) authentication.getCredentials();
            return jwtTokenProvider.parseClaims(token).get("role", String.class);
        }
        throw new IllegalStateException("인증 정보를 찾을 수 없습니다.");
    }
}
