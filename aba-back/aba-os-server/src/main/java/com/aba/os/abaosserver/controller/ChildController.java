package com.aba.os.abaosserver.controller;

import com.aba.os.abaosserver.common.ApiResponse;
import com.aba.os.abaosserver.dto.child.ChildCreateRequest;
import com.aba.os.abaosserver.dto.child.ChildDetailResponse;
import com.aba.os.abaosserver.dto.child.ChildListResponse;
import com.aba.os.abaosserver.service.ChildService;
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
@Tag(name = "1. 아동 (Child)", description = "아동 등록/조회 API")
public class ChildController {

    private final ChildService childService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<ChildListResponse>>> getChildren(
            @RequestParam(required = false) UUID therapistId) {
        List<ChildListResponse> children = childService.getChildren(therapistId);
        return ResponseEntity.ok(ApiResponse.success(children));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ChildDetailResponse>> getChildDetail(@PathVariable UUID id) {
        ChildDetailResponse child = childService.getChildDetail(id);
        return ResponseEntity.ok(ApiResponse.success(child));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UUID>> createChild(@Valid @RequestBody ChildCreateRequest request) {
        UUID childId = childService.createChild(request);
        return ResponseEntity.ok(ApiResponse.success(childId));
    }
}
