package com.aba.os.abaosserver.repository;

import com.aba.os.abaosserver.domain.Child;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ChildRepository extends JpaRepository<Child, UUID> {

    List<Child> findByCenterId(UUID centerId);

    List<Child> findByTherapistId(UUID therapistId);

    @Query("SELECT c FROM Child c WHERE c.center.id = :centerId AND (:therapistId IS NULL OR c.therapist.id = :therapistId)")
    List<Child> findByCenterIdAndOptionalTherapistId(
            @Param("centerId") UUID centerId,
            @Param("therapistId") UUID therapistId
    );

    // Dashboard: 센터별 활성 아동 수 카운트
    long countByCenter_IdAndStatus(UUID centerId, String status);

    // Migration: 이름과 생년월일로 중복 체크
    boolean existsByCenter_IdAndNameAndBirthDate(UUID centerId, String name, java.time.LocalDate birthDate);

    // Migration: 이름으로 아동 조회 (목표 연결용)
    java.util.Optional<Child> findByCenter_IdAndName(UUID centerId, String name);

    // Dashboard: 센터별 활성 아동 목록 조회 (연령 계산용)
    List<Child> findByCenter_IdAndStatus(UUID centerId, String status);
}
