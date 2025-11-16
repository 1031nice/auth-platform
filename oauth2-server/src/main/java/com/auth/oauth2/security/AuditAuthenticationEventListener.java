package com.auth.oauth2.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationListener;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.stereotype.Component;
import org.springframework.lang.NonNull;

@Component
@Slf4j
public class AuditAuthenticationEventListener
    implements ApplicationListener<AuthenticationSuccessEvent> {

  @Override
  public void onApplicationEvent(@NonNull AuthenticationSuccessEvent event) {
    String principal = event.getAuthentication() != null ? event.getAuthentication().getName() : "unknown";
    String details =
        event.getAuthentication() != null && event.getAuthentication().getDetails() != null
            ? event.getAuthentication().getDetails().toString()
            : "";
    log.info("audit.auth.success principal={} details={}", principal, details);
  }

  @Component
  @Slf4j
  public static class FailureListener
      implements ApplicationListener<AbstractAuthenticationFailureEvent> {
    @Override
    public void onApplicationEvent(@NonNull AbstractAuthenticationFailureEvent event) {
      String principal =
          event.getAuthentication() != null ? event.getAuthentication().getName() : "unknown";
      String exception = event.getException() != null ? event.getException().getClass().getSimpleName() : "Exception";
      log.warn(
          "audit.auth.failure principal={} error={} message={}",
          principal,
          exception,
          event.getException() != null ? event.getException().getMessage() : "");
    }
  }
}

