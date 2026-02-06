package com.aba.os.abaosserver.repository;

import com.aba.os.abaosserver.domain.Center;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CenterRepository extends JpaRepository<Center, Long> {

    Optional<Center> findByInviteCode(String inviteCode);
}
