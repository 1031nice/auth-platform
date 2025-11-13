package com.auth.server.repository;

import com.auth.server.domain.entity.RefreshToken;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

  Optional<RefreshToken> findByToken(String token);

  List<RefreshToken> findByUserId(Long userId);

  @Modifying
  @Query("DELETE FROM RefreshToken rt WHERE rt.userId = :userId")
  void deleteByUserId(@Param("userId") Long userId);

  @Modifying
  @Query("DELETE FROM RefreshToken rt WHERE rt.expiresAt < :now")
  void deleteExpiredTokens(@Param("now") java.time.LocalDateTime now);
}

