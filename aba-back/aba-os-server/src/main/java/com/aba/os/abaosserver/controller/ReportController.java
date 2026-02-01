package com.aba.os.abaosserver.controller;

import com.aba.os.abaosserver.common.ApiResponse;
import com.aba.os.abaosserver.dto.report.ReportCreateRequest;
import com.aba.os.abaosserver.dto.report.ReportListResponse;
import com.aba.os.abaosserver.dto.report.ReportResponse;
import com.aba.os.abaosserver.service.ReportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    /**
     * 리포트 생성 (Admin/Therapist만 가능)
     * POST /api/v1/reports
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'THERAPIST')")
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
            @RequestParam UUID childId) {
        List<ReportListResponse> reports = reportService.getReports(childId);
        return ResponseEntity.ok(ApiResponse.success(reports));
    }

    /**
     * 리포트 상세 조회
     * GET /api/v1/reports/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ReportResponse>> getReportDetail(@PathVariable UUID id) {
        ReportResponse report = reportService.getReportDetail(id);
        return ResponseEntity.ok(ApiResponse.success(report));
    }
}
