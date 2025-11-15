package com.auth.oauth2.domain.dto.response;

import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OAuth2ClientResponse {

  private Long id;
  private String clientId;
  private List<String> redirectUris;
  private List<String> scopes;
  private List<String> grantTypes;
  private Boolean enabled;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}
