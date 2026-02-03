package com.aba.os.abaosserver.controller;

import com.aba.os.abaosserver.common.ApiResponse;
import com.aba.os.abaosserver.dto.therapist.TherapistCreateRequest;
import com.aba.os.abaosserver.dto.therapist.TherapistListResponse;
import com.aba.os.abaosserver.dto.therapist.TherapistResponse;
import com.aba.os.abaosserver.dto.therapist.TherapistUpdateRequest;
import com.aba.os.abaosserver.service.TherapistService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/therapists")
@RequiredArgsConstructor
@Tag(name = "1. 치료사 관리 (Therapist)", description = "치료사 등록/조회/수정/삭제 API (Admin 전용)")
public class TherapistController {

    private final TherapistService therapistService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "치료사 목록 조회", description = "센터에 소속된 치료사 목록을 조회합니다. (Admin 전용)")
    public ResponseEntity<ApiResponse<List<TherapistListResponse>>> getTherapists() {
        List<TherapistListResponse> therapists = therapistService.getTherapists();
        return ResponseEntity.ok(ApiResponse.success(therapists));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "치료사 상세 조회", description = "치료사의 상세 정보를 조회합니다. (Admin 전용)")
    public ResponseEntity<ApiResponse<TherapistResponse>> getTherapistDetail(@PathVariable UUID id) {
        TherapistResponse therapist = therapistService.getTherapistDetail(id);
        return ResponseEntity.ok(ApiResponse.success(therapist));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "치료사 등록", description = "새 치료사를 등록합니다. 먼저 사용자(User)가 생성되어 있어야 합니다. (Admin 전용)")
    public ResponseEntity<ApiResponse<UUID>> createTherapist(@Valid @RequestBody TherapistCreateRequest request) {
        UUID therapistId = therapistService.createTherapist(request);
        return ResponseEntity.ok(ApiResponse.success(therapistId));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "치료사 정보 수정", description = "치료사의 전문분야, 경력 등을 수정합니다. (Admin 전용)")
    public ResponseEntity<ApiResponse<String>> updateTherapist(
            @PathVariable UUID id,
            @Valid @RequestBody TherapistUpdateRequest request) {
        therapistService.updateTherapist(id, request);
        return ResponseEntity.ok(ApiResponse.success("치료사 정보가 수정되었습니다."));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "치료사 삭제", description = "치료사를 삭제합니다. (Soft Delete - 데이터는 보존됨) (Admin 전용)")
    public ResponseEntity<ApiResponse<String>> deleteTherapist(@PathVariable UUID id) {
        therapistService.deleteTherapist(id);
        return ResponseEntity.ok(ApiResponse.success("치료사가 삭제되었습니다."));
    }
}
