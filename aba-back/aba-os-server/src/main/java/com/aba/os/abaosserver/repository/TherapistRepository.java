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

    // 센터별 삭제되지 않은 치료사 목록 조회
    List<Therapist> findByCenter_IdAndDeletedFalse(UUID centerId);

    // 삭제되지 않은 치료사 단건 조회
    Optional<Therapist> findByIdAndDeletedFalse(UUID id);

    // 특정 User가 이미 치료사로 등록되어 있는지 확인
    boolean existsByUserIdAndDeletedFalse(UUID userId);
}
