package com.aba.os.abaosserver.repository;

import com.aba.os.abaosserver.domain.SessionTrial;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SessionTrialRepository extends JpaRepository<SessionTrial, UUID> {

    List<SessionTrial> findBySessionId(UUID sessionId);

    List<SessionTrial> findByGoalId(UUID goalId);

    long countBySessionId(UUID sessionId);
}
