package com.saasquatch.rhinojsonata;

import static com.saasquatch.rhinojsonata.JunkDrawer.ASSIGN;
import static com.saasquatch.rhinojsonata.JunkDrawer.EVALUATE;
import static com.saasquatch.rhinojsonata.JunkDrawer.REGISTER_FUNCTION;
import static com.saasquatch.rhinojsonata.JunkDrawer.jsToJsonNode;
import static com.saasquatch.rhinojsonata.JunkDrawer.jsonNodeToJs;
import static com.saasquatch.rhinojsonata.JunkDrawer.rethrowRhinoException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.MissingNode;
import com.fasterxml.jackson.databind.node.NullNode;
import java.util.Map;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.Function;
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

  private final ContextFactory contextFactory;
  private final ObjectMapper objectMapper;
  private final Scriptable expressionNativeObject;
  private final Scriptable scope;
  private final JSONataExpressionOptions expressionOptions;

  JSONataExpression(@Nonnull JSONata jsonata, @Nonnull Scriptable expressionNativeObject,
      @Nonnull Scriptable scope, @Nonnull JSONataExpressionOptions expressionOptions) {
    this.contextFactory = jsonata.contextFactory;
    this.objectMapper = jsonata.objectMapper;
    this.expressionNativeObject = expressionNativeObject;
    this.scope = scope;
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
   *
   * @param input The JSON input. Java {@code null} is not allowed. Use {@link NullNode} for {@code
   *              null} or {@link MissingNode} for {@code undefined}.
   */
  public JsonNode evaluate(@Nonnull JsonNode input) {
    return evaluate(input, EvaluateBindings.EMPTY);
  }

  /**
   * Evaluate the compiled JSONata expression with the given input and bindings.
   *
   * @param input The JSON input. Java {@code null} is not allowed. Use {@link NullNode} for {@code
   *              null} or {@link MissingNode} for {@code undefined}.
   */
  public JsonNode evaluate(@Nonnull JsonNode input, @Nonnull EvaluateBindings bindings) {
    Objects.requireNonNull(input);
    Objects.requireNonNull(bindings);
    final Context cx = contextFactory.enterContext();
    try {
      cx.setOptimizationLevel(-1);
      final Object inputJsObject = jsonNodeToJs(cx, scope, objectMapper, input);
      final Object bindingsJsObject = buildBindings(cx, bindings.bindingsMap);
      prepEvaluationContext(cx);
      final Object evaluateResult;
      try {
        evaluateResult = ScriptableObject.callMethod(cx, expressionNativeObject, EVALUATE,
            new Object[]{inputJsObject, bindingsJsObject});
      } catch (SquatchTimeoutError e) {
        /*
         * The error message comes from https://github.com/jsonata-js/jsonata/blob/97295a6fdf0ed0df7677e5bf36a50bb633eb53a2/test/run-test-suite.js#L158
         * It is licenced under MIT License
         */
        throw new JSONataException("Expression evaluation timeout: Check for infinite loop");
      } catch (StackOverflowError e) {
        /*
         * The error message comes from https://github.com/jsonata-js/jsonata/blob/97295a6fdf0ed0df7677e5bf36a50bb633eb53a2/test/run-test-suite.js#L158
         * It is licenced under MIT License
         */
        throw new JSONataException("Stack overflow error: Check for non-terminating recursive "
            + "function. Consider rewriting as tail-recursive.", e);
      } finally {
        restoreEvaluationContext(cx);
      }
      return jsToJsonNode(cx, scope, objectMapper, evaluateResult);
    } catch (RhinoException e) {
      return rethrowRhinoException(cx, scope, objectMapper, e);
    } finally {
      Context.exit();
    }
  }

  private void prepEvaluationContext(Context cx) {
    cx.setOptimizationLevel(-1); // No point in optimizing
    /*
     * cx may not be an instance of SquatchContext if this library is used within some other custom
     * context
     */
    if (cx instanceof SquatchContext) {
      final SquatchContext squatchContext = (SquatchContext) cx;
      squatchContext.timeoutNanos = expressionOptions.evaluateTimeoutNanos;
    }
  }

  private void restoreEvaluationContext(Context cx) {
    if (cx instanceof SquatchContext) {
      final SquatchContext squatchContext = (SquatchContext) cx;
      squatchContext.timeoutNanos = 0;
    }
  }

  private Object buildBindings(Context cx, @Nonnull Map<String, Object> bindingsMap) {
    if (bindingsMap.isEmpty()) {
      return Undefined.instance;
    }
    final Scriptable nativeObject = cx.newObject(scope);
    bindingsMap.forEach((name, bindingValue) -> {
      final Object bindingJsObject;
      if (bindingValue instanceof String) {
        bindingJsObject = cx.evaluateString(scope, (String) bindingValue, null, 1, null);
      } else if (bindingValue instanceof JsonNode) {
        bindingJsObject = jsonNodeToJs(cx, scope, objectMapper, (JsonNode) bindingValue);
      } else if (bindingValue instanceof Scriptable) {
        bindingJsObject = bindingValue;
      } else {
        throw new AssertionError();
      }
      ScriptableObject.putProperty(nativeObject, name, bindingJsObject);
    });
    return nativeObject;
  }

  /**
   * Bind a value in the form of a JavaScript expression to a name in the expression.
   *
   * @param jsExpression The JS expression to bind. Note that if binding a JS function is desired,
   *                     then this parameter has the same restrictions as the {@link
   *                     JSONataExpression#registerFunction(String, String, String)} method. See
   *                     {@link JSONataExpression#registerFunction(String, String, String)} for more
   *                     details.
   */
  public void assign(@Nonnull String name, @Nonnull String jsExpression) {
    Objects.requireNonNull(name);
    Objects.requireNonNull(jsExpression);
    final Context cx = contextFactory.enterContext();
    try {
      _assign(cx, name, cx.evaluateString(scope, jsExpression, null, 1, null));
    } catch (RhinoException e) {
      rethrowRhinoException(cx, scope, objectMapper, e);
    } finally {
      Context.exit();
    }
  }

  /**
   * Bind a value in the form of a {@link JsonNode} to a name in the expression.
   *
   * @param jsonValue The value in the form of a {@link JsonNode}. Java {@code null} is not allowed.
   *                  Use {@link NullNode} for {@code null} or {@link MissingNode} for {@code
   *                  undefined}.
   */
  public void assign(@Nonnull String name, @Nonnull JsonNode jsonValue) {
    Objects.requireNonNull(name);
    Objects.requireNonNull(jsonValue);
    final Context cx = contextFactory.enterContext();
    try {
      _assign(cx, name, jsonNodeToJs(cx, scope, objectMapper, jsonValue));
    } catch (RhinoException e) {
      rethrowRhinoException(cx, scope, objectMapper, e);
    } finally {
      Context.exit();
    }
  }

  /**
   * Bind a value in the form of a {@link Scriptable} to a name in the expression.
   */
  public void assign(@Nonnull String name, @Nonnull Scriptable scriptable) {
    Objects.requireNonNull(name);
    Objects.requireNonNull(scriptable);
    final Context cx = contextFactory.enterContext();
    try {
      _assign(cx, name, scriptable);
    } catch (RhinoException e) {
      rethrowRhinoException(cx, scope, objectMapper, e);
    } finally {
      Context.exit();
    }
  }

  private void _assign(@Nonnull Context cx, @Nonnull String name, @Nullable Object jsObject) {
    ScriptableObject.callMethod(cx, expressionNativeObject, ASSIGN, new Object[]{name, jsObject});
  }

  /**
   * Bind a JavaScript function to a name in the expression.<br>Note that the JS function string has
   * to be a JS expression that strictly evaluates to a function, which is to say that {@code "a =>
   * a"} and {@code "(function(a) {return a;})"} work, but {@code "function(a) {return a;}"} and
   * {@code "function foo(a) {return a;}"} do not work. Also, depending on the nature of the JS
   * function, it may make the {@link #evaluate()} methods not thread safe.
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
      _registerFunction(cx, name, cx.evaluateString(scope, jsFunctionExpression, null, 1, null),
          signature);
    } catch (RhinoException e) {
      rethrowRhinoException(cx, scope, objectMapper, e);
    } finally {
      Context.exit();
    }
  }

  /**
   * Bind a JavaScript function to a name in the expression.
   */
  public void registerFunction(@Nonnull String name, @Nonnull Function jsFunction,
      @Nullable String signature) {
    Objects.requireNonNull(name);
    Objects.requireNonNull(jsFunction);
    final Context cx = contextFactory.enterContext();
    try {
      _registerFunction(cx, name, jsFunction, signature);
    } catch (RhinoException e) {
      rethrowRhinoException(cx, scope, objectMapper, e);
    } finally {
      Context.exit();
    }
  }

  private void _registerFunction(Context cx, @Nonnull String name, @Nonnull Object jsFunctionObject,
      @Nullable String signature) {
    Objects.requireNonNull(name);
    Objects.requireNonNull(jsFunctionObject);
    ScriptableObject.callMethod(cx, expressionNativeObject, REGISTER_FUNCTION,
        new Object[]{name, jsFunctionObject, signature == null ? Undefined.instance : signature});
  }

}
