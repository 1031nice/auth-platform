package com.auth.server.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.*;

import com.auth.server.config.JwtConfig;
import com.auth.server.domain.dto.response.AuthResponse;
import com.auth.server.domain.entity.RefreshToken;
import com.auth.server.domain.entity.Role;
import com.auth.server.domain.entity.User;
import com.auth.server.exception.InvalidTokenException;
import com.auth.server.exception.TokenReuseException;
import com.auth.server.repository.RefreshTokenRepository;
import com.auth.server.repository.UserRepository;
import com.auth.server.security.JwtTokenProvider;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService refreshToken method tests")
class AuthServiceRefreshTokenTest {

  @Mock private UserRepository userRepository;
  @Mock private RefreshTokenRepository refreshTokenRepository;
  @Mock private JwtTokenProvider jwtTokenProvider;
  @Mock private JwtConfig jwtConfig;

  @InjectMocks private AuthService authService;

  private User testUser;
  private RefreshToken storedToken;
  private String refreshTokenString;

  @BeforeEach
  void setUp() {
    testUser =
        User.builder()
            .id(1L)
            .username("testuser")
            .email("test@example.com")
            .password("encodedPassword")
            .roles(List.of(Role.ROLE_USER))
            .enabled(true)
            .accountNonExpired(true)
            .accountNonLocked(true)
            .credentialsNonExpired(true)
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();

    refreshTokenString = "valid.refresh.token";
    storedToken =
        RefreshToken.builder()
            .id(1L)
            .userId(1L)
            .token(refreshTokenString)
            .expiresAt(LocalDateTime.now().plusDays(7))
            .revoked(false)
            .build();
  }

  @Test
  @DisplayName("refreshToken: 유효한 토큰으로 새 토큰 발급 성공")
  void refreshToken_shouldIssueNewTokensWithValidToken() {
    // given
    var newAccessToken = "new.access.token";
    var newRefreshTokenString = "new.refresh.token";
    var roles = List.of("ROLE_USER");

    given(jwtTokenProvider.validateToken(refreshTokenString)).willReturn(true);
    given(jwtTokenProvider.isRefreshToken(refreshTokenString)).willReturn(true);
    given(refreshTokenRepository.findByToken(refreshTokenString))
        .willReturn(Optional.of(storedToken));
    given(jwtTokenProvider.getUsernameFromToken(refreshTokenString)).willReturn("testuser");
    given(jwtTokenProvider.getScopes(refreshTokenString)).willReturn(roles);
    given(userRepository.findByUsername("testuser")).willReturn(Optional.of(testUser));
    given(jwtTokenProvider.generateAccessToken("testuser", 1L, roles))
        .willReturn(newAccessToken);
    given(jwtTokenProvider.generateRefreshToken("testuser", 1L, roles))
        .willReturn(newRefreshTokenString);
    given(jwtConfig.getRefreshExpiration()).willReturn(604800000L);
    given(refreshTokenRepository.save(any(RefreshToken.class)))
        .willAnswer(invocation -> invocation.getArgument(0));

    // when
    var response = authService.refreshToken(refreshTokenString);

    // then
    assertThat(response).isNotNull();
    assertThat(response.getAccessToken()).isEqualTo(newAccessToken);
    assertThat(response.getRefreshToken()).isEqualTo(newRefreshTokenString);
    assertThat(response.getUsername()).isEqualTo("testuser");

    // 기존 토큰이 무효화되었는지 확인
    then(refreshTokenRepository).should(times(1)).save(argThat(token -> token.getRevoked()));
    // 새 토큰이 저장되었는지 확인
    then(refreshTokenRepository).should(times(2)).save(any(RefreshToken.class));
  }

  @Test
  @DisplayName("인증 실패: 유효하지 않은 JWT 토큰")
  void refreshToken_shouldThrowExceptionForInvalidJwtToken() {
    // given
    given(jwtTokenProvider.validateToken(refreshTokenString)).willReturn(false);

    // when & then
    assertThatThrownBy(() -> authService.refreshToken(refreshTokenString))
        .isInstanceOf(InvalidTokenException.class)
        .hasMessageContaining("Invalid refresh token");

    then(refreshTokenRepository).shouldHaveNoInteractions();
  }

  @Test
  @DisplayName("인증 실패: Refresh Token이 아닌 토큰")
  void refreshToken_shouldThrowExceptionForNonRefreshToken() {
    // given
    given(jwtTokenProvider.validateToken(refreshTokenString)).willReturn(true);
    given(jwtTokenProvider.isRefreshToken(refreshTokenString)).willReturn(false);

    // when & then
    assertThatThrownBy(() -> authService.refreshToken(refreshTokenString))
        .isInstanceOf(InvalidTokenException.class)
        .hasMessageContaining("Invalid refresh token");

    then(refreshTokenRepository).shouldHaveNoInteractions();
  }

