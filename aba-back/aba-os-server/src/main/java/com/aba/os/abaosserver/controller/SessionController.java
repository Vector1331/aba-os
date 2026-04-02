package com.aba.os.abaosserver.controller;

import com.aba.os.abaosserver.common.ApiResponse;
import com.aba.os.abaosserver.dto.session.SessionCreateRequest;
import com.aba.os.abaosserver.dto.session.SessionDetailResponse;
import com.aba.os.abaosserver.dto.session.SessionResponse;
import com.aba.os.abaosserver.service.SessionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/sessions")
@RequiredArgsConstructor
@Tag(name = "4. 세션 및 데이터 (Session)")
public class SessionController {

    private final SessionService sessionService;

    @PostMapping
    @Operation(summary = "세션 생성",
            description = """
                    STEP 6-1: 새로운 치료 세션과 시행(Trial) 기록을 함께 생성합니다.

                    **필수 ID:**
                    - `childId`: 아동 관리 → 아동 목록에서 획득
                    - `therapistId`: 치료사 관리 → 치료사 조회에서 획득
                    - `trials[].goalId`: 목표 관리 → 목표 목록에서 획득
                    """,
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    content = @Content(examples = {
                            @ExampleObject(name = "단일 목표 세션",
                                    summary = "STEP 6-1: 첫 번째 세션 (시행 1개)",
                                    value = """
                                            {
                                              "childId": 1,
                                              "therapistId": 1,
                                              "sessionDate": "2026-02-07",
                                              "duration": 50,
                                              "notes": "첫 번째 세션. 아동 컨디션 양호.",
                                              "trials": [
                                                {
                                                  "goalId": 1,
                                                  "taskContent": "이름 부르고 눈 맞춤 유도",
                                                  "trials": 10,
                                                  "successes": 6,
                                                  "promptCount": 3,
                                                  "memo": "언어적 촉구에 반응 좋음"
                                                }
                                              ]
                                            }
                                            """),
                            @ExampleObject(name = "추가 세션 (리포트용)",
                                    summary = "STEP 6-2: 두 번째 세션",
                                    value = """
                                            {
                                              "childId": 1,
                                              "therapistId": 1,
                                              "sessionDate": "2026-02-05",
                                              "duration": 50,
                                              "notes": "두 번째 세션. 눈 맞춤 개선 보임.",
                                              "trials": [
                                                {
                                                  "goalId": 1,
                                                  "taskContent": "이름 부르고 눈 맞춤 유도",
                                                  "trials": 12,
                                                  "successes": 9,
                                                  "promptCount": 2,
                                                  "memo": "제스처 촉구로 전환 시도"
                                                }
                                              ]
                                            }
                                            """),
                            @ExampleObject(name = "다중 목표 세션",
                                    summary = "복수 목표 시행 기록",
                                    value = """
                                            {
                                              "childId": 1,
                                              "therapistId": 1,
                                              "sessionDate": "2026-02-06",
                                              "duration": 45,
                                              "notes": "세 번째 세션. 복수 목표 진행.",
                                              "trials": [
                                                {
                                                  "goalId": 1,
                                                  "taskContent": "눈 맞춤 훈련",
                                                  "trials": 15,
                                                  "successes": 11,
                                                  "promptCount": 2,
                                                  "memo": "제스처 촉구 감소"
                                                },
                                                {
                                                  "goalId": 2,
                                                  "taskContent": "앉아 지시 따르기",
                                                  "trials": 10,
                                                  "successes": 7,
                                                  "promptCount": 3,
                                                  "memo": "신체적 촉구 필요"
                                                }
                                              ]
                                            }
                                            """)
                    })
            )
    )
    public ResponseEntity<ApiResponse<Long>> createSession(
            @Valid @RequestBody SessionCreateRequest request) {
        Long sessionId = sessionService.createSession(request);
        return ResponseEntity.ok(ApiResponse.success(sessionId));
    }

    @GetMapping
    @Operation(summary = "세션 목록 조회",
            description = """
                    STEP 6-3: 아동별 세션 목록을 조회합니다.

                    **파라미터:**
                    - `childId` (필수): 아동 ID
                    - `startDate` (선택): 시작 날짜 (yyyy-MM-dd)
                    - `endDate` (선택): 종료 날짜 (yyyy-MM-dd)
                    """)
    public ResponseEntity<ApiResponse<List<SessionResponse>>> getSessions(
            @RequestParam Long childId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        List<SessionResponse> sessions = sessionService.getSessions(childId, startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success(sessions));
    }

    @GetMapping("/{id}")
    @Operation(summary = "세션 상세 조회",
            description = "STEP 6-4: 세션의 상세 정보와 시행(Trial) 기록을 조회합니다.")
    public ResponseEntity<ApiResponse<SessionDetailResponse>> getSessionDetail(@PathVariable Long id) {
        SessionDetailResponse session = sessionService.getSessionDetail(id);
        return ResponseEntity.ok(ApiResponse.success(session));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "세션 삭제",
            description = "세션과 연관된 시행 기록을 함께 삭제합니다.")
    public ResponseEntity<ApiResponse<String>> deleteSession(@PathVariable Long id) {
        sessionService.deleteSession(id);
        return ResponseEntity.ok(ApiResponse.success("세션이 삭제되었습니다."));
    }
}
