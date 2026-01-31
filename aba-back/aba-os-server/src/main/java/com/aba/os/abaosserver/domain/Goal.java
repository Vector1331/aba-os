package com.aba.os.abaosserver.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "goals")
public class Goal {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

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

    public enum GoalCategory {
        COMMUNICATION, SOCIAL, SENSORY, SELF_CARE, COGNITIVE, MOTOR, PLAY, BEHAVIOR
    }

    public enum GoalStatus {
        ACTIVE, ACHIEVED, PAUSED
    }
}