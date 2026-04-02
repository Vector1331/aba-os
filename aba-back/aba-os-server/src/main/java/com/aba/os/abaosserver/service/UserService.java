package com.aba.os.abaosserver.service;

import com.aba.os.abaosserver.domain.User;
import com.aba.os.abaosserver.dto.user.UserResponse;
import com.aba.os.abaosserver.dto.user.UserUpdateRequest;
import com.aba.os.abaosserver.repository.UserRepository;
import com.aba.os.abaosserver.security.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final SecurityUtil securityUtil;

    /**
     * 현재 로그인된 사용자 정보 조회
     */
    public UserResponse getCurrentUser() {
        Long userId = securityUtil.getCurrentUserId();

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        return UserResponse.from(user);
    }

    /**
     * 현재 로그인된 사용자 정보 수정
     */
    @Transactional
    public UserResponse updateCurrentUser(UserUpdateRequest request) {
        Long userId = securityUtil.getCurrentUserId();

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        user.updateProfile(request.getName());

        log.info("사용자 정보 수정 완료 - ID: {}, 이름: {}", userId, user.getName());

        return UserResponse.from(user);
    }
}
