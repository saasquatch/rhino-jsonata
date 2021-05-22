package com.saasquatch.rhinojsonata;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import javax.annotation.Nonnull;

public final class EvaluationBindings {

  static final EvaluationBindings EMPTY = newBuilder().build();

  final Map<String, Object> bindingsMap;

  private EvaluationBindings(Map<String, Object> bindingsMap) {
    this.bindingsMap = bindingsMap;
  }

  public static Builder newBuilder() {
    return new Builder();
  }

  public static final class Builder {

    private final Map<String, Object> bindingsMap = new HashMap<>();

    private Builder() {}

    public Builder put(@Nonnull String name, @Nonnull String jsExpression) {
      bindingsMap.put(Objects.requireNonNull(name), Objects.requireNonNull(jsExpression));
      return this;
    }

    public Builder put(@Nonnull String name, @Nonnull JsonNode jsonNode) {
      bindingsMap.put(Objects.requireNonNull(name), Objects.requireNonNull(jsonNode));
      return this;
    }

    public EvaluationBindings build() {
      return new EvaluationBindings(Collections.unmodifiableMap(new HashMap<>(bindingsMap)));
    }

  }

}
