package com.auth.oauth2.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.lang.Nullable;

@Configuration
@Slf4j
public class AuditConfig {

  @Bean
  public OAuth2AuthorizationService oauth2AuthorizationService(
      JdbcTemplate jdbcTemplate, RegisteredClientRepository registeredClientRepository) {
    OAuth2AuthorizationService delegate =
        new JdbcOAuth2AuthorizationService(jdbcTemplate, registeredClientRepository);
    return new OAuth2AuthorizationService() {
      @Override
      public void save(OAuth2Authorization authorization) {
        // Check if this is a refresh token rotation (new refresh token issued)
        // When reuseRefreshTokens(false), Spring automatically rotates refresh tokens
        if (authorization.getRefreshToken() != null) {
          String principalName = authorization.getPrincipalName();
          String clientId = authorization.getRegisteredClientId();
          String refreshTokenId = authorization.getId();
          
          // Log refresh token rotation
          log.info(
              "audit.token.rotation refresh_token_issued principal={} client_id={} authorization_id={}",
              principalName,
              clientId,
              refreshTokenId);
        }
        
        delegate.save(authorization);
        String principalName = authorization.getPrincipalName();
        String clientId = authorization.getRegisteredClientId();
        boolean hasAccessToken = authorization.getAccessToken() != null;
        boolean hasRefreshToken = authorization.getRefreshToken() != null;
        log.info(
            "audit.token.issued principal={} client_id={} access_token={} refresh_token={}",
            principalName,
            clientId,
            hasAccessToken,
            hasRefreshToken);
      }

      @Override
      public void remove(OAuth2Authorization authorization) {
        String principalName = authorization.getPrincipalName();
        String clientId = authorization.getRegisteredClientId();
        boolean hadRefreshToken = authorization.getRefreshToken() != null;
        
        delegate.remove(authorization);
        
        if (hadRefreshToken) {
          log.warn(
              "audit.token.rotation refresh_token_invalidated principal={} client_id={}",
              principalName,
              clientId);
        } else {
          log.info("audit.token.removed principal={} client_id={}", principalName, clientId);
        }
      }

      @Override
      public OAuth2Authorization findById(String id) {
        return delegate.findById(id);
      }

      @Override
      public OAuth2Authorization findByToken(String token, @Nullable OAuth2TokenType tokenType) {
        OAuth2Authorization authorization = delegate.findByToken(token, tokenType);
        
        // Refresh Token Rotation (RTR) - Detect reuse of revoked refresh token
        // When reuseRefreshTokens(false), Spring automatically invalidates old refresh tokens
        // If findByToken returns null for a refresh token, it means the token was already used
        if (authorization == null && tokenType == OAuth2TokenType.REFRESH_TOKEN) {
          // This is a reuse attempt - the refresh token was already used and rotated
          log.error(
              "audit.token.reuse_detected refresh_token_reuse_attempt token_hash={}",
              token.length() > 8 ? token.substring(0, 8) : "***");
          
          // Return null to indicate the token is invalid (already handled by Spring)
          return null;
        }
        
        // Additional check: if authorization exists but refresh token doesn't match
        // This shouldn't happen with Spring's automatic rotation, but we log it for safety
        if (authorization != null && tokenType == OAuth2TokenType.REFRESH_TOKEN) {
          var refreshToken = authorization.getRefreshToken();
          if (refreshToken != null && refreshToken.getToken() != null) {
            String storedTokenValue = refreshToken.getToken().getTokenValue();
            if (storedTokenValue != null && !storedTokenValue.equals(token)) {
              log.error(
                  "audit.token.reuse_detected token_mismatch principal={} client_id={} token_hash={}",
                  authorization.getPrincipalName(),
                  authorization.getRegisteredClientId(),
                  token.length() > 8 ? token.substring(0, 8) : "***");
              
              // Invalidate all tokens for this authorization as a security measure
              delegate.remove(authorization);
              return null;
            }
          }
        }
        
        return authorization;
      }
    };
  }
}

