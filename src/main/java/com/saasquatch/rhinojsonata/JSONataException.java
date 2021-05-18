package com.saasquatch.rhinojsonata;

import com.fasterxml.jackson.databind.JsonNode;
import javax.annotation.Nullable;

public final class JSONataException extends RuntimeException {

  private JsonNode errorJson;

  public JSONataException(String message) {
    super(message);
  }

  public JSONataException(String message, Throwable cause) {
    super(message, cause);
  }

  public JSONataException(String message, Throwable cause, JsonNode errorJson) {
    super(message, cause);
    this.errorJson = errorJson;
  }

  @Nullable
  public JsonNode getErrorJson() {
    return errorJson;
  }

}
