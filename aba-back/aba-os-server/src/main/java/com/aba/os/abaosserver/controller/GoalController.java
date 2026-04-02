package com.aba.os.abaosserver.controller;

import com.aba.os.abaosserver.common.ApiResponse;
import com.aba.os.abaosserver.domain.Goal.GoalStatus;
import com.aba.os.abaosserver.dto.goal.GoalCreateRequest;
import com.aba.os.abaosserver.dto.goal.GoalResponse;
import com.aba.os.abaosserver.dto.goal.GoalUpdateRequest;
import com.aba.os.abaosserver.service.GoalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Tag(name = "3. 목표 관리 (Goal)")
public class GoalController {

    private final GoalService goalService;

    @GetMapping("/api/v1/children/{childId}/goals")
    @Operation(summary = "아동별 목표 목록 조회",
            description = "STEP 5-2: 특정 아동의 치료 목표 목록을 조회합니다. status 파라미터로 필터링 가능")
    public ResponseEntity<ApiResponse<List<GoalResponse>>> getGoals(
            @PathVariable Long childId,
            @RequestParam(required = false) GoalStatus status) {
        List<GoalResponse> goals = goalService.getGoals(childId, status);
        return ResponseEntity.ok(ApiResponse.success(goals));
    }

    @PostMapping("/api/v1/children/{childId}/goals")
    @Operation(summary = "목표 생성",
            description = """
                    STEP 5-1: 아동에게 새로운 치료 목표를 추가합니다.

                    **category 옵션:** `SOCIAL`, `COMMUNICATION`, `BEHAVIOR`, `ACADEMIC`, `SELF_CARE`, `OTHER`
                    """,
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    content = @Content(examples = {
                            @ExampleObject(name = "사회성 목표",
                                    summary = "STEP 5-1: 눈 맞춤 목표 생성",
                                    value = """
                                            {
                                              "name": "이름 부르면 눈 맞추기",
                                              "category": "SOCIAL",
                                              "description": "이름을 부르면 3초 이내에 눈을 맞추는 행동 형성",
                                              "targetSuccessRate": 80,
                                              "consecutiveDays": 3,
                                              "promptPlan": "신체적 촉구 -> 제스처 촉구 -> 언어적 촉구 -> 독립 수행"
                                            }
                                            """),
                            @ExampleObject(name = "의사소통 목표",
                                    summary = "추가 목표 생성",
                                    value = """
                                            {
                                              "name": "간단한 지시 따르기",
                                              "category": "COMMUNICATION",
                                              "description": "앉아, 일어서 등 간단한 지시에 반응하기",
                                              "targetSuccessRate": 70,
                                              "consecutiveDays": 5
                                            }
                                            """)
                    })
            )
    )
    public ResponseEntity<ApiResponse<Long>> createGoal(
            @PathVariable Long childId,
            @Valid @RequestBody GoalCreateRequest request) {
        Long goalId = goalService.createGoal(childId, request);
        return ResponseEntity.ok(ApiResponse.success(goalId));
    }

    @GetMapping("/api/v1/goals/{id}")
    @Operation(summary = "목표 상세 조회",
            description = "STEP 5-3: 특정 목표의 상세 정보를 조회합니다.")
    public ResponseEntity<ApiResponse<GoalResponse>> getGoalDetail(@PathVariable Long id) {
        GoalResponse goal = goalService.getGoalDetail(id);
        return ResponseEntity.ok(ApiResponse.success(goal));
    }

    @PutMapping("/api/v1/goals/{id}")
    @Operation(summary = "목표 수정",
            description = """
                    목표의 정보(이름, 설명, 상태 등)를 수정합니다.

                    **status 옵션:** `NOT_STARTED`, `IN_PROGRESS`, `MASTERED`, `ON_HOLD`
                    """,
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    content = @Content(examples = @ExampleObject(
                            name = "목표 수정",
                            summary = "상태/목표 성공률 변경",
                            value = """
                                    {
                                      "name": "이름 부르면 눈 맞추기 (수정됨)",
                                      "status": "IN_PROGRESS",
                                      "targetSuccessRate": 85
                                    }
                                    """
                    ))
            )
    )
    public ResponseEntity<ApiResponse<String>> updateGoal(
            @PathVariable Long id,
            @Valid @RequestBody GoalUpdateRequest request) {
        goalService.updateGoal(id, request);
        return ResponseEntity.ok(ApiResponse.success("목표가 수정되었습니다."));
    }

    @DeleteMapping("/api/v1/goals/{id}")
    @Operation(summary = "목표 삭제",
            description = "잘못 생성된 목표를 삭제합니다.")
    public ResponseEntity<ApiResponse<String>> deleteGoal(@PathVariable Long id) {
        goalService.deleteGoal(id);
        return ResponseEntity.ok(ApiResponse.success("목표가 삭제되었습니다."));
    }
}
