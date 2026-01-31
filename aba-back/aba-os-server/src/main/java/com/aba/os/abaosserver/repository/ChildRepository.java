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
}
