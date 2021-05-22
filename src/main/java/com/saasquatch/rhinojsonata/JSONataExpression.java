package com.saasquatch.rhinojsonata;

import static com.saasquatch.rhinojsonata.JunkDrawer.ASSIGN;
import static com.saasquatch.rhinojsonata.JunkDrawer.EVALUATE;
import static com.saasquatch.rhinojsonata.JunkDrawer.REGISTER_FUNCTION;
import static com.saasquatch.rhinojsonata.JunkDrawer.createScope;
import static com.saasquatch.rhinojsonata.JunkDrawer.jsObjectToJsonNode;
import static com.saasquatch.rhinojsonata.JunkDrawer.jsonNodeToJs;
import static com.saasquatch.rhinojsonata.JunkDrawer.rethrowRhinoException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.saasquatch.rhinojsonata.annotations.Beta;
import java.util.Map;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory;
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

  private final ContextFactory contextFactory;
  private final ObjectMapper objectMapper;
  private final NativeObject expressionNativeObject;
  private final JSONataExpressionOptions expressionOptions;
  private final Scriptable scope;

  JSONataExpression(@Nonnull JSONata jsonata, @Nonnull NativeObject expressionNativeObject,
      @Nonnull JSONataExpressionOptions expressionOptions) {
    this.contextFactory = jsonata.contextFactory;
    this.objectMapper = jsonata.objectMapper;
    this.expressionNativeObject = expressionNativeObject;
    this.expressionOptions = expressionOptions;
    // Every JSONataExpression gets its own scope so the original and likely shared JSONata instance
    // doesn't get contaminated
    this.scope = createScope(contextFactory);
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
  public JsonNode evaluate(@Nonnull JsonNode input) {
    return evaluate(input, EvaluationBindings.EMPTY);
  }

  /**
   * Evaluate the compiled JSONata expression with the given input and bindings.
   */
  public JsonNode evaluate(@Nonnull JsonNode input, @Nonnull EvaluationBindings bindings) {
    Objects.requireNonNull(input);
    Objects.requireNonNull(bindings);
    final Context cx = contextFactory.enterContext();
    try {
      final Object inputJsObject = jsonNodeToJs(cx, scope, objectMapper, input);
      final Object bindingsJsObject = buildBindings(cx, bindings.bindingsMap);
      final Object evaluateResult;
      // Only the evaluate call and nothing else should be run in this context
      final Context evaluationCx = contextFactory.enterContext();
      try {
        prepEvaluationContext(evaluationCx);
        evaluateResult = ScriptableObject.callMethod(evaluationCx, expressionNativeObject, EVALUATE,
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
        Context.exit();
      }
      return jsObjectToJsonNode(cx, scope, objectMapper, evaluateResult);
    } catch (RhinoException e) {
      return rethrowRhinoException(cx, scope, objectMapper, e);
    } finally {
      Context.exit();
    }
  }

  private void prepEvaluationContext(Context cx) {
    cx.setOptimizationLevel(-1); // No point in optimizing
    final SquatchContext squatchContext = (SquatchContext) cx;
    squatchContext.timeoutNanos = expressionOptions.evaluateTimeoutNanos;
  }

  private Object buildBindings(Context cx, @Nonnull Map<String, Object> bindingsMap) {
    if (bindingsMap.isEmpty()) {
      return Undefined.instance;
    }
    final NativeObject nativeObject = new NativeObject();
    for (Map.Entry<String, Object> binding : bindingsMap.entrySet()) {
      final String name = binding.getKey();
      final Object bindingValue = binding.getValue();
      final Object bindingJsObject;
      if (bindingValue instanceof String) {
        bindingJsObject = cx.evaluateString(scope, (String) bindingValue, null, 1, null);
      } else if (bindingValue instanceof JsonNode) {
        bindingJsObject = jsonNodeToJs(cx, scope, objectMapper, (JsonNode) bindingValue);
      } else {
        throw new AssertionError();
      }
      ScriptableObject.putProperty(nativeObject, name, bindingJsObject);
    }
    return nativeObject;
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
      rethrowRhinoException(cx, scope, objectMapper, e);
    } finally {
      Context.exit();
    }
  }

  /**
   * Bind a value in the form of a {@link JsonNode} to a name in the expression.
   */
  public void assign(@Nonnull String name, @Nonnull JsonNode jsonNode) {
    Objects.requireNonNull(name);
    Objects.requireNonNull(jsonNode);
    final Context cx = contextFactory.enterContext();
    try {
      _assign(cx, name, jsonNodeToJs(cx, scope, objectMapper, jsonNode));
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
   * to be a JS expression whose value is a function, which is to say that {@code "a => a"} and
   * {@code "(function(a) {return a;})"} work, but {@code "function(a) {return a;}"} and {@code
   * "function foo(a) {return a;}"} do not work. Also, depending on the nature of the JS function,
   * it may make the {@link #evaluate()} methods not thread safe.
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
      rethrowRhinoException(cx, scope, objectMapper, e);
    } finally {
      Context.exit();
    }
  }

}
