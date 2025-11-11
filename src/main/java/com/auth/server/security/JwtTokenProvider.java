package com.auth.server.security;

import com.auth.server.config.JwtConfig;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import javax.crypto.SecretKey;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

  private final JwtConfig jwtConfig;

  private SecretKey getSigningKey() {
    return Keys.hmacShaKeyFor(jwtConfig.getSecret().getBytes(StandardCharsets.UTF_8));
  }

  public String generateAccessToken(String username, Long userId, List<String> scopes) {
    return buildToken(username, userId, scopes, jwtConfig.getExpiration(), "ACCESS");
  }

  public String generateRefreshToken(String username, Long userId, List<String> scopes) {
    return buildToken(username, userId, scopes, jwtConfig.getRefreshExpiration(), "REFRESH");
  }

  public String generateAccessToken(String username, Long userId) {
    return generateAccessToken(username, userId, Collections.emptyList());
  }

  public String generateRefreshToken(String username, Long userId) {
    return generateRefreshToken(username, userId, Collections.emptyList());
  }

  private String buildToken(
      String username, Long userId, List<String> scopes, Long expirationMillis, String tokenType) {
    Date now = new Date();
    Date expiryDate = new Date(now.getTime() + expirationMillis);

    return Jwts.builder()
        .subject(username)
        .claim("userId", userId)
        .claim("scope", scopes)
        .claim("tokenType", tokenType)
        .issuedAt(now)
        .expiration(expiryDate)
        .signWith(getSigningKey(), Jwts.SIG.HS512)
        .compact();
  }

  public String getUsernameFromToken(String token) {
    Claims claims = getClaims(token);

    return claims.getSubject();
  }

  public boolean validateToken(String token) {
    try {
      Jwts.parser().verifyWith(getSigningKey()).build().parseSignedClaims(token);
      return true;
    } catch (JwtException | IllegalArgumentException e) {
      log.error("JWT token validation error: {}", e.getMessage());
      return false;
    }
  }

  public boolean isRefreshToken(String token) {
    return "REFRESH".equals(getClaims(token).get("tokenType", String.class));
  }

  public List<String> getScopes(String token) {
    Object scopeClaim = getClaims(token).get("scope");
    if (scopeClaim instanceof Collection<?>) {
      return ((Collection<?>) scopeClaim)
          .stream().filter(String.class::isInstance).map(String.class::cast).toList();
    }

    if (scopeClaim instanceof String scopeString) {
      return List.of(scopeString.split(" "));
    }

    return Collections.emptyList();
  }

  public Optional<Long> getUserId(String token) {
    Claims claims = getClaims(token);
    Object userId = claims.get("userId");
    if (userId instanceof Number number) {
      return Optional.of(number.longValue());
    }
    return Optional.empty();
  }

  private Claims getClaims(String token) {
    return Jwts.parser().verifyWith(getSigningKey()).build().parseSignedClaims(token).getPayload();
  }
}
