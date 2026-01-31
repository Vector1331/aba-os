package com.aba.os.abaosserver.service;

import com.aba.os.abaosserver.domain.Center;
import com.aba.os.abaosserver.domain.User;
import com.aba.os.abaosserver.dto.auth.LoginRequest;
import com.aba.os.abaosserver.dto.auth.LoginResponse;
import com.aba.os.abaosserver.dto.auth.RegisterRequest;
import com.aba.os.abaosserver.repository.CenterRepository;
import com.aba.os.abaosserver.repository.UserRepository;
import com.aba.os.abaosserver.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final UserRepository userRepository;
    private final CenterRepository centerRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @Transactional
    public void register(RegisterRequest request) {
        // 이메일 중복 체크
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
        }

        // 초대 코드로 센터 조회
        Center center = centerRepository.findByInviteCode(request.getInviteCode())
                .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 초대 코드입니다."));

        // 사용자 생성
        User user = User.builder()
                .center(center)
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .name(request.getName())
                .role(request.getRole())
                .build();

        userRepository.save(user);
    }

    @Transactional
    public LoginResponse login(LoginRequest request) {
        // 사용자 조회
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("이메일 또는 비밀번호가 올바르지 않습니다."));

        // 비밀번호 검증
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("이메일 또는 비밀번호가 올바르지 않습니다.");
        }

        // 토큰 생성
        String accessToken = jwtTokenProvider.createAccessToken(
                user.getId(),
                user.getEmail(),
                user.getRole().name(),
                user.getCenter().getId()
        );
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getId());

        // 마지막 로그인 시간 업데이트
        user.updateLastLoginAt(LocalDateTime.now());

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .user(LoginResponse.UserInfo.builder()
                        .id(user.getId())
                        .name(user.getName())
                        .role(user.getRole().name())
                        .centerId(user.getCenter().getId())
                        .build())
                .build();
    }
}
