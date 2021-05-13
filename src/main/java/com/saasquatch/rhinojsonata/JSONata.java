package com.saasquatch.rhinojsonata;

import static com.saasquatch.rhinojsonata.JunkDrawer.TIMEBOX_EXPRESSION_JS;
import static com.saasquatch.rhinojsonata.JunkDrawer.getDefaultJSONataSource;
import static com.saasquatch.rhinojsonata.JunkDrawer.rethrowRhinoException;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Objects;
import javax.annotation.Nonnull;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.NativeObject;
import org.mozilla.javascript.RhinoException;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

public final class JSONata {

  private final Context cx;
  private final Scriptable scope;
  private final ObjectMapper objectMapper;
  private final JSONataOptions options;

  private JSONata(@Nonnull Context cx, @Nonnull Scriptable scope,
      @Nonnull ObjectMapper objectMapper, @Nonnull JSONataOptions options) {
    this.cx = cx;
    this.scope = scope;
    this.objectMapper = objectMapper;
    this.options = options;
  }

  public JSONataExpression parse(@Nonnull String expression) {
    Objects.requireNonNull(expression);
    try {
      final NativeObject expressionNativeObject = (NativeObject) ScriptableObject.callMethod(
          scope, "jsonata", new Object[]{expression});
      applyOptionsToExpression(expressionNativeObject, options);
      return new JSONataExpression(cx, scope, objectMapper, expressionNativeObject);
    } catch (RhinoException e) {
      return rethrowRhinoException(cx, scope, e);
    }
  }

  private void applyOptionsToExpression(@Nonnull NativeObject expressionNativeObject,
      @Nonnull JSONataOptions options) {
    if (options.timeout != null) {
      cx.compileFunction(scope, TIMEBOX_EXPRESSION_JS, null, 1, null)
          .call(cx, scope, scope,
              new Object[]{expressionNativeObject, options.timeout.toMillis(), options.maxDepth});
    }
  }

  public static JSONata create() {
    return create(JSONataOptions.newBuilder().build());
  }

  public static JSONata create(@Nonnull JSONataOptions options) {
    final String jsonataJsString =
        options.jsonataJsSource == null ? getDefaultJSONataSource() : options.jsonataJsSource;
    final Context cx = Context.enter();
    final Scriptable scope = cx.initSafeStandardObjects();
    try {
      cx.evaluateString(scope, jsonataJsString, null, 1, null);
      return new JSONata(cx, scope,
          options.objectMapper == null ? new ObjectMapper() : options.objectMapper, options);
    } catch (RhinoException e) {
      return rethrowRhinoException(cx, scope, e);
    }
  }

}
