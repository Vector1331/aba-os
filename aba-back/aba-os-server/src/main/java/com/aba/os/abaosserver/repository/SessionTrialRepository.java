package com.aba.os.abaosserver.repository;

import com.aba.os.abaosserver.domain.SessionTrial;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SessionTrialRepository extends JpaRepository<SessionTrial, Long> {

    List<SessionTrial> findBySessionId(Long sessionId);

    List<SessionTrial> findByGoalId(Long goalId);

    long countBySessionId(Long sessionId);

    void deleteAllBySessionId(Long sessionId);
}
