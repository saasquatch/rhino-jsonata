package com.saasquatch.rhinojsonata;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Options for the {@link JSONata} runtime.
 *
 * @author sli
 * @see #newBuilder()
 */
public final class JSONataOptions {

  final ObjectMapper objectMapper;
  final String jsonataJsSource;

  private JSONataOptions(@Nullable ObjectMapper objectMapper, @Nullable String jsonataJsSource) {
    this.objectMapper = objectMapper;
    this.jsonataJsSource = jsonataJsSource;
  }

  public static Builder newBuilder() {
    return new Builder();
  }

  public static final class Builder {

    private ObjectMapper objectMapper;
    private String jsonataJsSource;

    private Builder() {}

    /**
     * Set the {@link ObjectMapper} used by the {@link JSONata} instance and {@link
     * JSONataExpression} instances it creates.
     */
    public Builder setObjectMapper(@Nonnull ObjectMapper objectMapper) {
      this.objectMapper = Objects.requireNonNull(objectMapper);
      return this;
    }

    /**
     * Set the alternative <a href="https://github.com/jsonata-js/jsonata">jsonata-js</a> source.e
     */
    public Builder setJSONataJsSource(@Nonnull String jsonataJsSource) {
      this.jsonataJsSource = Objects.requireNonNull(jsonataJsSource);
      return this;
    }

    public JSONataOptions build() {
      return new JSONataOptions(objectMapper, jsonataJsSource);
    }

  }

}
