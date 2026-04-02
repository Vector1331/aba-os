package com.aba.os.abaosserver.controller;

import com.aba.os.abaosserver.common.ApiResponse;
import com.aba.os.abaosserver.dto.dashboard.DashboardSummaryResponse;
import com.aba.os.abaosserver.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
@Tag(name = "6. 대시보드 (Dashboard)")
public class DashboardController {

    private final DashboardService dashboardService;

    /**
     * 대시보드 요약 정보 조회
     * GET /api/v1/dashboard/summary
     */
    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole('THERAPIST', 'ADMIN')")
    @Operation(
            summary = "대시보드 요약 정보 조회",
            description = """
                    STEP 8: 메인 화면용 통계 데이터를 조회합니다. Admin/Therapist 토큰으로 각각 테스트하세요.

                    **반환 데이터:**
                    - `activeCasesCount`: 활성 케이스(아동) 수
                    - `weeklySessionsCount`: 이번 주(월~일) 세션 수
                    - `developmentAgeStats`: 발달 연령 현황 (연령대별 아동 수)
                    - `pendingReportsCount`: 미작성 리포트 수 (최근 30일 내 리포트 없는 아동)
                    - `recentSessions`: 최근 세션 Top 5 (아동명, 치료사, 날짜, 성공률)

                    **권한:** THERAPIST, ADMIN
                    """
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "성공 응답 예시",
                                    value = """
                                            {
                                              "success": true,
                                              "data": {
                                                "activeCasesCount": 15,
                                                "weeklySessionsCount": 42,
                                                "developmentAgeStats": {
                                                  "0-3세": 3,
                                                  "4-6세": 8,
                                                  "7-9세": 3,
                                                  "10세 이상": 1
                                                },
                                                "pendingReportsCount": 5,
                                                "recentSessions": [
                                                  {
                                                    "sessionId": "uuid-1",
                                                    "childId": "uuid-2",
                                                    "childName": "김철수",
                                                    "therapistName": "김치료",
                                                    "sessionDate": "2026-02-04",
                                                    "successRate": 75.0,
                                                    "totalTrials": 20,
                                                    "totalSuccesses": 15
                                                  }
                                                ]
                                              }
                                            }
                                            """
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증 실패 - 로그인 필요"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "권한 없음 - THERAPIST 또는 ADMIN 권한 필요"
            )
    })
    public ResponseEntity<ApiResponse<DashboardSummaryResponse>> getDashboardSummary() {
        DashboardSummaryResponse summary = dashboardService.getDashboardSummary();
        return ResponseEntity.ok(ApiResponse.success(summary));
    }
}
