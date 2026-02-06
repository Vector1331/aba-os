package com.aba.os.abaosserver.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "goals")
public class Goal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "child_id", nullable = false)
    private Child child;

    @Column(nullable = false, length = 200)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private GoalCategory category;

    @Column(columnDefinition = "TEXT")
    private String description; // 상세 설명

    @Column(name = "target_success_rate")
    private Integer targetSuccessRate; // [신규] 목표 성공률

    @Column(name = "consecutive_days")
    private Integer consecutiveDays; // [신규] 연속 달성일

    @Column(name = "prompt_plan", columnDefinition = "TEXT")
    private String promptPlan; // [신규] 촉구 계획

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private GoalStatus status = GoalStatus.ACTIVE;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }

    public void update(String name, GoalCategory category, String description, GoalStatus status,
                       Integer targetSuccessRate, Integer consecutiveDays, String promptPlan) {
        if (name != null) this.name = name;
        if (category != null) this.category = category;
        if (description != null) this.description = description;
        if (status != null) this.status = status;
        if (targetSuccessRate != null) this.targetSuccessRate = targetSuccessRate;
        if (consecutiveDays != null) this.consecutiveDays = consecutiveDays;
        if (promptPlan != null) this.promptPlan = promptPlan;
    }

    public enum GoalCategory {
        COMMUNICATION, SOCIAL, SENSORY, SELF_CARE, COGNITIVE, MOTOR, PLAY, BEHAVIOR
    }

    public enum GoalStatus {
        ACTIVE,         // 진행 중 (= IN_PROGRESS)
        ACHIEVED,       // 달성 완료 (= COMPLETED)
        PAUSED,         // 일시 중단
        WAITING,        // 대기 중 (미시작)
        IN_PROGRESS,    // 진행 중 (ACTIVE와 동일 용도)
        COMPLETED,      // 완료 (ACHIEVED와 동일 용도)
        MAINTENANCE     // 유지
    }
}