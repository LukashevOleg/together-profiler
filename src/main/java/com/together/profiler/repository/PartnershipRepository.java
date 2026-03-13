package com.together.profiler.repository;

import com.together.profiler.entity.Partnership;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PartnershipRepository extends JpaRepository<Partnership, Long> {

    // Найти активную связь пользователя (он может быть user1 или user2)
    @Query("""
        SELECT p FROM Partnership p
        WHERE p.deletedAt IS NULL
          AND (p.user1Id = :userId OR p.user2Id = :userId)
        """)
    Optional<Partnership> findActiveByUserId(@Param("userId") Long userId);

    // Проверить есть ли уже связь между двумя пользователями
    @Query("""
        SELECT COUNT(p) > 0 FROM Partnership p
        WHERE p.deletedAt IS NULL
          AND ((p.user1Id = :a AND p.user2Id = :b)
            OR (p.user1Id = :b AND p.user2Id = :a))
        """)
    boolean existsActiveBetween(@Param("a") Long userA, @Param("b") Long userB);
}