package com.saasquatch.rhinojsonata;

import static com.saasquatch.rhinojsonata.JunkDrawer.TIMEBOX_EXPRESSION_JS;
import static com.saasquatch.rhinojsonata.JunkDrawer.getDefaultJSONataSource;
import static com.saasquatch.rhinojsonata.JunkDrawer.rethrowRhinoException;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Objects;
import javax.annotation.Nonnull;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.NativeObject;
import org.mozilla.javascript.RhinoException;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

public final class JSONata {

  // Lazy init
  private Function timeboxFunction;
  final Context cx;
  final Scriptable scope;
  final ObjectMapper objectMapper;

  private JSONata(@Nonnull Context cx, @Nonnull Scriptable scope,
      @Nonnull ObjectMapper objectMapper) {
    this.cx = cx;
    this.scope = scope;
    this.objectMapper = objectMapper;
  }

  public JSONataExpression parse(@Nonnull String expression) {
    Objects.requireNonNull(expression);
    try {
      final NativeObject expressionNativeObject = (NativeObject) ScriptableObject.callMethod(
          scope, "jsonata", new Object[]{expression});
      return new JSONataExpression(this, expressionNativeObject);
    } catch (RhinoException e) {
      return rethrowRhinoException(cx, scope, e);
    }
  }

  Function getTimeboxExpressionFunction() {
    Function f = timeboxFunction;
    if (f == null) {
      timeboxFunction = f = (Function) cx.evaluateString(
          scope, TIMEBOX_EXPRESSION_JS, null, 1, null);
    }
    return f;
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
          options.objectMapper == null ? new ObjectMapper() : options.objectMapper);
    } catch (RhinoException e) {
      return rethrowRhinoException(cx, scope, e);
    }
  }

}
