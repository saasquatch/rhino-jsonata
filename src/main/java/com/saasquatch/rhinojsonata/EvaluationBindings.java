package com.saasquatch.rhinojsonata;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.MissingNode;
import com.fasterxml.jackson.databind.node.NullNode;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import javax.annotation.Nonnull;

/**
 * Bindings for {@link JSONataExpression#evaluate(JsonNode, EvaluationBindings)}
 *
 * @author sli
 * @see #newBuilder()
 */
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

    /**
     * Put a binding in the form of a JavaScript expression
     */
    public Builder put(@Nonnull String name, @Nonnull String jsExpression) {
      bindingsMap.put(Objects.requireNonNull(name), Objects.requireNonNull(jsExpression));
      return this;
    }

    /**
     * Put a binding in the form of a {@link JsonNode}
     *
     * @param jsonValue The value in the form of a {@link JsonNode}. Java {@code null} is not
     *                  allowed. Use {@link NullNode} for {@code null} or {@link MissingNode} for
     *                  {@code undefined}.
     */
    public Builder put(@Nonnull String name, @Nonnull JsonNode jsonValue) {
      // deepCopy to ensure immutability
      bindingsMap.put(Objects.requireNonNull(name), jsonValue.deepCopy());
      return this;
    }

    public EvaluationBindings build() {
      return new EvaluationBindings(Collections.unmodifiableMap(new HashMap<>(bindingsMap)));
    }

  }

}
