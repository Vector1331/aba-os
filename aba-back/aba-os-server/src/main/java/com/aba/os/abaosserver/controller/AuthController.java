package com.aba.os.abaosserver.controller;

import com.aba.os.abaosserver.common.ApiResponse;
import com.aba.os.abaosserver.dto.auth.LoginRequest;
import com.aba.os.abaosserver.dto.auth.LoginResponse;
import com.aba.os.abaosserver.dto.auth.RegisterRequest;
import com.aba.os.abaosserver.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "0. 인증 (Auth)", description = "로그인/회원가입 API - 먼저 로그인하여 토큰을 발급받으세요")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @SecurityRequirements  // 인증 불필요 표시
    @Operation(
            summary = "회원가입",
            description = "초대코드를 이용해 새 계정을 생성합니다."
    )
    public ResponseEntity<ApiResponse<Void>> register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request);
        return ResponseEntity.ok(ApiResponse.success());
    }

    @PostMapping("/login")
    @SecurityRequirements  // 인증 불필요 표시
    @Operation(
            summary = "로그인 (토큰 발급)",
            description = """
                    로그인하여 JWT 토큰을 발급받습니다.

                    **사용법:**
                    1. 아래 'Try it out' 클릭
                    2. email/password 입력 후 Execute
                    3. 응답의 `accessToken` 복사
                    4. 페이지 상단 'Authorize' 버튼 클릭
                    5. 토큰 붙여넣기 후 Authorize
                    """
    )
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
