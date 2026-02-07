package com.aba.os.abaosserver.controller;

import com.aba.os.abaosserver.common.ApiResponse;
import com.aba.os.abaosserver.dto.report.ReportCreateRequest;
import com.aba.os.abaosserver.dto.report.ReportListResponse;
import com.aba.os.abaosserver.dto.report.ReportResponse;
import com.aba.os.abaosserver.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
@Tag(name = "5. 보고서 (Report)")
public class ReportController {

    private final ReportService reportService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'THERAPIST')")
    @Operation(
            summary = "AI 리포트 생성",
            description = """
                    STEP 7: 기간 내 세션 데이터를 집계하고 AI 기반 전문가 소견을 생성합니다.

                    **reportType 옵션:**
                    - `PARENT_SUMMARY`: AI가 구조화된 JSON 리포트 생성 (차트 데이터 포함)
                    - `STATISTICS_ONLY`: 통계 텍스트만 생성 (AI 미사용)

                    **PARENT_SUMMARY 응답 구조:**
                    ```json
                    {
                      "summary": "전반적인 발달 상태 요약",
                      "strength_weakness": "강점/보완점 분석",
                      "recommendation": "가정 연계 활동 제안",
                      "chart_data": [{"date": "2026-02-01", "score": 65.0}]
                    }
                    ```

                    **AI 기능:**
                    - OpenAI API 키 설정 시: GPT가 구조화된 JSON 리포트 생성
                    - API 키 미설정 시: 테스트용 더미 데이터 사용 (에러 없이 정상 동작)
                    """,
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    content = @Content(examples = {
                            @ExampleObject(name = "AI 리포트 (PARENT_SUMMARY)",
                                    summary = "STEP 7-1: AI 부모 요약 리포트",
                                    value = """
                                            {
                                              "childId": 1,
                                              "title": "2026년 2월 발달 리포트 (AI 버전)",
                                              "periodStart": "2026-02-01",
                                              "periodEnd": "2026-02-28",
                                              "reportType": "PARENT_SUMMARY"
                                            }
                                            """),
                            @ExampleObject(name = "통계 리포트 (STATISTICS_ONLY)",
                                    summary = "STEP 7-2: 통계 전용 리포트",
                                    value = """
                                            {
                                              "childId": 1,
                                              "title": "2026년 2월 통계 리포트",
                                              "periodStart": "2026-02-01",
                                              "periodEnd": "2026-02-28",
                                              "reportType": "STATISTICS_ONLY"
                                            }
                                            """)
                    })
            )
    )
    public ResponseEntity<ApiResponse<ReportResponse>> createReport(
            @Valid @RequestBody ReportCreateRequest request) {
        ReportResponse report = reportService.createReport(request);
        return ResponseEntity.ok(ApiResponse.success(report));
    }

    @GetMapping
    @Operation(summary = "리포트 목록 조회",
            description = "STEP 7-3: 아동별 리포트 목록을 조회합니다. childId 파라미터 필수")
    public ResponseEntity<ApiResponse<List<ReportListResponse>>> getReports(
            @RequestParam Long childId) {
        List<ReportListResponse> reports = reportService.getReports(childId);
        return ResponseEntity.ok(ApiResponse.success(reports));
    }

    @GetMapping("/{id}")
    @Operation(summary = "리포트 상세 조회",
            description = "STEP 7-4: 리포트의 상세 정보와 통계를 조회합니다.")
    public ResponseEntity<ApiResponse<ReportResponse>> getReportDetail(@PathVariable Long id) {
        ReportResponse report = reportService.getReportDetail(id);
        return ResponseEntity.ok(ApiResponse.success(report));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'THERAPIST')")
    @Operation(summary = "리포트 삭제",
            description = "리포트를 삭제합니다. 같은 센터 소속의 Admin/Therapist만 삭제 가능합니다.")
    public ResponseEntity<ApiResponse<String>> deleteReport(@PathVariable Long id) {
        reportService.deleteReport(id);
        return ResponseEntity.ok(ApiResponse.success("리포트가 삭제되었습니다."));
    }
}
