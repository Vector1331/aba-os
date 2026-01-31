package com.aba.os.abaosserver.repository;

import com.aba.os.abaosserver.domain.Goal;
import com.aba.os.abaosserver.domain.Goal.GoalStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface GoalRepository extends JpaRepository<Goal, UUID> {

    List<Goal> findByChildId(UUID childId);

    List<Goal> findByChildIdAndStatus(UUID childId, GoalStatus status);

    long countByChildIdAndStatus(UUID childId, GoalStatus status);
}
