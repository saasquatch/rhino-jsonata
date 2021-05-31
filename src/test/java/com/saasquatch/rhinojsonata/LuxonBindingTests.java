package com.saasquatch.rhinojsonata;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

public class LuxonBindingTests {

  private static final String LUXON_JS_URL = "https://cdn.jsdelivr.net/npm/luxon@1.27.0/build/global/luxon.min.js";

  private static JSONata jsonata;

  @BeforeAll
  public static void beforeAll() {
    jsonata = JSONata.create();
  }

  @Test
  public void test() throws Exception {
    final Scriptable scope;
    final Context cx = new ContextFactory().enterContext();
    try (Reader luxonReader = new InputStreamReader(new URL(LUXON_JS_URL).openStream(), UTF_8)) {
      scope = cx.initSafeStandardObjects();
      cx.evaluateReader(scope, luxonReader, null, 1, null);
      cx.evaluateString(scope, ""
          + "function getProperties(clazz) {\n"
          + "  return Object.getOwnPropertyNames(clazz)\n"
          + "    .filter((p) => ![\"prototype\", \"name\", \"length\"].includes(p))\n"
          + "    .reduce((props, p) => {\n"
          + "      props[p] = clazz[p];\n"
          + "      return props;\n"
          + "    }, {});\n"
          + "}", null, 1, null);
    } finally {
      Context.exit();
    }
    final JSONataExpression expression = jsonata.parse(
        "$Duration.fromISO(\"P2M\").plus({\"months\":3, \"days\":10}).toISO()",
        JSONataExpressionOptions.newBuilder().setScope(scope).build());
    expression.assign("Duration", "getProperties(luxon.Duration)");
    assertEquals("P5M10D", expression.evaluate().textValue());
  }

  @Test
  public void test2() throws Exception {
    final Scriptable jsonataFriendlyLuxonDuration;
    final Context cx = new ContextFactory().enterContext();
    try (Reader luxonReader = new InputStreamReader(new URL(LUXON_JS_URL).openStream(), UTF_8)) {
      final Scriptable scope = cx.initSafeStandardObjects();
      cx.evaluateReader(scope, luxonReader, null, 1, null);
      final Function getPropertiesFunction = (Function) cx.evaluateString(scope, ""
          + "(clazz) => {\n"
          + "  return Object.getOwnPropertyNames(clazz)\n"
          + "    .filter((p) => ![\"prototype\", \"name\", \"length\"].includes(p))\n"
          + "    .reduce((props, p) => {\n"
          + "      props[p] = clazz[p];\n"
          + "      return props;\n"
          + "    }, {});\n"
          + "}", null, 1, null);
      final Scriptable luxon = (Scriptable) ScriptableObject.getProperty(scope, "luxon");
      final Scriptable luxonDuration = (Scriptable) ScriptableObject.getProperty(luxon, "Duration");
      jsonataFriendlyLuxonDuration = (Scriptable) getPropertiesFunction.call(
          cx, scope, scope, new Object[]{luxonDuration});
    } finally {
      Context.exit();
    }
    final JSONataExpression expression = jsonata.parse(
        "$Duration.fromISO(\"P2M\").plus({\"months\":3, \"days\":10}).toISO()");
    expression.assign("Duration", jsonataFriendlyLuxonDuration);
    assertEquals("P5M10D", expression.evaluate().textValue());
  }

}
