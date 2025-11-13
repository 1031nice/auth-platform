package com.auth.server.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.*;

import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

@DisplayName("GlobalExceptionHandler tests")
class GlobalExceptionHandlerTest {

  private GlobalExceptionHandler exceptionHandler;

  @BeforeEach
  void setUp() {
    exceptionHandler = new GlobalExceptionHandler();
  }

  @Test
  @DisplayName("handleTokenReuseException: 토큰 재사용 예외 처리")
  void handleTokenReuseException_shouldReturnUnauthorizedResponse() {
    // given
    var exception =
        new TokenReuseException("Token has been revoked. This may indicate a security breach.");

    // when
    var response = exceptionHandler.handleTokenReuseException(exception);

    // then
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().get("status")).isEqualTo(HttpStatus.UNAUTHORIZED.value());
    assertThat(response.getBody().get("message"))
        .isEqualTo("Token has been revoked. This may indicate a security breach.");
    assertThat(response.getBody().get("error")).isEqualTo("TOKEN_REUSE_DETECTED");
  }

  @Test
  @DisplayName("handleInvalidTokenException: 유효하지 않은 토큰 예외 처리")
  void handleInvalidTokenException_shouldReturnUnauthorizedResponse() {
    // given
    var exception = new InvalidTokenException("Invalid refresh token");

    // when
    var response = exceptionHandler.handleInvalidTokenException(exception);

    // then
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().get("status")).isEqualTo(HttpStatus.UNAUTHORIZED.value());
    assertThat(response.getBody().get("message")).isEqualTo("Invalid refresh token");
    assertThat(response.getBody().get("error")).isEqualTo("INVALID_TOKEN");
  }

  @Test
  @DisplayName("handleRuntimeException: 런타임 예외 처리")
  void handleRuntimeException_shouldReturnBadRequestResponse() {
    // given
    var exception = new RuntimeException("User not found");

    // when
    var response = exceptionHandler.handleRuntimeException(exception);

    // then
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().get("status")).isEqualTo(HttpStatus.BAD_REQUEST.value());
    assertThat(response.getBody().get("message")).isEqualTo("User not found");
  }

  @Test
  @DisplayName("handleGlobalException: 일반 예외 처리")
  void handleGlobalException_shouldReturnInternalServerErrorResponse() {
    // given
    var exception = new Exception("Unexpected error occurred");

    // when
    var response = exceptionHandler.handleGlobalException(exception);

    // then
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().get("status"))
        .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
    assertThat(response.getBody().get("message")).isEqualTo("An unexpected error occurred");
  }

  @Test
  @DisplayName("handleValidationExceptions: 유효성 검증 예외 처리")
  void handleValidationExceptions_shouldReturnBadRequestWithFieldErrors() {
    // given
    // MethodArgumentNotValidException은 Mockito로 생성하기 어려우므로
    // 실제로는 통합 테스트에서 확인하는 것이 좋지만,
    // 여기서는 기본 구조만 확인
    var bindingResult = mock(BindingResult.class);
    var fieldError =
        new FieldError("loginRequest", "username", "Username is required");
    given(bindingResult.getAllErrors())
        .willReturn(java.util.Arrays.asList(fieldError));

    var exception = new MethodArgumentNotValidException(null, bindingResult);

    // when
    var response = exceptionHandler.handleValidationExceptions(exception);

    // then
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().get("status")).isEqualTo(HttpStatus.BAD_REQUEST.value());
    assertThat(response.getBody().get("message")).isEqualTo("Validation failed");
    assertThat(response.getBody().get("errors")).isNotNull();
  }
}
