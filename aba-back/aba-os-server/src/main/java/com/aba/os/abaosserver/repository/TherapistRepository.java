package com.aba.os.abaosserver.repository;

import com.aba.os.abaosserver.domain.Therapist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TherapistRepository extends JpaRepository<Therapist, Long> {

    Optional<Therapist> findByUserId(Long userId);

    // Migration: 센터별 치료사 목록 조회
    List<Therapist> findByCenter_Id(Long centerId);

    // Migration: 센터의 첫 번째 치료사 조회 (기본 담당자용)
    Optional<Therapist> findFirstByCenter_Id(Long centerId);

    // 센터별 삭제되지 않은 치료사 목록 조회
    List<Therapist> findByCenter_IdAndDeletedFalse(Long centerId);

    // 삭제되지 않은 치료사 단건 조회
    Optional<Therapist> findByIdAndDeletedFalse(Long id);

    // 삭제되지 않은 치료사 단건 조회 (연관 엔티티 fetch join)
    @Query("SELECT t FROM Therapist t JOIN FETCH t.user JOIN FETCH t.center WHERE t.id = :id AND t.deleted = false")
    Optional<Therapist> findByIdWithDetailsAndDeletedFalse(@Param("id") Long id);

    // 특정 User가 이미 치료사로 등록되어 있는지 확인
    boolean existsByUserIdAndDeletedFalse(Long userId);
}
