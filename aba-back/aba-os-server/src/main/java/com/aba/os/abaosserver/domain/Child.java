package com.aba.os.abaosserver.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "child")
public class Child {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "center_id", nullable = false)
    private Center center; // Center와 연결

    // Therapist 엔티티 만들기 전이라 임시로 ID만 저장하도록 주석 처리 또는 UUID 필드로 둡니다.
    // 나중에 @ManyToOne으로 바꿀 예정
    @Column(name = "therapist_id", nullable = false)
    private UUID therapistId;

    @Column(nullable = false, length = 50)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Gender gender; // [신규] 성별

    @Column(nullable = false)
    private LocalDate birthDate;

    @Column(length = 200)
    private String diagnosis;

    @Column(name = "current_dev_level", columnDefinition = "TEXT")
    private String currentDevLevel; // 현재 발달 수준

    @Column(name = "parent_characteristics", columnDefinition = "TEXT")
    private String parentCharacteristics; // 부모 특징

    @Column(name = "request_details", columnDefinition = "TEXT")
    private String requestDetails; // 요청사항

    @Column(length = 20)
    @Builder.Default
    private String status = "active";

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }

    public enum Gender {
        MALE, FEMALE
    }
}