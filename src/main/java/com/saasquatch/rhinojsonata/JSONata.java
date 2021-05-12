package com.saasquatch.rhinojsonata;

import static com.saasquatch.rhinojsonata.JunkDrawer.readerToString;
import static com.saasquatch.rhinojsonata.JunkDrawer.rethrowRhinoException;
import static java.nio.charset.StandardCharsets.UTF_8;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
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

public final class JSONata {

  private final ObjectMapper mapper = new ObjectMapper();
  final Context cx;
  final Scriptable scope;
  final NativeObject jsonataObject;

  private JSONata(Context cx, Scriptable scope, NativeObject jsonataObject) {
    this.cx = cx;
    this.scope = scope;
    this.jsonataObject = jsonataObject;
  }

  public JsonNode evaluate(@Nullable JsonNode input) {
    final Object evaluateResult;
    try {
      evaluateResult = ScriptableObject.callMethod(jsonataObject, "evaluate",
          new Object[]{new JsonParser(cx, scope).parseValue(mapper.writeValueAsString(input))});
    } catch (RhinoException e) {
      return rethrowRhinoException(cx, scope, e);
    } catch (JsonParser.ParseException | IOException e) {
      throw new JSONataException(e.getMessage(), e);
    }
    if (evaluateResult instanceof Undefined) {
      return JsonNodeFactory.instance.missingNode();
    }
    return mapper.valueToTree(evaluateResult);
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

  public void assignJavaMember(@Nonnull String name, @Nonnull Member methodOrConstructor) {
    try {
      ScriptableObject.callMethod(jsonataObject, "assign",
          new Object[]{name, new FunctionObject(name, methodOrConstructor, scope)});
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

  public void registerJavaMemberFunction(@Nonnull String name, @Nonnull Member methodOrConstructor,
      @Nullable String signature) {
    try {
      ScriptableObject.callMethod(jsonataObject, "assign",
          new Object[]{name, new FunctionObject(name, methodOrConstructor, scope), signature});
    } catch (RhinoException e) {
      rethrowRhinoException(cx, scope, e);
    }
  }

  public static JSONata parse(@Nonnull String expression) {
    Objects.requireNonNull(expression);
    final String jsonataJsString;
    try (
        InputStream jsonataSourceStream = JSONata.class.getResourceAsStream(
            "/saasquatch-jsonata-es5.min.js");
        Reader jsonataSourceReader = new InputStreamReader(jsonataSourceStream, UTF_8);
    ) {
      jsonataJsString = readerToString(jsonataSourceReader);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
    final Context cx = Context.enter();
    final Scriptable scope = cx.initSafeStandardObjects();
    try {
      cx.evaluateString(scope, jsonataJsString, null, 1, null);
      final NativeObject jsonataObject = (NativeObject) ScriptableObject.callMethod(
          scope, "jsonata", new Object[]{expression});
      return new JSONata(cx, scope, jsonataObject);
    } catch (RhinoException e) {
      return rethrowRhinoException(cx, scope, e);
    }
  }

}
