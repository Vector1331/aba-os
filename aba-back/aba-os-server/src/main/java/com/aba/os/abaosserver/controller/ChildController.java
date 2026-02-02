package com.aba.os.abaosserver.controller;

import com.aba.os.abaosserver.common.ApiResponse;
import com.aba.os.abaosserver.dto.child.ChildCreateRequest;
import com.aba.os.abaosserver.dto.child.ChildDetailResponse;
import com.aba.os.abaosserver.dto.child.ChildListResponse;
import com.aba.os.abaosserver.dto.child.ChildUpdateRequest;
import com.aba.os.abaosserver.service.ChildService;
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
@RequestMapping("/api/v1/children")
@RequiredArgsConstructor
@Tag(name = "1. 아동 (Child)", description = "아동 등록/조회/수정/삭제 API")
public class ChildController {

    private final ChildService childService;

    @GetMapping
    @Operation(summary = "아동 목록 조회", description = "담당 아동 목록을 조회합니다. therapistId로 필터링 가능")
    public ResponseEntity<ApiResponse<List<ChildListResponse>>> getChildren(
            @RequestParam(required = false) UUID therapistId) {
        List<ChildListResponse> children = childService.getChildren(therapistId);
        return ResponseEntity.ok(ApiResponse.success(children));
    }

    @GetMapping("/{id}")
    @Operation(summary = "아동 상세 조회", description = "아동의 상세 정보를 조회합니다.")
    public ResponseEntity<ApiResponse<ChildDetailResponse>> getChildDetail(@PathVariable UUID id) {
        ChildDetailResponse child = childService.getChildDetail(id);
        return ResponseEntity.ok(ApiResponse.success(child));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "아동 등록", description = "새 아동을 등록합니다. (Admin 전용)")
    public ResponseEntity<ApiResponse<UUID>> createChild(@Valid @RequestBody ChildCreateRequest request) {
        UUID childId = childService.createChild(request);
        return ResponseEntity.ok(ApiResponse.success(childId));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "아동 정보 수정", description = "아동의 정보를 수정합니다. (Admin 전용)")
    public ResponseEntity<ApiResponse<String>> updateChild(
            @PathVariable UUID id,
            @Valid @RequestBody ChildUpdateRequest request) {
        childService.updateChild(id, request);
        return ResponseEntity.ok(ApiResponse.success("아동 정보가 수정되었습니다."));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "아동 삭제", description = "아동을 삭제합니다. 연관된 목표, 세션 데이터도 함께 삭제됩니다. (Admin 전용)")
    public ResponseEntity<ApiResponse<String>> deleteChild(@PathVariable UUID id) {
        childService.deleteChild(id);
        return ResponseEntity.ok(ApiResponse.success("아동이 삭제되었습니다."));
    }
}
