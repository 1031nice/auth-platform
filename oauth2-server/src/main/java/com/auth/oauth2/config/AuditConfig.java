package com.auth.oauth2.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.InMemoryOAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.lang.Nullable;

@Configuration
@Slf4j
public class AuditConfig {

  @Bean
  public OAuth2AuthorizationService oauth2AuthorizationService() {
    OAuth2AuthorizationService delegate = new InMemoryOAuth2AuthorizationService();
    return new OAuth2AuthorizationService() {
      @Override
      public void save(OAuth2Authorization authorization) {
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
        delegate.remove(authorization);
        String principalName = authorization.getPrincipalName();
        String clientId = authorization.getRegisteredClientId();
        log.info("audit.token.removed principal={} client_id={}", principalName, clientId);
      }

      @Override
      public OAuth2Authorization findById(String id) {
        return delegate.findById(id);
      }

      @Override
      public OAuth2Authorization findByToken(String token, @Nullable OAuth2TokenType tokenType) {
        return delegate.findByToken(token, tokenType);
      }
    };
  }
}

