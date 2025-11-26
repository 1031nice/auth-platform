package com.auth.oauth2.config;

import com.auth.oauth2.domain.entity.Role;
import com.auth.oauth2.domain.entity.User;
import com.auth.oauth2.repository.UserRepository;
import java.util.Collections;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;

  @EventListener(ApplicationReadyEvent.class)
  @Transactional
  public void initializeDefaultUsers() {
    // Create default test user
    if (!userRepository.existsByEmail("test@example.com")) {
      User testUser =
          User.builder()
              .email("test@example.com")
              .password(passwordEncoder.encode("password123"))
              .roles(Collections.singletonList(Role.ROLE_USER))
              .enabled(true)
              .accountNonExpired(true)
              .accountNonLocked(true)
              .credentialsNonExpired(true)
              .build();

      userRepository.save(testUser);
      log.info("Default test user created: test@example.com / password123");
    } else {
      log.debug("Default test user already exists");
    }
  }
}

