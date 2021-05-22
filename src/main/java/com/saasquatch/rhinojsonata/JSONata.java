package com.saasquatch.rhinojsonata;

import static com.saasquatch.rhinojsonata.JunkDrawer.JSONATA;
import static com.saasquatch.rhinojsonata.JunkDrawer.TIMEBOX_EXPRESSION_JS;
import static com.saasquatch.rhinojsonata.JunkDrawer.createScope;
import static com.saasquatch.rhinojsonata.JunkDrawer.loadDefaultJSONataSource;
import static com.saasquatch.rhinojsonata.JunkDrawer.rethrowRhinoException;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Objects;
import javax.annotation.Nonnull;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.NativeObject;
import org.mozilla.javascript.RhinoException;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

/**
 * This class represents a JSONata runtime.<br>Calling {@link JSONata#create()} is equivalent to
 * calling {@code require("jsonata")} in jsonata-js.<br>Creating an instance of {@link JSONata}
 * involves multiple expensive operations, including reading the jsonata-es5.min.js source code from
 * disk, spinning up Rhino, executing the JS code, among other things. It is therefore recommended
 * that one shared global instance of {@link JSONata} should be used.
 *
 * @author sli
 * @see #create()
 * @see #parse(String)
 */
public final class JSONata {

  // Lazy init
  private Function timeboxFunction;
  final ContextFactory contextFactory;
  final Scriptable scope;
  final ObjectMapper objectMapper;

  private JSONata(@Nonnull ContextFactory contextFactory, @Nonnull Scriptable scope,
      @Nonnull ObjectMapper objectMapper) {
    this.contextFactory = contextFactory;
    this.scope = scope;
    this.objectMapper = objectMapper;
  }

  /**
   * Parse the given JSONata expression.<br>This method is equivalent to calling {@code
   * jsonata("jsonata expression")} in jsonata-js.
   */
  public JSONataExpression parse(@Nonnull String expression) {
    return parse(expression, JSONataExpressionOptions.newBuilder().build());
  }

  /**
   * Parse the given JSONata expression with custom options.
   */
  public JSONataExpression parse(@Nonnull String expression,
      @Nonnull JSONataExpressionOptions expressionOptions) {
    Objects.requireNonNull(expression);
    final Context cx = contextFactory.enterContext();
    try {
      cx.setOptimizationLevel(-1); // No point in optimizing
      final NativeObject expressionNativeObject = (NativeObject) ScriptableObject.callMethod(
          cx, scope, JSONATA, new Object[]{expression});
      if (expressionOptions.timeboxExpressionTimeboxMillis > 0) {
        getTimeboxExpressionFunction().call(cx, scope, scope,
            new Object[]{expressionNativeObject, expressionOptions.timeboxExpressionTimeboxMillis,
                expressionOptions.timeboxExpressionMaxDepth});
      }
      /*
       * Every JSONataExpression gets its own scope so the original and likely shared JSONata
       * instance doesn't get contaminated
       */
      return new JSONataExpression(this, expressionNativeObject, createScope(contextFactory),
          expressionOptions);
    } catch (RhinoException e) {
      return rethrowRhinoException(cx, scope, objectMapper, e);
    } finally {
      Context.exit();
    }
  }

  Function getTimeboxExpressionFunction() {
    Function f = timeboxFunction;
    if (f == null) {
      final Context cx = contextFactory.enterContext();
      try {
        timeboxFunction = f = (Function) cx.evaluateString(
            scope, TIMEBOX_EXPRESSION_JS, null, 1, null);
      } finally {
        Context.exit();
      }
    }
    return f;
  }

  /**
   * @return An instance of {@link JSONata} with default options.
   */
  public static JSONata create() {
    return create(JSONataOptions.newBuilder().build());
  }

  /**
   * @return An instance of {@link JSONata} with the given custom options.
   */
  public static JSONata create(@Nonnull JSONataOptions options) {
    // Avoid using the global ContextFactory
    final ContextFactory contextFactory = SquatchContextFactory.INSTANCE;
    final String jsonataJsSource =
        options.jsonataJsSource == null ? loadDefaultJSONataSource() : options.jsonataJsSource;
    final ObjectMapper objectMapper =
        options.objectMapper == null ? new ObjectMapper() : options.objectMapper;
    final Scriptable scope = createScope(contextFactory);
    final Context cx = contextFactory.enterContext();
    try {
      cx.evaluateString(scope, jsonataJsSource, null, 1, null);
      return new JSONata(contextFactory, scope, objectMapper);
    } catch (RhinoException e) {
      return rethrowRhinoException(cx, scope, objectMapper, e);
    } finally {
      Context.exit();
    }
  }

}
