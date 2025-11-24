package com.auth.oauth2.repository;

import com.auth.oauth2.domain.entity.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

  Optional<User> findByEmail(String email);

  boolean existsByEmail(String email);

  // Spring Security 호환성을 위한 메서드 (실제로는 email로 조회)
  default Optional<User> findByUsername(String username) {
    return findByEmail(username);
  }
}
