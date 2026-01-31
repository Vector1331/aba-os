package com.aba.os.abaosserver.service;

import com.aba.os.abaosserver.domain.Child;
import com.aba.os.abaosserver.domain.Goal;
import com.aba.os.abaosserver.domain.Goal.GoalStatus;
import com.aba.os.abaosserver.dto.goal.GoalCreateRequest;
import com.aba.os.abaosserver.dto.goal.GoalResponse;
import com.aba.os.abaosserver.repository.ChildRepository;
import com.aba.os.abaosserver.repository.GoalRepository;
import com.aba.os.abaosserver.security.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GoalService {

    private final GoalRepository goalRepository;
    private final ChildRepository childRepository;
    private final SecurityUtil securityUtil;

    public List<GoalResponse> getGoals(UUID childId, GoalStatus status) {
        UUID centerId = securityUtil.getCurrentCenterId();

        // 아동 조회 및 권한 검증
        Child child = childRepository.findById(childId)
                .orElseThrow(() -> new IllegalArgumentException("아동을 찾을 수 없습니다."));

        if (!child.getCenter().getId().equals(centerId)) {
            throw new IllegalArgumentException("접근 권한이 없습니다.");
        }

        List<Goal> goals;
        if (status != null) {
            goals = goalRepository.findByChildIdAndStatus(childId, status);
        } else {
            goals = goalRepository.findByChildId(childId);
        }

        return goals.stream()
                .map(GoalResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public UUID createGoal(UUID childId, GoalCreateRequest request) {
        UUID centerId = securityUtil.getCurrentCenterId();

        // 아동 조회 및 권한 검증
        Child child = childRepository.findById(childId)
                .orElseThrow(() -> new IllegalArgumentException("아동을 찾을 수 없습니다."));

        if (!child.getCenter().getId().equals(centerId)) {
            throw new IllegalArgumentException("접근 권한이 없습니다.");
        }

        Goal goal = Goal.builder()
                .child(child)
                .name(request.getName())
                .category(request.getCategory())
                .description(request.getDescription())
                .targetSuccessRate(request.getTargetSuccessRate())
                .consecutiveDays(request.getConsecutiveDays())
                .promptPlan(request.getPromptPlan())
                .status(GoalStatus.ACTIVE)
                .build();

        Goal savedGoal = goalRepository.save(goal);
        return savedGoal.getId();
    }
}
