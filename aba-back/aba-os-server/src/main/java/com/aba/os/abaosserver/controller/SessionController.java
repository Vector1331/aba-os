package com.aba.os.abaosserver.controller;

import com.aba.os.abaosserver.common.ApiResponse;
import com.aba.os.abaosserver.dto.session.SessionCreateRequest;
import com.aba.os.abaosserver.dto.session.SessionDetailResponse;
import com.aba.os.abaosserver.dto.session.SessionResponse;
import com.aba.os.abaosserver.service.SessionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/sessions")
@RequiredArgsConstructor
@Tag(name = "3. 세션 (Session)", description = "치료 세션 기록/삭제 API")
public class SessionController {

    private final SessionService sessionService;

    @PostMapping
    @Operation(summary = "세션 생성", description = "새로운 치료 세션과 시행 기록을 생성합니다.")
    public ResponseEntity<ApiResponse<UUID>> createSession(
            @Valid @RequestBody SessionCreateRequest request) {
        UUID sessionId = sessionService.createSession(request);
        return ResponseEntity.ok(ApiResponse.success(sessionId));
    }

    @GetMapping
    @Operation(summary = "세션 목록 조회", description = "아동별 세션 목록을 조회합니다. 날짜 범위로 필터링 가능")
    public ResponseEntity<ApiResponse<List<SessionResponse>>> getSessions(
            @RequestParam UUID childId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        List<SessionResponse> sessions = sessionService.getSessions(childId, startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success(sessions));
    }

    @GetMapping("/{id}")
    @Operation(summary = "세션 상세 조회", description = "세션의 상세 정보와 시행 기록을 조회합니다.")
    public ResponseEntity<ApiResponse<SessionDetailResponse>> getSessionDetail(@PathVariable UUID id) {
        SessionDetailResponse session = sessionService.getSessionDetail(id);
        return ResponseEntity.ok(ApiResponse.success(session));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "세션 삭제", description = "세션과 연관된 시행 기록을 함께 삭제합니다.")
    public ResponseEntity<ApiResponse<String>> deleteSession(@PathVariable UUID id) {
        sessionService.deleteSession(id);
        return ResponseEntity.ok(ApiResponse.success("세션이 삭제되었습니다."));
    }
}
