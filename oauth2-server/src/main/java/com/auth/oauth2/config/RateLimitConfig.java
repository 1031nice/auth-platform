package com.auth.oauth2.config;

import io.github.bucket4j.distributed.ExpirationAfterWriteStrategy;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(
    name = "rate-limit.redis.enabled",
    havingValue = "true",
    matchIfMissing = true)
public class RateLimitConfig {

  private final RateLimitProperties rateLimitProperties;

  @Bean
  public LettuceBasedProxyManager<byte[]> proxyManager() {
    RedisURI redisUri =
        RedisURI.builder()
            .withHost(rateLimitProperties.getRedis().isEnabled() ? "localhost" : "localhost")
            .withPort(6379)
            .build();

    RedisClient redisClient = RedisClient.create(redisUri);

    return LettuceBasedProxyManager.builderFor(redisClient)
        .withExpirationStrategy(
            ExpirationAfterWriteStrategy.basedOnTimeForRefillingBucketUpToMax(
                Duration.ofSeconds(
                    rateLimitProperties.getDefaultConfig().getRefillPeriodSeconds())))
        .build();
  }
}
