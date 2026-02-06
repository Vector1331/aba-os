package com.aba.os.abaosserver.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Persistable;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "children")
public class Child implements Persistable<Long> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Transient
    @Builder.Default
    private boolean isNew = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "center_id", nullable = false)
    private Center center; // Center와 연결

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "therapist_id", nullable = false)
    private Therapist therapist;

    @Column(nullable = false, length = 50)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Gender gender; //  성별

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

    @Column(name = "last_session_date")
    private LocalDate lastSessionDate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }

    public void updateLastSessionDate(LocalDate sessionDate) {
        if (this.lastSessionDate == null || sessionDate.isAfter(this.lastSessionDate)) {
            this.lastSessionDate = sessionDate;
        }
    }

    public void update(String name, LocalDate birthDate, Gender gender, String diagnosis,
                       String currentDevLevel, String parentCharacteristics, String requestDetails, String status) {
        if (name != null) this.name = name;
        if (birthDate != null) this.birthDate = birthDate;
        if (gender != null) this.gender = gender;
        if (diagnosis != null) this.diagnosis = diagnosis;
        if (currentDevLevel != null) this.currentDevLevel = currentDevLevel;
        if (parentCharacteristics != null) this.parentCharacteristics = parentCharacteristics;
        if (requestDetails != null) this.requestDetails = requestDetails;
        if (status != null) this.status = status;
    }

    @PostLoad
    @PostPersist
    private void markNotNew() {
        this.isNew = false;
    }

    @Override
    public boolean isNew() {
        return isNew;
    }

    public enum Gender {
        MALE, FEMALE
    }
}