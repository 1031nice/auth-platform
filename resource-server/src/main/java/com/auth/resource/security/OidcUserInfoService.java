package com.auth.resource.security;

import com.auth.resource.repository.UserRepository;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OidcUserInfoService {

  private final UserRepository userRepository;

  /**
   * JWT 토큰에서 사용자 정보를 추출하여 OIDC UserInfo를 생성합니다.
   * Resource Server에서 호출되며, JWT의 claims에서 사용자 정보를 가져옵니다.
   */
  public OidcUserInfo getUserInfo(Authentication authentication) {
    if (authentication == null || !(authentication.getPrincipal() instanceof Jwt)) {
      return createDefaultUserInfo("unknown");
    }

    Jwt jwt = (Jwt) authentication.getPrincipal();
    
    // JWT claims에서 사용자 정보 추출
    String username = jwt.getClaimAsString("username");
    String userId = jwt.getClaimAsString("userId");
    String email = jwt.getClaimAsString("email");

    // JWT에 사용자 정보가 있으면 그대로 사용
    if (username != null || userId != null) {
      Map<String, Object> claims = new HashMap<>();
      claims.put("sub", userId != null ? userId : username);
      if (username != null) {
        claims.put("name", username);
        claims.put("preferred_username", username);
      }
      if (email != null) {
        claims.put("email", email);
        claims.put("email_verified", true);
      }
      return new OidcUserInfo(claims);
    }

    // JWT에 사용자 정보가 없으면 DB에서 조회 시도
    String subject = jwt.getSubject();
    if (subject != null) {
      return userRepository
          .findByUsername(subject)
          .map(
              user -> {
                Map<String, Object> claims = new HashMap<>();
                claims.put("sub", user.getId().toString());
                claims.put("name", user.getUsername());
                claims.put("email", user.getEmail());
                claims.put("email_verified", true);
                claims.put("preferred_username", user.getUsername());
                return new OidcUserInfo(claims);
              })
          .orElseGet(() -> createDefaultUserInfo(subject));
    }

    return createDefaultUserInfo("unknown");
  }

  private OidcUserInfo createDefaultUserInfo(String subject) {
    Map<String, Object> claims = new HashMap<>();
    claims.put("sub", subject != null ? subject : "unknown");
    return new OidcUserInfo(claims);
  }
}

