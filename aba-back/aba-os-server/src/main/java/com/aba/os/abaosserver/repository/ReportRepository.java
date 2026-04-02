package com.aba.os.abaosserver.repository;

import com.aba.os.abaosserver.domain.Report;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReportRepository extends JpaRepository<Report, Long> {

    // 아동별 리포트 목록 조회 (최신순)
    @Query("SELECT r FROM Report r " +
            "JOIN FETCH r.child c " +
            "WHERE c.id = :childId " +
            "ORDER BY r.createdAt DESC")
    List<Report> findByChildIdOrderByCreatedAtDesc(@Param("childId") Long childId);

    // 상세 조회 (Child 정보 포함)
    @Query("SELECT r FROM Report r " +
            "JOIN FETCH r.child c " +
            "WHERE r.id = :reportId")
    Optional<Report> findByIdWithChild(@Param("reportId") Long reportId);

    // 아동별 리포트 수 조회
    long countByChildId(Long childId);

    // Dashboard: 센터별 특정 기간 리포트가 있는 아동 ID 목록 조회
    @Query("SELECT DISTINCT r.child.id FROM Report r " +
            "WHERE r.child.center.id = :centerId " +
            "AND r.periodEnd >= :periodStart")
    List<Long> findChildIdsWithReportInPeriod(
            @Param("centerId") Long centerId,
            @Param("periodStart") java.time.LocalDate periodStart);
}
