package com.auth.server.exception;

public class TokenReuseException extends RuntimeException {
  public TokenReuseException(String message) {
    super(message);
  }
}

