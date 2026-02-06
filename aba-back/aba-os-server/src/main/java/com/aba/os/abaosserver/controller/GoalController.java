package com.aba.os.abaosserver.controller;

import com.aba.os.abaosserver.common.ApiResponse;
import com.aba.os.abaosserver.domain.Goal.GoalStatus;
import com.aba.os.abaosserver.dto.goal.GoalCreateRequest;
import com.aba.os.abaosserver.dto.goal.GoalResponse;
import com.aba.os.abaosserver.dto.goal.GoalUpdateRequest;
import com.aba.os.abaosserver.service.GoalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Tag(name = "3. 목표 관리 (Goal)", description = "치료 목표 등록/조회/수정/삭제 API")
public class GoalController {

    private final GoalService goalService;

    @GetMapping("/api/v1/children/{childId}/goals")
    @Operation(summary = "아동별 목표 목록 조회", description = "특정 아동의 치료 목표 목록을 조회합니다.")
    public ResponseEntity<ApiResponse<List<GoalResponse>>> getGoals(
            @PathVariable Long childId,
            @RequestParam(required = false) GoalStatus status) {
        List<GoalResponse> goals = goalService.getGoals(childId, status);
        return ResponseEntity.ok(ApiResponse.success(goals));
    }

    @PostMapping("/api/v1/children/{childId}/goals")
    @Operation(summary = "목표 생성", description = "아동에게 새로운 치료 목표를 추가합니다.")
    public ResponseEntity<ApiResponse<Long>> createGoal(
            @PathVariable Long childId,
            @Valid @RequestBody GoalCreateRequest request) {
        Long goalId = goalService.createGoal(childId, request);
        return ResponseEntity.ok(ApiResponse.success(goalId));
    }

    @GetMapping("/api/v1/goals/{id}")
    @Operation(summary = "목표 상세 조회", description = "특정 목표의 상세 정보를 조회합니다.")
    public ResponseEntity<ApiResponse<GoalResponse>> getGoalDetail(@PathVariable Long id) {
        GoalResponse goal = goalService.getGoalDetail(id);
        return ResponseEntity.ok(ApiResponse.success(goal));
    }

    @PutMapping("/api/v1/goals/{id}")
    @Operation(summary = "목표 수정", description = "목표의 정보(이름, 설명, 상태 등)를 수정합니다.")
    public ResponseEntity<ApiResponse<String>> updateGoal(
            @PathVariable Long id,
            @Valid @RequestBody GoalUpdateRequest request) {
        goalService.updateGoal(id, request);
        return ResponseEntity.ok(ApiResponse.success("목표가 수정되었습니다."));
    }

    @DeleteMapping("/api/v1/goals/{id}")
    @Operation(summary = "목표 삭제", description = "잘못 생성된 목표를 삭제합니다.")
    public ResponseEntity<ApiResponse<String>> deleteGoal(@PathVariable Long id) {
        goalService.deleteGoal(id);
        return ResponseEntity.ok(ApiResponse.success("목표가 삭제되었습니다."));
    }
}
