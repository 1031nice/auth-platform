package com.auth.oauth2.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;

import com.auth.oauth2.domain.dto.request.OAuth2ClientRequest;
import com.auth.oauth2.domain.dto.response.OAuth2ClientResponse;
import com.auth.oauth2.domain.entity.OAuth2Client;
import com.auth.oauth2.repository.OAuth2ClientRepository;
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

@ExtendWith(MockitoExtension.class)
@DisplayName("OAuth2ClientService tests")
class OAuth2ClientServiceTest {

  @Mock private OAuth2ClientRepository clientRepository;

  @InjectMocks private OAuth2ClientService oAuth2ClientService;

  private OAuth2ClientRequest clientRequest;
  private OAuth2Client client;

  @BeforeEach
  void setUp() {
    clientRequest =
        OAuth2ClientRequest.builder()
            .clientId("test-client")
            .clientSecret("test-secret")
            .redirectUris(Arrays.asList("http://localhost:3000/callback"))
            .scopes(Arrays.asList("read", "write"))
            .grantTypes(Arrays.asList("authorization_code", "refresh_token"))
            .build();

    client =
        OAuth2Client.builder()
            .id(1L)
            .clientId("test-client")
            .clientSecret("test-secret")
            .redirectUris(Arrays.asList("http://localhost:3000/callback"))
            .scopes(Arrays.asList("read", "write"))
            .grantTypes(Arrays.asList("authorization_code", "refresh_token"))
            .enabled(true)
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();
  }

  @Test
  @DisplayName("createClient: 새로운 클라이언트 생성 성공")
  void createClient_shouldCreateNewClientSuccessfully() {
    // given
    given(clientRepository.existsByClientId("test-client")).willReturn(false);
    given(clientRepository.save(any(OAuth2Client.class))).willReturn(client);

    // when
    var response = oAuth2ClientService.createClient(clientRequest);

    // then
    assertThat(response).isNotNull();
    assertThat(response.getClientId()).isEqualTo("test-client");
    assertThat(response.getRedirectUris()).containsExactly("http://localhost:3000/callback");
    assertThat(response.getScopes()).containsExactlyInAnyOrder("read", "write");
    assertThat(response.getGrantTypes())
        .containsExactlyInAnyOrder("authorization_code", "refresh_token");
    assertThat(response.getEnabled()).isTrue();

    then(clientRepository).should(times(1)).existsByClientId("test-client");
    then(clientRepository).should(times(1)).save(any(OAuth2Client.class));
  }

  @Test
  @DisplayName("클라이언트 생성 실패: 이미 존재하는 Client ID")
  void createClient_shouldThrowExceptionWhenClientIdExists() {
    // given
    given(clientRepository.existsByClientId("test-client")).willReturn(true);

    // when & then
    assertThatThrownBy(() -> oAuth2ClientService.createClient(clientRequest))
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining("Client ID already exists");

    then(clientRepository).should(times(1)).existsByClientId("test-client");
    then(clientRepository).should(never()).save(any(OAuth2Client.class));
  }

  @Test
  @DisplayName("getClient: 클라이언트 조회 성공")
  void getClient_shouldReturnClientSuccessfully() {
    // given
    given(clientRepository.findByClientId("test-client")).willReturn(Optional.of(client));

    // when
    var response = oAuth2ClientService.getClient("test-client");

    // then
    assertThat(response).isNotNull();
    assertThat(response.getClientId()).isEqualTo("test-client");
    assertThat(response.getId()).isEqualTo(1L);

    then(clientRepository).should(times(1)).findByClientId("test-client");
  }

  @Test
  @DisplayName("클라이언트 조회 실패: 존재하지 않는 Client ID")
  void getClient_shouldThrowExceptionWhenClientNotFound() {
    // given
    given(clientRepository.findByClientId("non-existent")).willReturn(Optional.empty());

    // when & then
    assertThatThrownBy(() -> oAuth2ClientService.getClient("non-existent"))
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining("Client not found");

    then(clientRepository).should(times(1)).findByClientId("non-existent");
  }

  @Test
  @DisplayName("getAllClients: 모든 클라이언트 조회 성공")
  void getAllClients_shouldReturnAllClients() {
    // given
    var client2 =
        OAuth2Client.builder()
            .id(2L)
            .clientId("test-client-2")
            .clientSecret("test-secret-2")
            .redirectUris(Arrays.asList("http://localhost:3001/callback"))
            .scopes(Arrays.asList("read"))
            .grantTypes(Arrays.asList("client_credentials"))
            .enabled(true)
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();

    given(clientRepository.findAll()).willReturn(Arrays.asList(client, client2));

    // when
    var responses = oAuth2ClientService.getAllClients();

    // then
    assertThat(responses).hasSize(2);
    assertThat(responses)
        .extracting(OAuth2ClientResponse::getClientId)
        .containsExactlyInAnyOrder("test-client", "test-client-2");

    then(clientRepository).should(times(1)).findAll();
  }

  @Test
  @DisplayName("deleteClient: 클라이언트 삭제 성공")
  void deleteClient_shouldDeleteClientSuccessfully() {
    // given
    given(clientRepository.findByClientId("test-client")).willReturn(Optional.of(client));
    willDoNothing().given(clientRepository).delete(client);

    // when
    oAuth2ClientService.deleteClient("test-client");

    // then
    then(clientRepository).should(times(1)).findByClientId("test-client");
    then(clientRepository).should(times(1)).delete(client);
  }

  @Test
  @DisplayName("클라이언트 삭제 실패: 존재하지 않는 Client ID")
  void deleteClient_shouldThrowExceptionWhenClientNotFound() {
    // given
    given(clientRepository.findByClientId("non-existent")).willReturn(Optional.empty());

    // when & then
    assertThatThrownBy(() -> oAuth2ClientService.deleteClient("non-existent"))
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining("Client not found");

    then(clientRepository).should(times(1)).findByClientId("non-existent");
    then(clientRepository).should(never()).delete(any(OAuth2Client.class));
  }
}
