package com.saasquatch.rhinojsonata;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.MissingNode;
import com.fasterxml.jackson.databind.node.NullNode;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import javax.annotation.Nonnull;
import org.mozilla.javascript.Scriptable;

/**
 * Bindings for {@link JSONataExpression#evaluate(JsonNode, EvaluateBindings)}
 *
 * @author sli
 * @see #newBuilder()
 */
public final class EvaluateBindings {

  static final EvaluateBindings EMPTY = newBuilder().build();

  final Map<String, Object> bindingsMap;

  private EvaluateBindings(Map<String, Object> bindingsMap) {
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
      bindingsMap.put(Objects.requireNonNull(name), Objects.requireNonNull(jsonValue));
      return this;
    }

    /**
     * Put a binding in the form of a {@link Scriptable}
     */
    public Builder put(@Nonnull String name, @Nonnull Scriptable scriptable) {
      bindingsMap.put(Objects.requireNonNull(name), Objects.requireNonNull(scriptable));
      return this;
    }

    public EvaluateBindings build() {
      return new EvaluateBindings(Collections.unmodifiableMap(new HashMap<>(bindingsMap)));
    }

  }

}
