package com.saasquatch.rhinojsonata;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.Objects;
import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class JSONataOptions {

  final Duration timeout;
  final int maxDepth;
  final ObjectMapper objectMapper;
  final String jsonataJsSource;

  private JSONataOptions(@Nullable Duration timeout, int maxDepth,
      @Nullable ObjectMapper objectMapper, @Nullable String jsonataJsSource) {
    this.timeout = timeout;
    this.maxDepth = maxDepth;
    this.objectMapper = objectMapper;
    this.jsonataJsSource = jsonataJsSource;
  }

  public static Builder newBuilder() {
    return new Builder();
  }

  public static final class Builder {

    private Duration timeout;
    private int maxDepth;
    private ObjectMapper objectMapper;
    private String jsonataJsSource;

    private Builder() {
    }

    private Builder timeboxExpression(@Nonnull Duration timeout, @Nonnegative int maxDepth) {
      if (timeout.isNegative()) {
        throw new IllegalArgumentException("timeout cannot be negative");
      }
      //noinspection ConstantConditions
      if (maxDepth < 0) {
        throw new IllegalArgumentException("maxDepth cannot be negative");
      }
      this.timeout = timeout;
      this.maxDepth = maxDepth;
      return this;
    }

    public Builder setObjectMapper(@Nonnull ObjectMapper objectMapper) {
      this.objectMapper = Objects.requireNonNull(objectMapper);
      return this;
    }

    public Builder setJSONataJsSource(@Nonnull String jsonataJsSource) {
      this.jsonataJsSource = Objects.requireNonNull(jsonataJsSource);
      return this;
    }

    public JSONataOptions build() {
      return new JSONataOptions(timeout, maxDepth, objectMapper, jsonataJsSource);
    }

  }

}
