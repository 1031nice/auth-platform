package com.auth.oauth2.domain.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "oauth2_clients")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OAuth2Client {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true, length = 100)
  private String clientId;

  @Column(nullable = false, length = 255)
  private String clientSecret;

  @ElementCollection(fetch = FetchType.EAGER)
  @CollectionTable(
      name = "oauth2_client_redirect_uris",
      joinColumns = @JoinColumn(name = "client_id"))
  @Column(name = "redirect_uri")
  @Builder.Default
  private List<String> redirectUris = new ArrayList<>();

  @ElementCollection(fetch = FetchType.EAGER)
  @CollectionTable(name = "oauth2_client_scopes", joinColumns = @JoinColumn(name = "client_id"))
  @Column(name = "scope")
  @Builder.Default
  private List<String> scopes = new ArrayList<>();

  @ElementCollection(fetch = FetchType.EAGER)
  @CollectionTable(
      name = "oauth2_client_grant_types",
      joinColumns = @JoinColumn(name = "client_id"))
  @Column(name = "grant_type")
  @Builder.Default
  private List<String> grantTypes = new ArrayList<>();

  @Column(nullable = false)
  @Builder.Default
  private Boolean enabled = true;

  /**
   * 커스텀 액세스 토큰 TTL (초 단위)
   * null이면 기본 설정 사용
   */
  @Column(name = "custom_access_token_ttl_seconds")
  private Long customAccessTokenTtlSeconds;

  /**
   * 커스텀 리프레시 토큰 TTL (초 단위)
   * null이면 기본 설정 사용
   */
  @Column(name = "custom_refresh_token_ttl_seconds")
  private Long customRefreshTokenTtlSeconds;

  @Column(nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(nullable = false)
  private LocalDateTime updatedAt;

  @PrePersist
  protected void onCreate() {
    createdAt = LocalDateTime.now();
    updatedAt = LocalDateTime.now();
  }

  @PreUpdate
  protected void onUpdate() {
    updatedAt = LocalDateTime.now();
  }
}
