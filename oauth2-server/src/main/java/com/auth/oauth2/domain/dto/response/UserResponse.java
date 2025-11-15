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
public class UserResponse {

  private Long id;
  private String username;
  private String email;
  private List<String> roles;
  private Boolean enabled;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}
