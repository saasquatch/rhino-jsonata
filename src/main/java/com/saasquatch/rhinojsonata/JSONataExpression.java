package com.saasquatch.rhinojsonata;

import static com.saasquatch.rhinojsonata.JunkDrawer.ASSIGN;
import static com.saasquatch.rhinojsonata.JunkDrawer.EVALUATE;
import static com.saasquatch.rhinojsonata.JunkDrawer.REGISTER_FUNCTION;
import static com.saasquatch.rhinojsonata.JunkDrawer.rethrowRhinoException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import java.io.IOException;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.NativeJSON;
import org.mozilla.javascript.NativeObject;
import org.mozilla.javascript.RhinoException;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.Undefined;
import org.mozilla.javascript.json.JsonParser;

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

  public JsonNode evaluate() {
    return evaluate(JsonNodeFactory.instance.missingNode());
  }

  public JsonNode evaluate(@Nullable JsonNode input) {
    final Object evaluateResult;
    try {
      final Object inputNativeObject;
      if (input == null || input.isNull()) {
        inputNativeObject = null;
      } else if (input.isMissingNode()) {
        inputNativeObject = Undefined.instance;
      } else {
        final String inputStringify = objectMapper.writeValueAsString(input);
        inputNativeObject = new JsonParser(cx, scope).parseValue(inputStringify);
      }
      evaluateLock.lock();
      try {
        evaluateResult = ScriptableObject.callMethod(expressionNativeObject, EVALUATE,
            new Object[]{inputNativeObject});
      } finally {
        evaluateLock.unlock();
      }
    } catch (RhinoException e) {
      return rethrowRhinoException(cx, scope, e);
    } catch (JsonParser.ParseException | IOException e) {
      throw new JSONataException(e.getMessage(), e);
    }
    if (evaluateResult instanceof Undefined) {
      return JsonNodeFactory.instance.missingNode();
    }
    try {
      final String evaluationResultStringify = NativeJSON.stringify(
          cx, scope, evaluateResult, null, null).toString();
      return objectMapper.readTree(evaluationResultStringify);
    } catch (RhinoException e) {
      return rethrowRhinoException(cx, scope, e);
    } catch (IOException e) {
      throw new JSONataException(e.getMessage(), e);
    }
  }

  public void assignJsExpression(@Nonnull String name, @Nonnull String jsExpression) {
    Objects.requireNonNull(name);
    Objects.requireNonNull(jsExpression);
    try {
      ScriptableObject.callMethod(expressionNativeObject, ASSIGN,
          new Object[]{name, cx.evaluateString(scope, jsExpression, null, 1, null)});
    } catch (RhinoException e) {
      rethrowRhinoException(cx, scope, e);
    }
  }

  public void assignJavaObject(@Nonnull String name, @Nonnull Object javaObject) {
    Objects.requireNonNull(name);
    Objects.requireNonNull(javaObject);
    try {
      ScriptableObject.callMethod(expressionNativeObject, ASSIGN,
          new Object[]{name, Context.javaToJS(javaObject, scope)});
    } catch (RhinoException e) {
      rethrowRhinoException(cx, scope, e);
    }
  }

  public void registerJsFunction(@Nonnull String name, @Nonnull String jsFunctionExpression,
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

  public void timeboxExpression(@Nonnull Duration timeout, @Nonnegative int maxDepth) {
    Objects.requireNonNull(timeout);
    jsonata.getTimeboxExpressionFunction().call(cx, scope, scope,
        new Object[]{expressionNativeObject, (int) timeout.toMillis(), maxDepth});
  }

}
