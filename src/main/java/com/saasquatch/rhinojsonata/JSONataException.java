package com.saasquatch.rhinojsonata;

public final class JSONataException extends RuntimeException {

  public JSONataException() {}

  public JSONataException(String message) {
    super(message);
  }

  public JSONataException(String message, Throwable cause) {
    super(message, cause);
  }

  public JSONataException(Throwable cause) {
    super(cause);
  }

}
