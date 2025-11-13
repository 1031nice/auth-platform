package com.auth.server.service;

import com.auth.server.config.JwtConfig;
import com.auth.server.domain.dto.request.LoginRequest;
import com.auth.server.domain.dto.response.AuthResponse;
import com.auth.server.domain.entity.RefreshToken;
import com.auth.server.domain.entity.User;
import com.auth.server.exception.InvalidTokenException;
import com.auth.server.exception.TokenReuseException;
import com.auth.server.repository.RefreshTokenRepository;
import com.auth.server.repository.UserRepository;
import com.auth.server.security.JwtTokenProvider;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

  private final UserRepository userRepository;
  private final RefreshTokenRepository refreshTokenRepository;
  private final JwtTokenProvider jwtTokenProvider;
  private final AuthenticationManager authenticationManager;
  private final JwtConfig jwtConfig;

  @Transactional
  public AuthResponse login(LoginRequest request) {
    Authentication authentication =
        authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));

    SecurityContextHolder.getContext().setAuthentication(authentication);

    User user =
        userRepository
            .findByUsername(request.getUsername())
            .orElseThrow(() -> new RuntimeException("User not found"));

    // 기존 Refresh Token 삭제 (단일 세션 유지)
    refreshTokenRepository.deleteByUserId(user.getId());

    List<String> roles = extractRoles(user);
    String accessToken =
        jwtTokenProvider.generateAccessToken(user.getUsername(), user.getId(), roles);
    String refreshTokenString =
        jwtTokenProvider.generateRefreshToken(user.getUsername(), user.getId(), roles);

    // Refresh Token 저장
    LocalDateTime expiresAt =
        LocalDateTime.now().plusSeconds(jwtConfig.getRefreshExpiration() / 1000);
    RefreshToken refreshToken =
        RefreshToken.builder()
            .userId(user.getId())
            .token(refreshTokenString)
            .expiresAt(expiresAt)
            .revoked(false)
            .build();
    refreshTokenRepository.save(refreshToken);

    return buildAuthResponse(user, accessToken, refreshTokenString, roles);
  }

  @Transactional
  public AuthResponse refreshToken(String refreshTokenString) {
    // JWT 검증
    if (!jwtTokenProvider.validateToken(refreshTokenString)
        || !jwtTokenProvider.isRefreshToken(refreshTokenString)) {
      throw new InvalidTokenException("Invalid refresh token");
    }

    // DB에서 Refresh Token 조회
    RefreshToken storedToken =
        refreshTokenRepository
            .findByToken(refreshTokenString)
            .orElseThrow(() -> new InvalidTokenException("Refresh token not found"));

    // 재사용 감지: 이미 무효화된 토큰인 경우
    if (storedToken.getRevoked()) {
      log.warn(
          "Token reuse detected for user ID: {}. Possible token theft!",
          storedToken.getUserId());
      // 보안을 위해 해당 사용자의 모든 Refresh Token 무효화
      refreshTokenRepository.deleteByUserId(storedToken.getUserId());
      throw new TokenReuseException(
          "Token has been revoked. This may indicate a security breach.");
    }

    // 만료 확인
    if (!storedToken.isValid()) {
      throw new InvalidTokenException("Refresh token has expired");
    }

    // 사용자 정보 조회
    String username = jwtTokenProvider.getUsernameFromToken(refreshTokenString);
    User user =
        userRepository
            .findByUsername(username)
            .orElseThrow(() -> new RuntimeException("User not found"));

    // 기존 Refresh Token 무효화 (Rotation)
    storedToken.revoke();
    refreshTokenRepository.save(storedToken);

    // 새로운 토큰 발급
    List<String> scopes = jwtTokenProvider.getScopes(refreshTokenString);
    if (scopes == null || scopes.isEmpty()) {
      scopes = extractRoles(user);
    }

    String accessToken =
        jwtTokenProvider.generateAccessToken(user.getUsername(), user.getId(), scopes);
    String newRefreshTokenString =
        jwtTokenProvider.generateRefreshToken(user.getUsername(), user.getId(), scopes);

    // 새로운 Refresh Token 저장
    LocalDateTime expiresAt =
        LocalDateTime.now().plusSeconds(jwtConfig.getRefreshExpiration() / 1000);
    RefreshToken newRefreshToken =
        RefreshToken.builder()
            .userId(user.getId())
            .token(newRefreshTokenString)
            .expiresAt(expiresAt)
            .revoked(false)
            .build();
    refreshTokenRepository.save(newRefreshToken);

    return buildAuthResponse(user, accessToken, newRefreshTokenString, scopes);
  }

  private AuthResponse buildAuthResponse(
      User user, String accessToken, String refreshToken, List<String> roles) {
    return AuthResponse.builder()
        .accessToken(accessToken)
        .refreshToken(refreshToken)
        .id(user.getId())
        .username(user.getUsername())
        .email(user.getEmail())
        .roles(roles)
        .build();
  }

  private List<String> extractRoles(User user) {
    return user.getRoles().stream().map(Enum::name).collect(Collectors.toList());
  }
}
