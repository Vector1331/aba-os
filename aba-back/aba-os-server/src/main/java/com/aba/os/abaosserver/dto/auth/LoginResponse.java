package com.aba.os.abaosserver.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
@AllArgsConstructor
public class LoginResponse {

    private String accessToken;
    private String refreshToken;
    private UserInfo user;

    @Getter
    @Builder
    @AllArgsConstructor
    public static class UserInfo {
        private UUID id;
        private String name;
        private String role;
        private UUID centerId;
    }
}
