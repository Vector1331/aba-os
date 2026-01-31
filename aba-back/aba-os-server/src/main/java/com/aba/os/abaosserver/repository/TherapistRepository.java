package com.aba.os.abaosserver.repository;

import com.aba.os.abaosserver.domain.Therapist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TherapistRepository extends JpaRepository<Therapist, UUID> {

    Optional<Therapist> findByUserId(UUID userId);
}
