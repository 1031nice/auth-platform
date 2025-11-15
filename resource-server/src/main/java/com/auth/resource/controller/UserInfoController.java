package com.auth.resource.controller;

import com.auth.resource.security.OidcUserInfoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping
@RequiredArgsConstructor
public class UserInfoController {

  private final OidcUserInfoService oidcUserInfoService;

  /**
   * OIDC UserInfo 엔드포인트
   * OAuth2 Authorization Server에서 발급한 액세스 토큰을 사용하여 사용자 정보를 반환합니다.
   */
  @GetMapping("/userinfo")
  public ResponseEntity<OidcUserInfo> getUserInfo(Authentication authentication) {
    OidcUserInfo userInfo = oidcUserInfoService.getUserInfo(authentication);
    return ResponseEntity.ok(userInfo);
  }
}

