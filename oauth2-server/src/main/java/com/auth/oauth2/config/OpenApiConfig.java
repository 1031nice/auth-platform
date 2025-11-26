package com.auth.oauth2.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

@Slf4j
@Configuration
public class OpenApiConfig {

  @Bean
  public OpenAPI customOpenAPI() {
    // Try to load from openapi.yaml file first
    OpenAPI openAPI = loadOpenAPIFromYaml();
    
    // If YAML loading failed, create a basic OpenAPI object
    if (openAPI == null) {
      openAPI = new OpenAPI()
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
      log.warn("Failed to load openapi.yaml, using code-scan only");
    }
    
    return openAPI;
  }

  private OpenAPI loadOpenAPIFromYaml() {
    try {
      ClassPathResource resource = new ClassPathResource("static/openapi.yaml");
      if (!resource.exists()) {
        log.warn("openapi.yaml file not found in static resources");
        return null;
      }

      ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
      try (InputStream inputStream = resource.getInputStream()) {
        @SuppressWarnings("unchecked")
        Map<String, Object> yamlMap = yamlMapper.readValue(inputStream, Map.class);
        
        // Convert Map to OpenAPI object
        OpenAPI openAPI = yamlMapper.convertValue(yamlMap, OpenAPI.class);
        
        if (openAPI != null) {
          log.info("Successfully loaded OpenAPI specification from openapi.yaml");
          return openAPI;
        } else {
          log.error("Failed to parse openapi.yaml");
          return null;
        }
      }
    } catch (Exception e) {
      log.error("Failed to load openapi.yaml file", e);
      return null;
    }
  }
}

