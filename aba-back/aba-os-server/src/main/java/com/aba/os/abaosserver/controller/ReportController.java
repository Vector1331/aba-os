package com.aba.os.abaosserver.controller;

import com.aba.os.abaosserver.common.ApiResponse;
import com.aba.os.abaosserver.dto.report.ReportCreateRequest;
import com.aba.os.abaosserver.dto.report.ReportListResponse;
import com.aba.os.abaosserver.dto.report.ReportResponse;
import com.aba.os.abaosserver.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
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
@Tag(name = "5. 보고서 (Report)", description = "AI 기반 발달 리포트 생성 API")
public class ReportController {

    private final ReportService reportService;

    /**
     * 리포트 생성 (Admin/Therapist만 가능)
     * POST /api/v1/reports
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'THERAPIST')")
    @Operation(
            summary = "AI 리포트 생성",
            description = """
                    기간 내 세션 데이터를 집계하고 AI 기반 전문가 소견을 생성합니다.

                    **AI 기능:**
                    - OpenAI API 키 설정 시: GPT가 부모님께 드릴 치료 총평 작성
                    - API 키 미설정 시: 테스트용 더미 텍스트 사용 (에러 없이 정상 동작)
                    """
    )
    public ResponseEntity<ApiResponse<ReportResponse>> createReport(
            @Valid @RequestBody ReportCreateRequest request) {
        ReportResponse report = reportService.createReport(request);
        return ResponseEntity.ok(ApiResponse.success(report));
    }

    /**
     * 아동별 리포트 목록 조회
     * GET /api/v1/reports?childId={id}
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<ReportListResponse>>> getReports(
            @RequestParam Long childId) {
        List<ReportListResponse> reports = reportService.getReports(childId);
        return ResponseEntity.ok(ApiResponse.success(reports));
    }

    /**
     * 리포트 상세 조회
     * GET /api/v1/reports/{id}
     */
    @GetMapping("/{id}")
    @Operation(summary = "리포트 상세 조회", description = "리포트의 상세 정보와 통계를 조회합니다.")
    public ResponseEntity<ApiResponse<ReportResponse>> getReportDetail(@PathVariable Long id) {
        ReportResponse report = reportService.getReportDetail(id);
        return ResponseEntity.ok(ApiResponse.success(report));
    }

    /**
     * 리포트 삭제
     * DELETE /api/v1/reports/{id}
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'THERAPIST')")
    @Operation(summary = "리포트 삭제", description = "리포트를 삭제합니다. 같은 센터 소속의 Admin/Therapist만 삭제 가능합니다.")
    public ResponseEntity<ApiResponse<String>> deleteReport(@PathVariable Long id) {
        reportService.deleteReport(id);
        return ResponseEntity.ok(ApiResponse.success("리포트가 삭제되었습니다."));
    }
}
