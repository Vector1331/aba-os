package com.aba.os.abaosserver.repository;

import com.aba.os.abaosserver.domain.Session;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SessionRepository extends JpaRepository<Session, UUID> {

    List<Session> findByChildId(UUID childId);

    long countByChildId(UUID childId);

    // N+1 방지를 위한 fetch join 쿼리 (목록 조회용)
    @Query("SELECT DISTINCT s FROM Session s " +
            "JOIN FETCH s.child c " +
            "JOIN FETCH s.therapist t " +
            "JOIN FETCH t.user " +
            "LEFT JOIN FETCH s.trials " +
            "WHERE c.id = :childId " +
            "ORDER BY s.sessionDate DESC")
    List<Session> findByChildIdWithDetails(@Param("childId") UUID childId);

    // 기간 필터링 포함 목록 조회
    List<Session> findAllByChildIdAndSessionDateBetween(
            UUID childId,
            LocalDate startDate,
            LocalDate endDate);

    // 상세 조회용 (시행 기록의 목표 정보까지 fetch)
    @Query("SELECT s FROM Session s " +
            "JOIN FETCH s.child c " +
            "JOIN FETCH s.therapist t " +
            "JOIN FETCH t.user " +
            "LEFT JOIN FETCH s.trials tr " +
            "LEFT JOIN FETCH tr.goal " +
            "WHERE s.id = :sessionId")
    Optional<Session> findByIdWithDetails(@Param("sessionId") UUID sessionId);

    // Dashboard: 특정 날짜의 센터별 세션 수 카운트
    long countByChild_Center_IdAndSessionDate(UUID centerId, LocalDate sessionDate);

    // Dashboard: 기간별 센터 세션 수 카운트
    long countByChild_Center_IdAndSessionDateBetween(UUID centerId, LocalDate startDate, LocalDate endDate);

    // Dashboard: 미래 예정 세션 조회 (최대 5개, 날짜 오름차순)
    List<Session> findTop5ByChild_Center_IdAndSessionDateGreaterThanEqualOrderBySessionDateAsc(
            UUID centerId, LocalDate fromDate);
}
