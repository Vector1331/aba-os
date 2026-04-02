package com.aba.os.abaosserver.repository;

import com.aba.os.abaosserver.domain.Session;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface SessionRepository extends JpaRepository<Session, Long> {

    List<Session> findByChildId(Long childId);

    long countByChildId(Long childId);

    // N+1 방지를 위한 fetch join 쿼리 (목록 조회용)
    @Query("SELECT DISTINCT s FROM Session s " +
            "JOIN FETCH s.child c " +
            "JOIN FETCH s.therapist t " +
            "JOIN FETCH t.user " +
            "LEFT JOIN FETCH s.trials " +
            "WHERE c.id = :childId " +
            "ORDER BY s.sessionDate DESC")
    List<Session> findByChildIdWithDetails(@Param("childId") Long childId);

    // 기간 필터링 포함 목록 조회
    List<Session> findAllByChildIdAndSessionDateBetween(
            Long childId,
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
    Optional<Session> findByIdWithDetails(@Param("sessionId") Long sessionId);

    // Dashboard: 특정 날짜의 센터별 세션 수 카운트
    long countByChild_Center_IdAndSessionDate(Long centerId, LocalDate sessionDate);

    // Dashboard: 기간별 센터 세션 수 카운트
    long countByChild_Center_IdAndSessionDateBetween(Long centerId, LocalDate startDate, LocalDate endDate);

    // Dashboard: 미래 예정 세션 조회 (최대 5개, 날짜 오름차순)
    List<Session> findTop5ByChild_Center_IdAndSessionDateGreaterThanEqualOrderBySessionDateAsc(
            Long centerId, LocalDate fromDate);

    // Dashboard: 예정 세션 조회 (N+1 방지 Fetch Join)
    @Query("SELECT s FROM Session s " +
            "JOIN FETCH s.child c " +
            "JOIN FETCH s.therapist t " +
            "JOIN FETCH t.user u " +
            "WHERE c.center.id = :centerId " +
            "AND s.sessionDate >= :fromDate " +
            "ORDER BY s.sessionDate ASC " +
            "LIMIT 5")
    List<Session> findUpcomingSessionsWithDetails(
            @Param("centerId") Long centerId,
            @Param("fromDate") LocalDate fromDate);

    // 세션 목록 조회 (기간 필터링 + Fetch Join)
    @Query("SELECT DISTINCT s FROM Session s " +
            "JOIN FETCH s.child c " +
            "JOIN FETCH s.therapist t " +
            "JOIN FETCH t.user u " +
            "WHERE c.id = :childId " +
            "AND s.sessionDate BETWEEN :startDate AND :endDate " +
            "ORDER BY s.sessionDate DESC")
    List<Session> findByChildIdAndDateRangeWithDetails(
            @Param("childId") Long childId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    void deleteAllByChildId(Long childId);

    // Dashboard: 최근 세션 Top 5 조회 (시행 데이터 포함, 날짜 내림차순)
    @Query("SELECT DISTINCT s FROM Session s " +
            "JOIN FETCH s.child c " +
            "JOIN FETCH s.therapist t " +
            "JOIN FETCH t.user u " +
            "LEFT JOIN FETCH s.trials tr " +
            "WHERE c.center.id = :centerId " +
            "ORDER BY s.sessionDate DESC, s.createdAt DESC")
    List<Session> findRecentSessionsWithTrials(@Param("centerId") Long centerId);
}
