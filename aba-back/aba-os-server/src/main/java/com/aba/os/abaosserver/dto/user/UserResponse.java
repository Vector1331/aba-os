package com.aba.os.abaosserver.dto.user;

import com.aba.os.abaosserver.domain.User;
import com.aba.os.abaosserver.domain.User.UserRole;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class UserResponse {
    private UUID id;
    private String email;
    private String name;
    private UserRole role;
    private String centerName;
    private UUID centerId;
    private LocalDateTime lastLoginAt;
    private LocalDateTime createdAt;

    public static UserResponse from(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .role(user.getRole())
                .centerId(user.getCenter().getId())
                .centerName(user.getCenter().getName())
                .lastLoginAt(user.getLastLoginAt())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
