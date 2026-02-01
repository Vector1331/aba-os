package com.aba.os.abaosserver.controller;

import com.aba.os.abaosserver.common.ApiResponse;
import com.aba.os.abaosserver.dto.session.SessionCreateRequest;
import com.aba.os.abaosserver.dto.session.SessionDetailResponse;
import com.aba.os.abaosserver.dto.session.SessionResponse;
import com.aba.os.abaosserver.service.SessionService;
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
public class SessionController {

    private final SessionService sessionService;

    /**
     * 세션 및 시행 기록 생성
     * POST /api/v1/sessions
     */
    @PostMapping
    public ResponseEntity<ApiResponse<UUID>> createSession(
            @Valid @RequestBody SessionCreateRequest request) {
        UUID sessionId = sessionService.createSession(request);
        return ResponseEntity.ok(ApiResponse.success(sessionId));
    }

    /**
     * 세션 목록 조회 (아동 ID로 필터링)
     * GET /api/v1/sessions?childId=xxx&startDate=xxx&endDate=xxx
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<SessionResponse>>> getSessions(
            @RequestParam UUID childId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        List<SessionResponse> sessions = sessionService.getSessions(childId, startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success(sessions));
    }

    /**
     * 세션 상세 조회
     * GET /api/v1/sessions/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SessionDetailResponse>> getSessionDetail(@PathVariable UUID id) {
        SessionDetailResponse session = sessionService.getSessionDetail(id);
        return ResponseEntity.ok(ApiResponse.success(session));
    }
}
