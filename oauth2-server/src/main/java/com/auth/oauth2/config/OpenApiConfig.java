package com.auth.oauth2.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

  @Bean
  public OpenAPI customOpenAPI() {
    return new OpenAPI()
        .info(
            new Info()
                .title("Auth Platform API")
                .version("1.0.0")
                .description(
                    "Centralized authentication and authorization platform API. "
                        + "This API provides OAuth2/OIDC-compliant authentication services for multiple side projects."))
        .servers(
            List.of(
                new Server().url("http://localhost:8081").description("OAuth2 Authorization Server"),
                new Server().url("http://localhost:8082").description("OAuth2 Resource Server")));
  }
}

