package com.aba.os.abaosserver.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "session_trials")
public class SessionTrial {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private Session session;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "goal_id", nullable = false)
    private Goal goal;

    @Column(name = "task_content", columnDefinition = "TEXT")
    private String taskContent; // [신규] 과제 내용

    @Column(nullable = false)
    private Integer trials;

    @Column(nullable = false)
    private Integer successes;

    @Column(name = "prompt_count")
    private Integer promptCount; // 촉구 횟수

    @Column(columnDefinition = "TEXT")
    private String memo; // [신규] 상세 메모
}