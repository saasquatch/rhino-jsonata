package com.saasquatch.rhinojsonata;

import static com.saasquatch.rhinojsonata.JunkDrawer.ASSIGN;
import static com.saasquatch.rhinojsonata.JunkDrawer.EVALUATE;
import static com.saasquatch.rhinojsonata.JunkDrawer.REGISTER_FUNCTION;
import static com.saasquatch.rhinojsonata.JunkDrawer.jsonNodeToJs;
import static com.saasquatch.rhinojsonata.JunkDrawer.rethrowRhinoException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import java.io.IOException;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.NativeJSON;
import org.mozilla.javascript.NativeObject;
import org.mozilla.javascript.RhinoException;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.Undefined;

/**
 * A compiled JSONata expression. This class is equivalent to the result of calling {@code
 * jsonata("jsonata expression")} in jsonata-js.
 *
 * @author sli
 * @see JSONata#parse(String)
 */
public final class JSONataExpression {

  private final Lock evaluateLock = new ReentrantLock();
  private final Context cx;
  private final Scriptable scope;
  private final ObjectMapper objectMapper;
  private final JSONata jsonata;
  private final NativeObject expressionNativeObject;

  JSONataExpression(@Nonnull JSONata jsonata, @Nonnull NativeObject expressionNativeObject) {
    this.cx = jsonata.cx;
    this.scope = jsonata.scope;
    this.objectMapper = jsonata.objectMapper;
    this.jsonata = jsonata;
    this.expressionNativeObject = expressionNativeObject;
  }

  /**
   * Evaluate the compiled JSONata expression without an input.
   */
  public JsonNode evaluate() {
    return evaluate(JsonNodeFactory.instance.missingNode());
  }

  /**
   * Evaluate the compiled JSONata expression with the given input.
   */
  public JsonNode evaluate(@Nullable JsonNode input) {
    try {
      final Object evaluateResult;
      final Object jsObject = jsonNodeToJs(cx, scope, objectMapper, input);
      evaluateLock.lock();
      try {
        evaluateResult = ScriptableObject.callMethod(expressionNativeObject, EVALUATE,
            new Object[]{jsObject});
      } finally {
        evaluateLock.unlock();
      }
      if (evaluateResult instanceof Undefined) {
        return JsonNodeFactory.instance.missingNode();
      }
      return objectMapper.readTree(NativeJSON.stringify(
          cx, scope, evaluateResult, null, null).toString());
    } catch (RhinoException e) {
      return rethrowRhinoException(cx, scope, e);
    } catch (IOException e) {
      throw new JSONataException(e.getMessage(), e);
    }
  }

  /**
   * Bind a value in the form of a JavaScript expression to a name in the expression.
   */
  public void assign(@Nonnull String name, @Nonnull String jsExpression) {
    Objects.requireNonNull(name);
    Objects.requireNonNull(jsExpression);
    try {
      _assign(name, cx.evaluateString(scope, jsExpression, null, 1, null));
    } catch (RhinoException e) {
      rethrowRhinoException(cx, scope, e);
    }
  }

  /**
   * Bind a value in the form of a {@link JsonNode} to a name in the expression.
   */
  public void assign(@Nonnull String name, @Nullable JsonNode jsonNode) {
    Objects.requireNonNull(name);
    try {
      _assign(name, jsonNodeToJs(cx, scope, objectMapper, jsonNode));
    } catch (RhinoException e) {
      rethrowRhinoException(cx, scope, e);
    }
  }

  private void _assign(@Nonnull String name, @Nullable Object jsObject) {
    ScriptableObject.callMethod(expressionNativeObject, ASSIGN, new Object[]{name, jsObject});
  }

  public void registerFunction(@Nonnull String name, @Nonnull String jsFunctionExpression,
      @Nullable String signature) {
    Objects.requireNonNull(name);
    Objects.requireNonNull(jsFunctionExpression);
    try {
      ScriptableObject.callMethod(expressionNativeObject, REGISTER_FUNCTION,
          new Object[]{name, cx.evaluateString(scope, jsFunctionExpression, null, 1, null),
              signature == null ? Undefined.instance : signature});
    } catch (RhinoException e) {
      rethrowRhinoException(cx, scope, e);
    }
  }

  public void timeboxExpression(@Nonnull Duration timeout, int maxDepth) {
    final int timeoutMillis = (int) timeout.toMillis();
    if (timeoutMillis <= 0) {
      throw new IllegalArgumentException("timeout has to be positive");
    }
    if (maxDepth <= 0) {
      throw new IllegalArgumentException("maxDepth has to be positive");
    }
    try {
      jsonata.getTimeboxExpressionFunction().call(cx, scope, scope,
          new Object[]{expressionNativeObject, timeoutMillis, maxDepth});
    } catch (RhinoException e) {
      rethrowRhinoException(cx, scope, e);
    }
  }

}
