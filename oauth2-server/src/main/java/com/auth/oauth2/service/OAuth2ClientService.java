package com.auth.oauth2.service;

import com.auth.oauth2.domain.dto.request.OAuth2ClientRequest;
import com.auth.oauth2.domain.dto.response.OAuth2ClientResponse;
import com.auth.oauth2.domain.entity.OAuth2Client;
import com.auth.oauth2.repository.OAuth2ClientRepository;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OAuth2ClientService {

  private final OAuth2ClientRepository clientRepository;

  @Transactional
  public OAuth2ClientResponse createClient(OAuth2ClientRequest request) {
    if (clientRepository.existsByClientId(request.getClientId())) {
      throw new RuntimeException("Client ID already exists");
    }

    // Store client secret as plain text for OAuth2 compatibility
    // In production, consider using a dedicated secret management solution
    // or encrypting at the database level
    OAuth2Client client =
        OAuth2Client.builder()
            .clientId(request.getClientId())
            .clientSecret(request.getClientSecret()) // Store as plain text for OAuth2
            .redirectUris(request.getRedirectUris())
            .scopes(request.getScopes())
            .grantTypes(request.getGrantTypes())
            .enabled(true)
            .customAccessTokenTtlSeconds(request.getCustomAccessTokenTtlSeconds())
            .customRefreshTokenTtlSeconds(request.getCustomRefreshTokenTtlSeconds())
            .build();

    client = clientRepository.save(client);
    return toResponse(client);
  }

  @Transactional(readOnly = true)
  public OAuth2ClientResponse getClient(String clientId) {
    OAuth2Client client =
        clientRepository
            .findByClientId(clientId)
            .orElseThrow(() -> new RuntimeException("Client not found"));
    return toResponse(client);
  }

  @Transactional(readOnly = true)
  public List<OAuth2ClientResponse> getAllClients() {
    return clientRepository.findAll().stream().map(this::toResponse).collect(Collectors.toList());
  }

  @Transactional
  public void deleteClient(String clientId) {
    OAuth2Client client =
        clientRepository
            .findByClientId(clientId)
            .orElseThrow(() -> new RuntimeException("Client not found"));
    clientRepository.delete(client);
  }

  private OAuth2ClientResponse toResponse(OAuth2Client client) {
    return OAuth2ClientResponse.builder()
        .id(client.getId())
        .clientId(client.getClientId())
        .redirectUris(client.getRedirectUris())
        .scopes(client.getScopes())
        .grantTypes(client.getGrantTypes())
        .enabled(client.getEnabled())
        .customAccessTokenTtlSeconds(client.getCustomAccessTokenTtlSeconds())
        .customRefreshTokenTtlSeconds(client.getCustomRefreshTokenTtlSeconds())
        .createdAt(client.getCreatedAt())
        .updatedAt(client.getUpdatedAt())
        .build();
  }
}
