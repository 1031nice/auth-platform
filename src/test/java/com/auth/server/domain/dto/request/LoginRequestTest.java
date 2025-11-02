package com.auth.server.domain.dto.request;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("LoginRequest validation tests")
class LoginRequestTest {

  private Validator validator;

  @BeforeEach
  void setUp() {
    ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
    validator = factory.getValidator();
  }

  @Test
  @DisplayName("Should pass validation when request is valid")
  void shouldPassValidationWhenRequestIsValid() {
    // given
    LoginRequest request =
        LoginRequest.builder().username("testuser").password("password123").build();

    // when
    Set<ConstraintViolation<LoginRequest>> violations = validator.validate(request);

    // then
    assertThat(violations).isEmpty();
  }

  @Test
  @DisplayName("Should fail validation when username is null")
  void shouldFailValidationWhenUsernameIsNull() {
    // given
    LoginRequest request = LoginRequest.builder().username(null).password("password123").build();

    // when
    Set<ConstraintViolation<LoginRequest>> violations = validator.validate(request);

    // then
    assertThat(violations).hasSize(1);
    assertThat(violations.iterator().next().getMessage()).isEqualTo("Username is required");
  }

  @Test
  @DisplayName("Should fail validation when username is blank")
  void shouldFailValidationWhenUsernameIsBlank() {
    // given
    LoginRequest request = LoginRequest.builder().username("").password("password123").build();

    // when
    Set<ConstraintViolation<LoginRequest>> violations = validator.validate(request);

    // then
    assertThat(violations).hasSize(1);
    assertThat(violations.iterator().next().getMessage()).isEqualTo("Username is required");
  }

  @Test
  @DisplayName("Should fail validation when password is null")
  void shouldFailValidationWhenPasswordIsNull() {
    // given
    LoginRequest request = LoginRequest.builder().username("testuser").password(null).build();

    // when
    Set<ConstraintViolation<LoginRequest>> violations = validator.validate(request);

    // then
    assertThat(violations).hasSize(1);
    assertThat(violations.iterator().next().getMessage()).isEqualTo("Password is required");
  }

  @Test
  @DisplayName("Should fail validation when password is blank")
  void shouldFailValidationWhenPasswordIsBlank() {
    // given
    LoginRequest request = LoginRequest.builder().username("testuser").password("   ").build();

    // when
    Set<ConstraintViolation<LoginRequest>> violations = validator.validate(request);

    // then
    assertThat(violations).hasSize(1);
    assertThat(violations.iterator().next().getMessage()).isEqualTo("Password is required");
  }

  @Test
  @DisplayName("Should fail validation when both username and password are null")
  void shouldFailValidationWhenBothUsernameAndPasswordAreNull() {
    // given
    LoginRequest request = LoginRequest.builder().username(null).password(null).build();

    // when
    Set<ConstraintViolation<LoginRequest>> violations = validator.validate(request);

    // then
    assertThat(violations).hasSize(2);
  }
}
