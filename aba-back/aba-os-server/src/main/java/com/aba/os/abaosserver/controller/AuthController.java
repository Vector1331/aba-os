package com.aba.os.abaosserver.controller;

import com.aba.os.abaosserver.common.ApiResponse;
import com.aba.os.abaosserver.dto.auth.LoginRequest;
import com.aba.os.abaosserver.dto.auth.LoginResponse;
import com.aba.os.abaosserver.dto.auth.RegisterRequest;
import com.aba.os.abaosserver.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
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
@Tag(name = "0. 인증 (Auth)")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @SecurityRequirements
    @Operation(
            summary = "회원가입",
            description = """
                    초대코드를 이용해 새 계정을 생성합니다.

                    **역할(role) 옵션:** `ADMIN`, `THERAPIST`, `PARENT`
                    """,
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    content = @Content(examples = {
                            @ExampleObject(name = "Admin 회원가입",
                                    summary = "STEP 1-1: 센터장 계정 생성",
                                    value = """
                                            {
                                              "email": "admin@gmail.com",
                                              "password": "admin1234",
                                              "name": "센터장",
                                              "role": "ADMIN",
                                              "inviteCode": "ABA2026"
                                            }
                                            """),
                            @ExampleObject(name = "Therapist 회원가입",
                                    summary = "STEP 2-1: 치료사 계정 생성",
                                    value = """
                                            {
                                              "email": "therapist@gmail.com",
                                              "password": "therapist1234",
                                              "name": "김치료",
                                              "role": "THERAPIST",
                                              "inviteCode": "ABA2026"
                                            }
                                            """),
                            @ExampleObject(name = "Parent 회원가입",
                                    summary = "부모 계정 생성",
                                    value = """
                                            {
                                              "email": "parent@gmail.com",
                                              "password": "parent1234",
                                              "name": "김부모",
                                              "role": "PARENT",
                                              "inviteCode": "ABA2026"
                                            }
                                            """)
                    })
            )
    )
    public ResponseEntity<ApiResponse<Void>> register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request);
        return ResponseEntity.ok(ApiResponse.success());
    }

    @PostMapping("/login")
    @SecurityRequirements
    @Operation(
            summary = "로그인 (토큰 발급)",
            description = """
                    로그인하여 JWT 토큰을 발급받습니다.

                    **사용법:**
                    1. 아래 'Try it out' 클릭
                    2. email/password 입력 후 Execute
                    3. 응답의 `accessToken` 복사
                    4. 페이지 상단 **Authorize** 🔓 버튼 클릭
                    5. 토큰 붙여넣기 후 Authorize
                    """,
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    content = @Content(examples = {
                            @ExampleObject(name = "Admin 로그인",
                                    summary = "STEP 1-2: Admin 토큰 발급",
                                    value = """
                                            {
                                              "email": "admin@gmail.com",
                                              "password": "admin1234"
                                            }
                                            """),
                            @ExampleObject(name = "Therapist 로그인",
                                    summary = "STEP 2-2: 치료사 토큰 발급",
                                    value = """
                                            {
                                              "email": "therapist@gmail.com",
                                              "password": "therapist1234"
                                            }
                                            """),
                            @ExampleObject(name = "Parent 로그인",
                                    summary = "부모 토큰 발급",
                                    value = """
                                            {
                                              "email": "parent@gmail.com",
                                              "password": "parent1234"
                                            }
                                            """)
                    })
            )
    )
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
