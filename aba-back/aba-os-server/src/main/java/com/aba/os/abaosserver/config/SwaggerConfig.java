package com.aba.os.abaosserver.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.tags.Tag;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SwaggerConfig {

    private static final String SECURITY_SCHEME_NAME = "bearerAuth";

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(apiInfo())
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME, securityScheme()))
                .tags(orderedTags());
    }

    private List<Tag> orderedTags() {
        return List.of(
                new Tag().name("0. 인증 (Auth)")
                        .description("로그인/회원가입 API - **가장 먼저 실행하세요!**\n\n"
                                + "### 테스트 순서\n"
                                + "1. `회원가입` - Admin 계정 생성 (role: ADMIN, inviteCode 필요)\n"
                                + "2. `로그인` - accessToken 발급\n"
                                + "3. 페이지 상단 **Authorize** 버튼 → 토큰 입력\n\n"
                                + "### 테스트 계정\n"
                                + "| 역할 | 이메일 | 비밀번호 |\n"
                                + "|------|--------|----------|\n"
                                + "| Admin | admin@gmail.com | admin1234 |\n"
                                + "| Therapist | therapist@gmail.com | therapist1234 |\n"
                                + "| Invite Code | - | ABA2026 |"),
                new Tag().name("1. 치료사 관리 (Therapist)")
                        .description("치료사 등록/조회/수정/삭제 API (Admin 전용)\n\n"
                                + "### 테스트 순서\n"
                                + "1. THERAPIST 역할로 `회원가입` (Auth → 회원가입)\n"
                                + "2. 치료사 계정으로 `로그인` → 토큰 저장\n"
                                + "3. `내 정보 조회` (User → GET /me) → User ID 확인\n"
                                + "4. `사용자 ID로 치료사 조회` → Therapist ID 획득\n"
                                + "5. `치료사 정보 수정` (전문분야/경력 설정)\n"
                                + "6. `치료사 목록 조회`로 확인"),
                new Tag().name("2. 아동 관리 (Child)")
                        .description("아동 등록/조회/수정/삭제 API\n\n"
                                + "### 테스트 순서\n"
                                + "1. Admin 토큰으로 `아동 등록` (therapistId 필요)\n"
                                + "2. 치료사 토큰으로 `아동 목록 조회`\n"
                                + "3. `아동 상세 조회` → child ID 확인\n"
                                + "4. Admin 토큰으로 `아동 정보 수정`"),
                new Tag().name("3. 목표 관리 (Goal)")
                        .description("치료 목표 등록/조회/수정/삭제 API\n\n"
                                + "### 테스트 순서\n"
                                + "1. `목표 생성` (childId 경로 변수 필요)\n"
                                + "2. `아동별 목표 목록 조회`\n"
                                + "3. `목표 상세 조회` → goal ID로 조회\n"
                                + "4. `목표 수정` (이름/상태/목표 성공률 변경)"),
                new Tag().name("4. 세션 및 데이터 (Session)")
                        .description("치료 세션 기록/조회/삭제 API\n\n"
                                + "### 테스트 순서\n"
                                + "1. `세션 생성` (childId, therapistId, trials[] 포함)\n"
                                + "2. `세션 목록 조회` (childId 필수, 날짜 필터 선택)\n"
                                + "3. `세션 상세 조회` → 시행(Trial) 기록 확인"),
                new Tag().name("5. 보고서 (Report)")
                        .description("AI 기반 발달 리포트 생성 API\n\n"
                                + "### 테스트 순서\n"
                                + "1. `AI 리포트 생성` (reportType: PARENT_SUMMARY)\n"
                                + "2. `AI 리포트 생성` (reportType: STATISTICS_ONLY)\n"
                                + "3. `리포트 목록 조회`\n"
                                + "4. `리포트 상세 조회`\n\n"
                                + "### reportType 옵션\n"
                                + "- **PARENT_SUMMARY**: AI가 구조화된 JSON 리포트 생성\n"
                                + "- **STATISTICS_ONLY**: 통계 텍스트만 생성 (AI 미사용)"),
                new Tag().name("6. 대시보드 (Dashboard)")
                        .description("센터 현황 요약 API - 메인 화면용 통계 데이터\n\n"
                                + "### 테스트\n"
                                + "- Admin 토큰으로 `대시보드 요약 조회`\n"
                                + "- Therapist 토큰으로 `대시보드 요약 조회`\n"
                                + "- 반환: 활성 아동 수, 주간 세션 수, 발달 연령 통계, 최근 세션"),
                new Tag().name("7. 마이그레이션 (Migration)")
                        .description("엑셀/이미지 데이터 마이그레이션 API (Admin 전용)\n\n"
                                + "### 테스트 순서\n"
                                + "1. `엑셀 마이그레이션` - Children + Goals 시트 업로드\n"
                                + "2. `DIA 템플릿 마이그레이션` - DIA 형식 세션 기록 업로드\n"
                                + "3. `이미지 마이그레이션` - 수기 기록지 Vision AI 처리"),
                new Tag().name("8. 사용자 (User)")
                        .description("사용자 정보 조회/수정 API\n\n"
                                + "### 테스트\n"
                                + "- `내 정보 조회` (GET /me)\n"
                                + "- `내 정보 수정` (PUT /me)")
        );
    }

    private Info apiInfo() {
        return new Info()
                .title("ABA-OS API Server")
                .version("v1.2")
                .description("""
                        # 발달장애 아동 치료 센터 관리 시스템 API

                        ## 🚀 빠른 시작 (테스트 순서)
                        아래 순서대로 API를 실행하면 전체 서비스 플로우를 테스트할 수 있습니다.

                        | 순서 | 단계 | 설명 |
                        |------|------|------|
                        | 1 | **인증 (Auth)** | Admin 회원가입 → 로그인 → Authorize |
                        | 2 | **치료사 (Therapist)** | 치료사 회원가입 → 로그인 → User ID 조회 → Therapist ID 획득 |
                        | 3 | **아동 (Child)** | Admin 토큰으로 아동 등록 (therapistId 필요) |
                        | 4 | **목표 (Goal)** | 치료사 토큰으로 목표 생성 |
                        | 5 | **세션 (Session)** | 세션 + 시행 기록 생성 |
                        | 6 | **보고서 (Report)** | AI 리포트 / 통계 리포트 생성 |
                        | 7 | **대시보드 (Dashboard)** | 센터 현황 요약 조회 |
                        | 8 | **마이그레이션 (Migration)** | 엑셀/이미지 데이터 일괄 등록 |

                        ## 🔑 인증 방법
                        1. **Auth → 로그인** 엔드포인트로 로그인하여 `accessToken`을 발급받습니다.
                        2. 우측 상단의 **Authorize** 🔓 버튼을 클릭합니다.
                        3. 발급받은 `accessToken`을 입력합니다 (Bearer 접두사 없이 토큰만).
                        4. 이후 모든 API 요청에 자동으로 JWT 토큰이 포함됩니다.

                        ## 📋 테스트 계정
                        | 역할 | 이메일 | 비밀번호 | 초대코드 |
                        |------|--------|----------|----------|
                        | Admin (센터장) | admin@gmail.com | admin1234 | ABA2026 |
                        | Therapist (치료사) | therapist@gmail.com | therapist1234 | ABA2026 |
                        """)
                .contact(new Contact()
                        .name("ABA-OS Team")
                        .email("support@aba-os.com"))
                .license(new License()
                        .name("Private License")
                        .url("https://aba-os.com/license"));
    }

    private SecurityScheme securityScheme() {
        return new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .in(SecurityScheme.In.HEADER)
                .name("Authorization")
                .description("JWT Access Token을 입력하세요. (Bearer 접두사 없이 토큰만 입력)");
    }
}
