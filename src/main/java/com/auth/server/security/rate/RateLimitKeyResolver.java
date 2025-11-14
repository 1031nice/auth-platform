package com.auth.server.security.rate;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Component
public class RateLimitKeyResolver {

  public String resolveIp() {
    ServletRequestAttributes attributes =
        (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
    if (attributes == null) {
      return "unknown";
    }

    HttpServletRequest request = attributes.getRequest();
    String ip = request.getHeader("X-Forwarded-For");

    if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
      ip = request.getHeader("X-Real-IP");
    }

    if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
      ip = request.getRemoteAddr();
    }

    // X-Forwarded-For can contain multiple IPs, take the first one
    if (ip != null && ip.contains(",")) {
      ip = ip.split(",")[0].trim();
    }

    return ip != null ? ip : "unknown";
  }
}

