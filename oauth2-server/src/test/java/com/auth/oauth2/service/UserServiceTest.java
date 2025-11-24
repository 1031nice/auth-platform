package com.auth.oauth2.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;

import com.auth.oauth2.domain.dto.request.SignupRequest;
import com.auth.oauth2.domain.entity.OAuth2Client;
import com.auth.oauth2.domain.entity.Role;
import com.auth.oauth2.domain.entity.User;
import com.auth.oauth2.repository.OAuth2ClientRepository;
import com.auth.oauth2.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService tests")
class UserServiceTest {

  @Mock private UserRepository userRepository;
  @Mock private OAuth2ClientRepository clientRepository;
  @Mock private PasswordEncoder passwordEncoder;

  @InjectMocks private UserService userService;

  private SignupRequest signupRequest;
  private User savedUser;
  private OAuth2Client oAuth2Client;

  @BeforeEach
  void setUp() {
    signupRequest =
        SignupRequest.builder()
            .email("test@example.com")
            .password("password123")
            .build();

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

    oAuth2Client =
        OAuth2Client.builder()
            .id(1L)
            .clientId("test-client")
            .clientSecret("test-secret")
            .redirectUris(Arrays.asList("http://localhost:3000/callback"))
            .scopes(Arrays.asList("read", "write"))
            .grantTypes(Arrays.asList("authorization_code"))
            .enabled(true)
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();
  }

  @Test
  @DisplayName("signup: 회원가입 성공")
  void signup_shouldCreateUserSuccessfully() {
    // given
    given(userRepository.existsByEmail("test@example.com")).willReturn(false);
    given(passwordEncoder.encode("password123")).willReturn("encodedPassword");
    given(userRepository.save(any(User.class))).willReturn(savedUser);

    // when
    var result = userService.signup(signupRequest);

    // then
    assertThat(result).isNotNull();
    assertThat(result.getUsername()).isEqualTo("test@example.com"); // getUsername()은 email을 반환
    assertThat(result.getEmail()).isEqualTo("test@example.com");
    assertThat(result.getPassword()).isEqualTo("encodedPassword");
    assertThat(result.getRoles()).containsExactly(Role.ROLE_USER);
    assertThat(result.getEnabled()).isTrue();

    then(userRepository).should(times(1)).existsByEmail("test@example.com");
    then(passwordEncoder).should(times(1)).encode("password123");
    then(userRepository).should(times(1)).save(any(User.class));
  }

  @Test
  @DisplayName("signup: redirect_uri와 client_id가 있는 경우 검증 성공")
  void signup_shouldValidateRedirectUriSuccessfully() {
    // given
    signupRequest.setRedirectUri("http://localhost:3000/callback");
    signupRequest.setClientId("test-client");

    given(userRepository.existsByEmail("test@example.com")).willReturn(false);
    given(clientRepository.findByClientId("test-client"))
        .willReturn(Optional.of(oAuth2Client));
    given(passwordEncoder.encode("password123")).willReturn("encodedPassword");
    given(userRepository.save(any(User.class))).willReturn(savedUser);

    // when
    var result = userService.signup(signupRequest);

    // then
    assertThat(result).isNotNull();
    then(clientRepository).should(times(1)).findByClientId("test-client");
  }

  @Test
  @DisplayName("signup 실패: 이미 존재하는 email")
  void signup_shouldThrowExceptionWhenEmailExists() {
    // given
    given(userRepository.existsByEmail("test@example.com")).willReturn(true);

    // when & then
    assertThatThrownBy(() -> userService.signup(signupRequest))
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining("Email already exists");

    then(userRepository).should(times(1)).existsByEmail("test@example.com");
    then(userRepository).should(never()).save(any(User.class));
  }

  @Test
  @DisplayName("signup 실패: 잘못된 client_id")
  void signup_shouldThrowExceptionWhenClientIdInvalid() {
    // given
    signupRequest.setRedirectUri("http://localhost:3000/callback");
    signupRequest.setClientId("invalid-client");

    given(userRepository.existsByEmail("test@example.com")).willReturn(false);
    given(clientRepository.findByClientId("invalid-client")).willReturn(Optional.empty());

    // when & then
    assertThatThrownBy(() -> userService.signup(signupRequest))
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining("Invalid client ID");

    then(clientRepository).should(times(1)).findByClientId("invalid-client");
    then(userRepository).should(never()).save(any(User.class));
  }

  @Test
  @DisplayName("signup 실패: 등록되지 않은 redirect_uri")
  void signup_shouldThrowExceptionWhenRedirectUriNotRegistered() {
    // given
    signupRequest.setRedirectUri("http://malicious.com/callback");
    signupRequest.setClientId("test-client");

    given(userRepository.existsByEmail("test@example.com")).willReturn(false);
    given(clientRepository.findByClientId("test-client"))
        .willReturn(Optional.of(oAuth2Client));

    // when & then
    assertThatThrownBy(() -> userService.signup(signupRequest))
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining("Redirect URI is not registered for this client");

    then(clientRepository).should(times(1)).findByClientId("test-client");
    then(userRepository).should(never()).save(any(User.class));
  }

  @Test
  @DisplayName("signup: redirect_uri만 있고 client_id가 없는 경우 검증 스킵")
  void signup_shouldSkipValidationWhenClientIdIsNull() {
    // given
    signupRequest.setRedirectUri("http://localhost:3000/callback");
    signupRequest.setClientId(null);

    given(userRepository.existsByEmail("test@example.com")).willReturn(false);
    given(passwordEncoder.encode("password123")).willReturn("encodedPassword");
    given(userRepository.save(any(User.class))).willReturn(savedUser);

    // when
    var result = userService.signup(signupRequest);

    // then
    assertThat(result).isNotNull();
    then(clientRepository).should(never()).findByClientId(anyString());
  }
}