  @Test
  @DisplayName("인증 실패: DB에 존재하지 않는 토큰")
  void refreshToken_shouldThrowExceptionForTokenNotFoundInDb() {
    // given
    given(jwtTokenProvider.validateToken(refreshTokenString)).willReturn(true);
    given(jwtTokenProvider.isRefreshToken(refreshTokenString)).willReturn(true);
    given(refreshTokenRepository.findByToken(refreshTokenString)).willReturn(Optional.empty());

    // when & then
    assertThatThrownBy(() -> authService.refreshToken(refreshTokenString))
        .isInstanceOf(InvalidTokenException.class)
        .hasMessageContaining("Refresh token not found");
  }

  @Test
  @DisplayName("토큰 재사용 감지: 이미 무효화된 토큰 사용 시도")
  void refreshToken_shouldDetectTokenReuse() {
    // given
    var revokedToken =
        RefreshToken.builder()
            .id(1L)
            .userId(1L)
            .token(refreshTokenString)
            .expiresAt(LocalDateTime.now().plusDays(7))
            .revoked(true)
            .build();

    given(jwtTokenProvider.validateToken(refreshTokenString)).willReturn(true);
    given(jwtTokenProvider.isRefreshToken(refreshTokenString)).willReturn(true);
    given(refreshTokenRepository.findByToken(refreshTokenString))
        .willReturn(Optional.of(revokedToken));

    // when & then
    assertThatThrownBy(() -> authService.refreshToken(refreshTokenString))
        .isInstanceOf(TokenReuseException.class)
        .hasMessageContaining("Token has been revoked");

    // 모든 토큰이 삭제되었는지 확인
    then(refreshTokenRepository).should(times(1)).deleteByUserId(1L);
    then(refreshTokenRepository).should(never()).save(any(RefreshToken.class));
  }

  @Test
  @DisplayName("인증 실패: 만료된 토큰")
  void refreshToken_shouldThrowExceptionForExpiredToken() {
    // given
    var expiredToken =
        RefreshToken.builder()
            .id(1L)
            .userId(1L)
            .token(refreshTokenString)
            .expiresAt(LocalDateTime.now().minusDays(1))
            .revoked(false)
            .build();

    given(jwtTokenProvider.validateToken(refreshTokenString)).willReturn(true);
    given(jwtTokenProvider.isRefreshToken(refreshTokenString)).willReturn(true);
    given(refreshTokenRepository.findByToken(refreshTokenString))
        .willReturn(Optional.of(expiredToken));

    // when & then
    assertThatThrownBy(() -> authService.refreshToken(refreshTokenString))
        .isInstanceOf(InvalidTokenException.class)
        .hasMessageContaining("Refresh token has expired");
  }

  @Test
  @DisplayName("refreshToken: 기존 토큰 무효화 후 새 토큰 발급 (RTR)")
  void refreshToken_shouldRevokeOldTokenAndIssueNewToken() {
    // given
    var newAccessToken = "new.access.token";
    var newRefreshTokenString = "new.refresh.token";
    var roles = List.of("ROLE_USER");

    given(jwtTokenProvider.validateToken(refreshTokenString)).willReturn(true);
    given(jwtTokenProvider.isRefreshToken(refreshTokenString)).willReturn(true);
    given(refreshTokenRepository.findByToken(refreshTokenString))
        .willReturn(Optional.of(storedToken));
    given(jwtTokenProvider.getUsernameFromToken(refreshTokenString)).willReturn("testuser");
    given(jwtTokenProvider.getScopes(refreshTokenString)).willReturn(roles);
    given(userRepository.findByUsername("testuser")).willReturn(Optional.of(testUser));
    given(jwtTokenProvider.generateAccessToken("testuser", 1L, roles))
        .willReturn(newAccessToken);
    given(jwtTokenProvider.generateRefreshToken("testuser", 1L, roles))
        .willReturn(newRefreshTokenString);
    given(jwtConfig.getRefreshExpiration()).willReturn(604800000L);
    given(refreshTokenRepository.save(any(RefreshToken.class)))
        .willAnswer(invocation -> invocation.getArgument(0));

    // when
    authService.refreshToken(refreshTokenString);

    // then
    // 기존 토큰이 무효화되었는지 확인
    assertThat(storedToken.getRevoked()).isTrue();
    then(refreshTokenRepository).should(times(1)).save(storedToken);
    // 새 토큰이 저장되었는지 확인
    then(refreshTokenRepository).should(times(1))
        .save(argThat(token -> !token.getRevoked() && token.getToken().equals(newRefreshTokenString)));
  }
}
