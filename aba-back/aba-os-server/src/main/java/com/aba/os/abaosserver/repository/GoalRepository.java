package com.aba.os.abaosserver.repository;

import com.aba.os.abaosserver.domain.Goal;
import com.aba.os.abaosserver.domain.Goal.GoalStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GoalRepository extends JpaRepository<Goal, Long> {

    List<Goal> findByChildId(Long childId);

    Optional<Goal> findFirstByChildId(Long childId);

    List<Goal> findByChildIdAndStatus(Long childId, GoalStatus status);

    long countByChildIdAndStatus(Long childId, GoalStatus status);

    void deleteAllByChildId(Long childId);
}
