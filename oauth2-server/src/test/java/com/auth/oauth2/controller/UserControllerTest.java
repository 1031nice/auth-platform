package com.auth.oauth2.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.auth.oauth2.domain.dto.request.SignupRequest;
import com.auth.oauth2.domain.entity.Role;
import com.auth.oauth2.domain.entity.User;
import com.auth.oauth2.service.UserService;
import java.time.LocalDateTime;
import java.util.Arrays;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
    controllers = UserController.class,
    excludeAutoConfiguration = {
      org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class
    })
@DisplayName("UserController tests")
class UserControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockBean private UserService userService;

  private User savedUser;

  @BeforeEach
  void setUp() {
    savedUser =
        User.builder()
            .id(1L)
            .email("test@example.com")
            .password("encodedPassword")
            .roles(Arrays.asList(Role.ROLE_USER))
            .enabled(true)
            .accountNonExpired(true)
            .accountNonLocked(true)
            .credentialsNonExpired(true)
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();
  }

  @Test
  @DisplayName("GET /signup: 회원가입 페이지 표시")
  void showSignupForm_shouldReturnSignupPage() throws Exception {
    // when & then
    mockMvc
        .perform(get("/signup"))
        .andExpect(status().isOk())
        .andExpect(view().name("signup"))
        .andExpect(model().attributeExists("signupRequest"))
        .andExpect(model().attributeDoesNotExist("error"));
  }

  @Test
  @DisplayName("GET /signup: redirect_uri와 client_id 파라미터 전달")
  void showSignupForm_shouldIncludeRedirectUriAndClientId() throws Exception {
    // when & then
    mockMvc
        .perform(
            get("/signup")
                .param("redirectUri", "http://localhost:3000/callback")
                .param("clientId", "test-client"))
        .andExpect(status().isOk())
        .andExpect(view().name("signup"))
        .andExpect(model().attribute("redirectUri", "http://localhost:3000/callback"))
        .andExpect(model().attribute("clientId", "test-client"));
  }

  @Test
  @DisplayName("POST /signup: 회원가입 성공 - redirect_uri 없음")
  void signup_shouldRedirectToLoginWhenNoRedirectUri() throws Exception {
    // given
    given(userService.signup(any(SignupRequest.class))).willReturn(savedUser);

    // when & then
    mockMvc
        .perform(
            post("/signup")
                .param("email", "test@example.com")
                .param("password", "password123"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/login"))
        .andExpect(flash().attributeExists("signupSuccess"));

    then(userService).should(times(1)).signup(any(SignupRequest.class));
  }

  @Test
  @DisplayName("POST /signup: 회원가입 성공 - redirect_uri 있음")
  void signup_shouldRedirectToCallbackWhenRedirectUriProvided() throws Exception {
    // given
    given(userService.signup(any(SignupRequest.class))).willReturn(savedUser);

    // when & then
    mockMvc
        .perform(
            post("/signup")
                .param("email", "test@example.com")
                .param("password", "password123")
                .param("redirectUri", "http://localhost:3000/callback")
                .param("clientId", "test-client"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("http://localhost:3000/callback?success=true"));

    then(userService).should(times(1)).signup(any(SignupRequest.class));
  }

  @Test
  @DisplayName("POST /signup: 유효성 검증 실패")
  void signup_shouldReturnSignupPageWhenValidationFails() throws Exception {
    // when & then
    mockMvc
        .perform(
            post("/signup")
                .param("email", "invalid-email")
                .param("password", "123")) // 최소 길이 미달
        .andExpect(status().isOk())
        .andExpect(view().name("signup"))
        .andExpect(model().attributeHasErrors("signupRequest"));

    then(userService).should(never()).signup(any(SignupRequest.class));
  }

  @Test
  @DisplayName("POST /signup: 서비스 예외 발생 시 에러 메시지 표시")
  void signup_shouldShowErrorWhenServiceThrowsException() throws Exception {
    // given
    given(userService.signup(any(SignupRequest.class)))
        .willThrow(new RuntimeException("Email already exists"));

    // when & then
    mockMvc
        .perform(
            post("/signup")
                .param("email", "test@example.com")
                .param("password", "password123"))
        .andExpect(status().isOk())
        .andExpect(view().name("signup"))
        .andExpect(model().attribute("error", "Email already exists"));

    then(userService).should(times(1)).signup(any(SignupRequest.class));
  }

  @Test
  @DisplayName("POST /signup: 예외 발생 시 redirect_uri와 client_id 유지")
  void signup_shouldPreserveRedirectUriAndClientIdOnError() throws Exception {
    // given
    given(userService.signup(any(SignupRequest.class)))
        .willThrow(new RuntimeException("Email already exists"));

    // when & then
    mockMvc
        .perform(
            post("/signup")
                .param("email", "test@example.com")
                .param("password", "password123")
                .param("redirectUri", "http://localhost:3000/callback")
                .param("clientId", "test-client"))
        .andExpect(status().isOk())
        .andExpect(view().name("signup"))
        .andExpect(model().attribute("error", "Email already exists"))
        .andExpect(model().attribute("redirectUri", "http://localhost:3000/callback"))
        .andExpect(model().attribute("clientId", "test-client"));
  }
}

