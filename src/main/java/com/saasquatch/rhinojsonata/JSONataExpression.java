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
import org.mozilla.javascript.ContextFactory;
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
  private final ContextFactory contextFactory;
  private final Scriptable scope;
  private final ObjectMapper objectMapper;
  private final JSONata jsonata;
  private final NativeObject expressionNativeObject;
  private final JSONataExpressionOptions expressionOptions;

  JSONataExpression(@Nonnull JSONata jsonata, @Nonnull NativeObject expressionNativeObject,
      @Nonnull JSONataExpressionOptions expressionOptions) {
    this.contextFactory = jsonata.contextFactory;
    this.scope = jsonata.scope;
    this.objectMapper = jsonata.objectMapper;
    this.jsonata = jsonata;
    this.expressionNativeObject = expressionNativeObject;
    this.expressionOptions = expressionOptions;
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
    // This does not need to be locked
    final Object jsObject = toJsObject(input);
    final Object evaluateResult;
    evaluateLock.lock();
    try {
      // Create cx inside the lock so the correct start time is recorded
      final Context cx = contextFactory.enterContext();
      try {
        cx.setOptimizationLevel(-1); // No point in optimizing
        final SquatchContext squatchContext = (SquatchContext) cx;
        squatchContext.timeoutNanos = expressionOptions.evaluateTimeoutNanos;
        evaluateResult = ScriptableObject.callMethod(cx, expressionNativeObject, EVALUATE,
            new Object[]{jsObject});
      } catch (RhinoException e) {
        return rethrowRhinoException(cx, scope, e);
      } catch (SquatchTimeoutError e) {
        throw new JSONataException("Expression evaluation timeout: Check for infinite loop");
      } finally {
        Context.exit();
      }
    } finally {
      evaluateLock.unlock();
    }
    if (evaluateResult instanceof Undefined) {
      return JsonNodeFactory.instance.missingNode();
    }
    return toJsonNode(evaluateResult);
  }

  private Object toJsObject(@Nullable JsonNode input) {
    final Context cx = contextFactory.enterContext();
    try {
      return jsonNodeToJs(cx, scope, objectMapper, input);
    } finally {
      Context.exit();
    }
  }

  private JsonNode toJsonNode(@Nullable Object jsObject) {
    final Context cx = contextFactory.enterContext();
    try {
      return objectMapper.readTree(NativeJSON.stringify(
          cx, scope, jsObject, null, null).toString());
    } catch (IOException e) {
      throw new JSONataException(e.getMessage(), e);
    } finally {
      Context.exit();
    }
  }

  /**
   * Bind a value in the form of a JavaScript expression to a name in the expression.
   *
   * @param jsExpression The JS expression to bind. Note that if binding a JS function is desired,
   *                     then this parameter has the same restrictions as the {@link
   *                     JSONataExpression#registerFunction(String, String, String)} method. See
   *                     {@link JSONataExpression#registerFunction(String, String, String)} for more
   *                     detail.
   */
  public void assign(@Nonnull String name, @Nonnull String jsExpression) {
    Objects.requireNonNull(name);
    Objects.requireNonNull(jsExpression);
    final Context cx = contextFactory.enterContext();
    try {
      _assign(cx, name, cx.evaluateString(scope, jsExpression, null, 1, null));
    } catch (RhinoException e) {
      rethrowRhinoException(cx, scope, e);
    } finally {
      Context.exit();
    }
  }

  /**
   * Bind a value in the form of a {@link JsonNode} to a name in the expression.
   */
  public void assign(@Nonnull String name, @Nullable JsonNode jsonNode) {
    Objects.requireNonNull(name);
    final Context cx = contextFactory.enterContext();
    try {
      _assign(cx, name, jsonNodeToJs(cx, scope, objectMapper, jsonNode));
    } catch (RhinoException e) {
      rethrowRhinoException(cx, scope, e);
    } finally {
      Context.exit();
    }
  }

  private void _assign(@Nonnull Context cx, @Nonnull String name, @Nullable Object jsObject) {
    ScriptableObject.callMethod(cx, expressionNativeObject, ASSIGN, new Object[]{name, jsObject});
  }

  /**
   * Bind a JavaScript function to a name in the expression.<br>Note that the JS function string has
   * to be a JS expression whose value is a function, which is to say that {@code "a => a"} and
   * {@code "(function(a) {return a;})"} work, but {@code "function(a) {return a;}"} and {@code
   * "function foo(a) {return a;}"} do not work.
   *
   * @param signature The JSONata function signature.
   *                  <a href="https://docs.jsonata.org/embedding-extending#function-signature-syntax">
   *                  Official docs</a>
   */
  public void registerFunction(@Nonnull String name, @Nonnull String jsFunctionExpression,
      @Nullable String signature) {
    Objects.requireNonNull(name);
    Objects.requireNonNull(jsFunctionExpression);
    final Context cx = contextFactory.enterContext();
    try {
      ScriptableObject.callMethod(cx, expressionNativeObject, REGISTER_FUNCTION,
          new Object[]{name, cx.evaluateString(scope, jsFunctionExpression, null, 1, null),
              signature == null ? Undefined.instance : signature});
    } catch (RhinoException e) {
      rethrowRhinoException(cx, scope, e);
    } finally {
      Context.exit();
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
    final Context cx = contextFactory.enterContext();
    try {
      jsonata.getTimeboxExpressionFunction().call(cx, scope, scope,
          new Object[]{expressionNativeObject, timeoutMillis, maxDepth});
    } catch (RhinoException e) {
      rethrowRhinoException(cx, scope, e);
    } finally {
      Context.exit();
    }
  }

}
