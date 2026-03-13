package com.together.profiler.repository;

import com.together.profiler.entity.InviteCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InviteCodeRepository extends JpaRepository<InviteCode, Long> {

    Optional<InviteCode> findByCode(String code);

    Optional<InviteCode> findByInviterIdAndStatus(Long inviterId, InviteCode.InviteStatus status);
}