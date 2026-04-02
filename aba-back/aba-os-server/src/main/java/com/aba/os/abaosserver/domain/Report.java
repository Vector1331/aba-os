package com.aba.os.abaosserver.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "reports")
public class Report {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "child_id", nullable = false)
    private Child child;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(name = "period_start", nullable = false)
    private LocalDate periodStart;

    @Column(name = "period_end", nullable = false)
    private LocalDate periodEnd;

    // 집계 통계
    @Column(name = "total_sessions", nullable = false)
    private Integer totalSessions;

    @Column(name = "total_trials", nullable = false)
    private Integer totalTrials;

    @Column(name = "total_successes", nullable = false)
    private Integer totalSuccesses;

    @Column(name = "average_accuracy", precision = 5, scale = 2)
    private BigDecimal averageAccuracy; // 평균 수행 정확도 (%)

    @Column(columnDefinition = "TEXT")
    private String content; // 자동 생성 문구 / AI 요약

    @Enumerated(EnumType.STRING)
    @Column(name = "report_type", nullable = false, length = 30)
    @Builder.Default
    private ReportType reportType = ReportType.STATISTICS_ONLY;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ReportStatus status = ReportStatus.GENERATED;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }

    public enum ReportStatus {
        GENERATED,  // 생성됨
        REVIEWED,   // 검토됨
        PUBLISHED   // 발행됨
    }

    public enum ReportType {
        STATISTICS_ONLY,    // 통계만 (AI 없음)
        PARENT_SUMMARY,     // 부모용 요약 (AI 생성)
        THERAPIST_NOTE,     // 치료사 기록용
        PROGRESS_REPORT     // 진도 리포트
    }
}
