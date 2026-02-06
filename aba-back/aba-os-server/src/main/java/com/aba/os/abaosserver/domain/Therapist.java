package com.aba.os.abaosserver.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Persistable;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "therapists")
public class Therapist implements Persistable<Long> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Transient
    @Builder.Default
    private boolean isNew = true;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "center_id", nullable = false)
    private Center center;

    @Column(length = 100)
    private String specialty;

    @Column(name = "experience_years")
    private Integer experienceYears; // 경력

    @Column(name = "deleted")
    @Builder.Default
    private boolean deleted = false; // Soft Delete

    @PostLoad
    @PostPersist
    private void markNotNew() {
        this.isNew = false;
    }

    @Override
    public boolean isNew() {
        return isNew;
    }

    public void update(String specialty, Integer experienceYears) {
        if (specialty != null) {
            this.specialty = specialty;
        }
        if (experienceYears != null) {
            this.experienceYears = experienceYears;
        }
    }

    public void softDelete() {
        this.deleted = true;
    }

    public enum TherapistStatus {
        ACTIVE, INACTIVE
    }
}