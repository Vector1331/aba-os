package com.aba.os.abaosserver.controller;

import com.aba.os.abaosserver.common.ApiResponse;
import com.aba.os.abaosserver.dto.migration.DiaMigrationResponse;
import com.aba.os.abaosserver.dto.migration.ImageMigrationResponse;
import com.aba.os.abaosserver.dto.migration.MigrationResponse;
import com.aba.os.abaosserver.service.MigrationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
@Tag(name = "7. 마이그레이션 (Migration)")
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
    @Operation(
            summary = "엑셀 데이터 마이그레이션",
            description = """
                    STEP 9-1: 엑셀 파일(.xlsx)을 업로드하여 아동 및 목표 데이터를 일괄 등록합니다.

                    **엑셀 형식:**
                    - Sheet 1 (Children): Name, BirthDate(yyyy-MM-dd), Gender(M/F), Diagnosis
                    - Sheet 2 (Goals): ChildName, GoalContent, Category
                    """
    )
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<MigrationResponse>> uploadExcel(
            @Parameter(description = "엑셀 파일 (.xlsx)", required = true)
            @RequestParam("file") MultipartFile file) {
        MigrationResponse result = migrationService.uploadExcel(file);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * 이미지 기반 데이터 마이그레이션 (Vision AI)
     * POST /api/v1/migration/image
     *
     * - 수기 기록지 이미지를 업로드하면 Vision AI가 텍스트를 추출
     * - No-Storage Strategy: 이미지는 메모리에서만 처리 후 즉시 삭제
     * - 지원 형식: JPEG, PNG, WebP, GIF (최대 20MB)
     */
    @Operation(
            summary = "이미지 기반 데이터 마이그레이션 (Vision AI)",
            description = """
                    STEP 9-3: 수기 기록지 이미지를 업로드하면 Vision AI가 아동 정보와 치료 목표를 자동으로 추출합니다.
                    (OPENAI_API_KEY 환경변수 필요)

                    **No-Storage Strategy**: 이미지는 서버에 저장되지 않고, 메모리에서 처리 후 즉시 삭제됩니다.

                    **지원 형식**: JPEG, PNG, WebP, GIF (최대 20MB)

                    **추출 정보**:
                    - 아동 이름, 생년월일, 성별, 진단명
                    - 치료 목표 (제목, 카테고리, 상태)
                    """
    )
    @PostMapping(value = "/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ImageMigrationResponse>> uploadImage(
            @Parameter(description = "수기 기록지 이미지 (JPEG, PNG, WebP, GIF)", required = true)
            @RequestParam("file") MultipartFile file) {
        ImageMigrationResponse result = migrationService.migrateFromImage(file);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * DIA 템플릿 엑셀 데이터 마이그레이션
     * POST /api/v1/migration/dia
     *
     * DIA 템플릿 구조:
     * - V3: 아동명 (또는 W3)
     * - V4: 세션 날짜 (또는 W4)
     * - 4행부터 데이터 시작
     * - C열: 과제 내용
     * - H~Q열: 시행 마커 (+/-/p)
     */
    @Operation(
            summary = "DIA 템플릿 엑셀 마이그레이션",
            description = """
                    STEP 9-2: DIA 템플릿 형식의 엑셀 파일(.xlsx)을 업로드하여 세션 및 시행 기록을 생성합니다.

                    **템플릿 구조**:
                    - V3 셀: 아동명 (V3이 "아동명" 라벨이면 W3에서 값 읽기)
                    - V4 셀: 세션 날짜 (V4가 "날짜" 라벨이면 W4에서 값 읽기)
                    - 4행부터: 데이터 행
                    - C열: 과제 내용 (task_content)
                    - H~Q열: 시행 마커
                      - `+`: 성공
                      - `-`: 실패
                      - `p`: 촉구 (prompt)

                    **처리 결과**:
                    - 아동이 없으면 자동 생성
                    - 세션 생성 (기본 50분)
                    - 각 행마다 시행 기록(SessionTrial) 생성
                    - trials: 마커 총 개수, successes: + 개수, promptCount: p 개수
                    """
    )
    @PostMapping(value = "/dia", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<DiaMigrationResponse>> uploadDiaExcel(
            @Parameter(description = "DIA 템플릿 엑셀 파일 (.xlsx)", required = true)
            @RequestParam("file") MultipartFile file) {
        DiaMigrationResponse result = migrationService.uploadDiaExcel(file);
        return ResponseEntity.ok(ApiResponse.success(result));
    }
}
