package com.aba.os.abaosserver.repository;

import com.aba.os.abaosserver.domain.Therapist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TherapistRepository extends JpaRepository<Therapist, UUID> {

    Optional<Therapist> findByUserId(UUID userId);

    // Migration: 센터별 치료사 목록 조회
    List<Therapist> findByCenter_Id(UUID centerId);

    // Migration: 센터의 첫 번째 치료사 조회 (기본 담당자용)
    Optional<Therapist> findFirstByCenter_Id(UUID centerId);
}
