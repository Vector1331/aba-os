package com.aba.os.abaosserver.repository;

import com.aba.os.abaosserver.domain.Session;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SessionRepository extends JpaRepository<Session, UUID> {

    List<Session> findByChildId(UUID childId);

    long countByChildId(UUID childId);
}
