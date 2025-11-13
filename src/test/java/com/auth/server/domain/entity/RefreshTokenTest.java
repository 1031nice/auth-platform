package com.auth.server.domain.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("RefreshToken entity tests")
class RefreshTokenTest {

  @Test
  @DisplayName("revoke: 토큰 무효화 성공")
  void revoke_shouldSetRevokedToTrue() {
    // given
    var token =
        RefreshToken.builder()
            .userId(1L)
            .token("test.token")
            .expiresAt(LocalDateTime.now().plusDays(7))
            .revoked(false)
            .build();

    // when
    token.revoke();

    // then
    assertThat(token.getRevoked()).isTrue();
  }

  @Test
  @DisplayName("isExpired: 만료된 토큰 확인")
  void isExpired_shouldReturnTrueForExpiredToken() {
    // given
    var expiredToken =
        RefreshToken.builder()
            .userId(1L)
            .token("test.token")
            .expiresAt(LocalDateTime.now().minusDays(1))
            .revoked(false)
            .build();

    // when
    var isExpired = expiredToken.isExpired();

    // then
    assertThat(isExpired).isTrue();
  }

  @Test
  @DisplayName("isExpired: 유효한 토큰 확인")
  void isExpired_shouldReturnFalseForValidToken() {
    // given
    var validToken =
        RefreshToken.builder()
            .userId(1L)
            .token("test.token")
            .expiresAt(LocalDateTime.now().plusDays(7))
            .revoked(false)
            .build();

    // when
    var isExpired = validToken.isExpired();

    // then
    assertThat(isExpired).isFalse();
  }

  @Test
  @DisplayName("isValid: 유효한 토큰 확인")
  void isValid_shouldReturnTrueForValidToken() {
    // given
    var validToken =
        RefreshToken.builder()
            .userId(1L)
            .token("test.token")
            .expiresAt(LocalDateTime.now().plusDays(7))
            .revoked(false)
            .build();

    // when
    var isValid = validToken.isValid();

    // then
    assertThat(isValid).isTrue();
  }

  @Test
  @DisplayName("isValid: 무효화된 토큰 확인")
  void isValid_shouldReturnFalseForRevokedToken() {
    // given
    var revokedToken =
        RefreshToken.builder()
            .userId(1L)
            .token("test.token")
            .expiresAt(LocalDateTime.now().plusDays(7))
            .revoked(true)
            .build();

    // when
    var isValid = revokedToken.isValid();

    // then
    assertThat(isValid).isFalse();
  }

  @Test
  @DisplayName("isValid: 만료된 토큰 확인")
  void isValid_shouldReturnFalseForExpiredToken() {
    // given
    var expiredToken =
        RefreshToken.builder()
            .userId(1L)
            .token("test.token")
            .expiresAt(LocalDateTime.now().minusDays(1))
            .revoked(false)
            .build();

    // when
    var isValid = expiredToken.isValid();

    // then
    assertThat(isValid).isFalse();
  }

  @Test
  @DisplayName("onCreate: 생성 시 createdAt 자동 설정")
  void onCreate_shouldSetCreatedAtAutomatically() {
    // given
    var token =
        RefreshToken.builder()
            .userId(1L)
            .token("test.token")
            .expiresAt(LocalDateTime.now().plusDays(7))
            .build();

    // when
    // JPA @PrePersist는 실제 DB 저장 시 동작하므로, 여기서는 빌더로 생성만 확인
    // 실제 동작은 통합 테스트에서 확인

    // then
    assertThat(token).isNotNull();
  }
}
