package com.auth.oauth2.repository;

import com.auth.oauth2.domain.entity.OAuth2Client;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OAuth2ClientRepository extends JpaRepository<OAuth2Client, Long> {

  Optional<OAuth2Client> findByClientId(String clientId);

  boolean existsByClientId(String clientId);
}
