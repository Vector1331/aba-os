package com.aba.os.abaosserver.controller;

import com.aba.os.abaosserver.common.ApiResponse;
import com.aba.os.abaosserver.dto.therapist.TherapistCreateRequest;
import com.aba.os.abaosserver.dto.therapist.TherapistListResponse;
import com.aba.os.abaosserver.dto.therapist.TherapistResponse;
import com.aba.os.abaosserver.dto.therapist.TherapistUpdateRequest;
import com.aba.os.abaosserver.service.TherapistService;
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
@RequestMapping("/api/v1/therapists")
@RequiredArgsConstructor
@Tag(name = "1. 치료사 관리 (Therapist)")
public class TherapistController {

    private final TherapistService therapistService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "치료사 목록 조회",
            description = "STEP 2-6: 센터에 소속된 치료사 목록을 조회합니다. (Admin 전용)")
    public ResponseEntity<ApiResponse<List<TherapistListResponse>>> getTherapists() {
        List<TherapistListResponse> therapists = therapistService.getTherapists();
        return ResponseEntity.ok(ApiResponse.success(therapists));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "치료사 상세 조회",
            description = "치료사의 상세 정보를 조회합니다. (Admin 전용)")
    public ResponseEntity<ApiResponse<TherapistResponse>> getTherapistDetail(@PathVariable Long id) {
        TherapistResponse therapist = therapistService.getTherapistDetail(id);
        return ResponseEntity.ok(ApiResponse.success(therapist));
    }

    @GetMapping("/by-user/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "사용자 ID로 치료사 조회",
            description = """
                    STEP 2-4: User ID로 치료사 정보를 조회하여 Therapist ID를 획득합니다. (Admin 전용)

                    **사용법:**
                    1. 치료사로 회원가입 (Auth → 회원가입, role: THERAPIST)
                    2. 치료사로 로그인 → 토큰 발급
                    3. User → 내 정보 조회 → User ID 확인
                    4. 이 API에 User ID 입력 → Therapist ID 획득
                    """)
    public ResponseEntity<ApiResponse<TherapistResponse>> getTherapistByUserId(@PathVariable Long userId) {
        TherapistResponse therapist = therapistService.getTherapistByUserId(userId);
        return ResponseEntity.ok(ApiResponse.success(therapist));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "치료사 등록",
            description = "새 치료사를 등록합니다. 먼저 사용자(User)가 생성되어 있어야 합니다. (Admin 전용)")
    public ResponseEntity<ApiResponse<Long>> createTherapist(@Valid @RequestBody TherapistCreateRequest request) {
        Long therapistId = therapistService.createTherapist(request);
        return ResponseEntity.ok(ApiResponse.success(therapistId));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "치료사 정보 수정",
            description = "STEP 2-5: 치료사의 전문분야, 경력 등을 수정합니다. (Admin 전용)",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    content = @Content(examples = {
                            @ExampleObject(name = "전문분야/경력 설정",
                                    summary = "치료사 전문분야와 경력 수정",
                                    value = """
                                            {
                                              "specialty": "ABA 치료",
                                              "experienceYears": 3
                                            }
                                            """),
                            @ExampleObject(name = "전문분야 변경",
                                    summary = "전문분야와 경력 업데이트",
                                    value = """
                                            {
                                              "specialty": "ABA 및 언어치료",
                                              "experienceYears": 5
                                            }
                                            """)
                    })
            )
    )
    public ResponseEntity<ApiResponse<String>> updateTherapist(
            @PathVariable Long id,
            @Valid @RequestBody TherapistUpdateRequest request) {
        therapistService.updateTherapist(id, request);
        return ResponseEntity.ok(ApiResponse.success("치료사 정보가 수정되었습니다."));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "치료사 삭제 (Soft Delete)",
            description = "치료사를 비활성화합니다. 데이터는 보존됩니다. (Admin 전용)")
    public ResponseEntity<ApiResponse<String>> deleteTherapist(@PathVariable Long id) {
        therapistService.deleteTherapist(id);
        return ResponseEntity.ok(ApiResponse.success("치료사가 삭제되었습니다."));
    }
}
