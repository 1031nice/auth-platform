package com.auth.server.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "rate-limit")
@Getter
@Setter
public class RateLimitProperties {

  private Redis redis = new Redis();
  private Default defaultConfig = new Default();
  private Endpoints endpoints = new Endpoints();
  private AccountLockout accountLockout = new AccountLockout();

  @Getter
  @Setter
  public static class Redis {
    private boolean enabled = true;
  }

  @Getter
  @Setter
  public static class Default {
    private int capacity = 100;
    private int refillRate = 100;
    private int refillPeriodSeconds = 60;
  }

  @Getter
  @Setter
  public static class Endpoints {
    private EndpointConfig login = new EndpointConfig();
    private EndpointConfig refresh = new EndpointConfig();
    private EndpointConfig oauth2Token = new EndpointConfig();
    private EndpointConfig oauth2Authorize = new EndpointConfig();

    @Getter
    @Setter
    public static class EndpointConfig {
      private int capacity = 100;
      private int refillRate = 100;
      private int refillPeriodSeconds = 60;
    }
  }

  @Getter
  @Setter
  public static class AccountLockout {
    private int maxAttempts = 5;
    private int lockoutDurationSeconds = 900; // 15 minutes
  }
}

