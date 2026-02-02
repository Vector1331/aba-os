package com.aba.os.abaosserver.service;

import com.aba.os.abaosserver.domain.Child;
import com.aba.os.abaosserver.domain.Goal;
import com.aba.os.abaosserver.domain.Goal.GoalStatus;
import com.aba.os.abaosserver.dto.goal.GoalCreateRequest;
import com.aba.os.abaosserver.dto.goal.GoalResponse;
import com.aba.os.abaosserver.dto.goal.GoalUpdateRequest;
import com.aba.os.abaosserver.repository.ChildRepository;
import com.aba.os.abaosserver.repository.GoalRepository;
import com.aba.os.abaosserver.security.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
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

    /**
     * 목표 상세 조회
     */
    public GoalResponse getGoalDetail(UUID goalId) {
        UUID centerId = securityUtil.getCurrentCenterId();

        Goal goal = goalRepository.findById(goalId)
                .orElseThrow(() -> new IllegalArgumentException("목표를 찾을 수 없습니다."));

        // 권한 검증: 해당 목표의 아동이 같은 센터 소속인지
        if (!goal.getChild().getCenter().getId().equals(centerId)) {
            throw new IllegalArgumentException("접근 권한이 없습니다.");
        }

        return GoalResponse.from(goal);
    }

    /**
     * 목표 정보 수정
     */
    @Transactional
    public void updateGoal(UUID goalId, GoalUpdateRequest request) {
        UUID centerId = securityUtil.getCurrentCenterId();

        Goal goal = goalRepository.findById(goalId)
                .orElseThrow(() -> new IllegalArgumentException("목표를 찾을 수 없습니다."));

        // 권한 검증
        if (!goal.getChild().getCenter().getId().equals(centerId)) {
            throw new IllegalArgumentException("접근 권한이 없습니다.");
        }

        goal.update(
                request.getName(),
                request.getCategory(),
                request.getDescription(),
                request.getStatus(),
                request.getTargetSuccessRate(),
                request.getConsecutiveDays(),
                request.getPromptPlan()
        );

        log.info("목표 수정 완료 - ID: {}, 이름: {}", goalId, goal.getName());
    }

    /**
     * 목표 삭제
     */
    @Transactional
    public void deleteGoal(UUID goalId) {
        UUID centerId = securityUtil.getCurrentCenterId();

        Goal goal = goalRepository.findById(goalId)
                .orElseThrow(() -> new IllegalArgumentException("목표를 찾을 수 없습니다."));

        // 권한 검증
        if (!goal.getChild().getCenter().getId().equals(centerId)) {
            throw new IllegalArgumentException("접근 권한이 없습니다.");
        }

        goalRepository.delete(goal);
        log.info("목표 삭제 완료 - ID: {}, 이름: {}", goalId, goal.getName());
    }
}
