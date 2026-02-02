package com.aba.os.abaosserver.controller;

import com.aba.os.abaosserver.common.ApiResponse;
import com.aba.os.abaosserver.dto.migration.MigrationResponse;
import com.aba.os.abaosserver.service.MigrationService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/migration")
@RequiredArgsConstructor
@Tag(name = "6. 마이그레이션 (Migration)", description = "엑셀 데이터 마이그레이션 API (Admin 전용)")
public class MigrationController {

    private final MigrationService migrationService;

    /**
     * 엑셀 데이터 업로드 및 마이그레이션
     * POST /api/v1/migration/upload
     *
     * Excel Format:
     * - Sheet 1 (Children): Name, BirthDate(yyyy-MM-dd), Gender(M/F), Diagnosis
     * - Sheet 2 (Goals): ChildName, GoalContent, Category
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<MigrationResponse>> uploadExcel(
            @RequestParam("file") MultipartFile file) {
        MigrationResponse result = migrationService.uploadExcel(file);
        return ResponseEntity.ok(ApiResponse.success(result));
    }
}
