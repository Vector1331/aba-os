package com.aba.os.abaosserver.controller;

import com.aba.os.abaosserver.common.ApiResponse;
import com.aba.os.abaosserver.domain.Goal.GoalStatus;
import com.aba.os.abaosserver.dto.goal.GoalCreateRequest;
import com.aba.os.abaosserver.dto.goal.GoalResponse;
import com.aba.os.abaosserver.service.GoalService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/children/{childId}/goals")
@RequiredArgsConstructor
@Tag(name = "2. 목표 (Goal)", description = "치료 목표 등록/조회 API")
public class GoalController {

    private final GoalService goalService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<GoalResponse>>> getGoals(
            @PathVariable UUID childId,
            @RequestParam(required = false) GoalStatus status) {
        List<GoalResponse> goals = goalService.getGoals(childId, status);
        return ResponseEntity.ok(ApiResponse.success(goals));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<UUID>> createGoal(
            @PathVariable UUID childId,
            @Valid @RequestBody GoalCreateRequest request) {
        UUID goalId = goalService.createGoal(childId, request);
        return ResponseEntity.ok(ApiResponse.success(goalId));
    }
}
