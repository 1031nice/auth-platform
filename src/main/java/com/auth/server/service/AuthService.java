package com.auth.server.service;

import com.auth.server.domain.dto.request.LoginRequest;
import com.auth.server.domain.dto.response.AuthResponse;
import com.auth.server.domain.entity.User;
import com.auth.server.repository.UserRepository;
import com.auth.server.security.JwtTokenProvider;
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
  private final JwtTokenProvider jwtTokenProvider;
  private final AuthenticationManager authenticationManager;

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

    List<String> roles = extractRoles(user);
    String accessToken =
        jwtTokenProvider.generateAccessToken(user.getUsername(), user.getId(), roles);
    String refreshToken =
        jwtTokenProvider.generateRefreshToken(user.getUsername(), user.getId(), roles);

    return buildAuthResponse(user, accessToken, refreshToken, roles);
  }

  @Transactional(readOnly = true)
  public AuthResponse refreshToken(String refreshToken) {
    if (!jwtTokenProvider.validateToken(refreshToken)
        || !jwtTokenProvider.isRefreshToken(refreshToken)) {
      throw new RuntimeException("Invalid refresh token");
    }

    String username = jwtTokenProvider.getUsernameFromToken(refreshToken);

    User user =
        userRepository
            .findByUsername(username)
            .orElseThrow(() -> new RuntimeException("User not found"));

    List<String> scopes = jwtTokenProvider.getScopes(refreshToken);
    if (scopes == null || scopes.isEmpty()) {
      scopes = extractRoles(user);
    }

    String accessToken =
        jwtTokenProvider.generateAccessToken(user.getUsername(), user.getId(), scopes);
    String newRefreshToken =
        jwtTokenProvider.generateRefreshToken(user.getUsername(), user.getId(), scopes);

    return buildAuthResponse(user, accessToken, newRefreshToken, scopes);
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
