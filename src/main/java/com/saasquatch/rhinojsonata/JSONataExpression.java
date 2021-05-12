package com.saasquatch.rhinojsonata;

import static com.saasquatch.rhinojsonata.JunkDrawer.TIMEBOX_EXPRESSION_JS;
import static com.saasquatch.rhinojsonata.JunkDrawer.getDefaultJSONataSource;
import static com.saasquatch.rhinojsonata.JunkDrawer.rethrowRhinoException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import java.io.IOException;
import java.lang.reflect.Member;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.FunctionObject;
import org.mozilla.javascript.NativeObject;
import org.mozilla.javascript.RhinoException;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.Undefined;
import org.mozilla.javascript.json.JsonParser;

public final class JSONataExpression {

  private final Context cx;
  private final Scriptable scope;
  private final NativeObject jsonataObject;
  private final ObjectMapper objectMapper;

  private JSONataExpression(@Nonnull Context cx, @Nonnull Scriptable scope,
      @Nonnull NativeObject jsonataObject, @Nonnull ObjectMapper objectMapper) {
    this.cx = cx;
    this.scope = scope;
    this.jsonataObject = jsonataObject;
    this.objectMapper = objectMapper;
  }

  public JsonNode evaluate(@Nullable JsonNode input) {
    final Object evaluateResult;
    try {
      evaluateResult = ScriptableObject.callMethod(jsonataObject, "evaluate",
          new Object[]{
              new JsonParser(cx, scope).parseValue(objectMapper.writeValueAsString(input))});
    } catch (RhinoException e) {
      return rethrowRhinoException(cx, scope, e);
    } catch (JsonParser.ParseException | IOException e) {
      throw new JSONataException(e.getMessage(), e);
    }
    if (evaluateResult instanceof Undefined) {
      return JsonNodeFactory.instance.missingNode();
    }
    return objectMapper.valueToTree(evaluateResult);
  }

  public void assignJsExpression(@Nonnull String name, @Nonnull String jsExpression) {
    try {
      ScriptableObject.callMethod(jsonataObject, "assign",
          new Object[]{name, cx.evaluateString(scope, jsExpression, null, 1, null)});
    } catch (RhinoException e) {
      rethrowRhinoException(cx, scope, e);
    }
  }

  public void assignJavaObject(@Nonnull String name, @Nonnull Object javaObject) {
    try {
      ScriptableObject.callMethod(jsonataObject, "assign",
          new Object[]{name, Context.javaToJS(javaObject, scope)});
    } catch (RhinoException e) {
      rethrowRhinoException(cx, scope, e);
    }
  }

  public void assignJavaMember(@Nonnull String name, @Nonnull Member javaMember) {
    try {
      ScriptableObject.callMethod(jsonataObject, "assign",
          new Object[]{name, new FunctionObject(name, javaMember, scope)});
    } catch (RhinoException e) {
      rethrowRhinoException(cx, scope, e);
    }
  }

  public void registerJsFunction(@Nonnull String name, @Nonnull String jsFunctionExpression,
      @Nullable String signature) {
    try {
      ScriptableObject.callMethod(jsonataObject, "registerFunction",
          new Object[]{name, cx.evaluateString(scope, jsFunctionExpression, null, 1, null),
              signature});
    } catch (RhinoException e) {
      rethrowRhinoException(cx, scope, e);
    }
  }

  public void registerJavaMemberFunction(@Nonnull String name, @Nonnull Member javaMember,
      @Nullable String signature) {
    try {
      ScriptableObject.callMethod(jsonataObject, "assign",
          new Object[]{name, new FunctionObject(name, javaMember, scope), signature});
    } catch (RhinoException e) {
      rethrowRhinoException(cx, scope, e);
    }
  }

  public static JSONataExpression parse(@Nonnull String expression) {
    return parse(expression, JSONataExpressionOptions.newBuilder().build());
  }

  public static JSONataExpression parse(@Nonnull String expression,
      @Nonnull JSONataExpressionOptions options) {
    Objects.requireNonNull(expression);
    final String jsonataJsString =
        options.jsonataJsSource == null ? getDefaultJSONataSource() : options.jsonataJsSource;
    final Context cx = Context.enter();
    final Scriptable scope = cx.initSafeStandardObjects();
    try {
      cx.evaluateString(scope, jsonataJsString, null, 1, null);
      final NativeObject jsonataObject = (NativeObject) ScriptableObject.callMethod(
          scope, "jsonata", new Object[]{expression});
      applyOptions(cx, scope, jsonataObject, options);
      return new JSONataExpression(cx, scope, jsonataObject,
          options.objectMapper == null ? new ObjectMapper() : options.objectMapper);
    } catch (RhinoException e) {
      return rethrowRhinoException(cx, scope, e);
    }
  }

  private static void applyOptions(Context cx, Scriptable scope, NativeObject jsonataObject,
      JSONataExpressionOptions options) {
    if (options.timeout != null) {
      cx.evaluateString(scope, TIMEBOX_EXPRESSION_JS, null, 1, null);
      ScriptableObject.callMethod(scope, "timeboxExpression",
          new Object[]{jsonataObject, options.timeout, options.maxDepth});
    }
  }

}
