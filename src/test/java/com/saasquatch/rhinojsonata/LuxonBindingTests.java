package com.saasquatch.rhinojsonata;

import static com.saasquatch.rhinojsonata.TestJSONataHolder.jsonata;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import org.junit.jupiter.api.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.Scriptable;

public class LuxonBindingTests {

  @SuppressWarnings("SpellCheckingInspection")
  private static final String LUXON_JS_URL = "https://cdn.jsdelivr.net/npm/luxon@1.27.0/build/global/luxon.min.js";

  @Test
  public void test() throws Exception {
    final Scriptable jsonataFriendlyLuxonDuration;
    final Context cx = Context.enter();
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
      final Object luxonDuration = cx.evaluateString(scope, "luxon.Duration", null, 1, null);
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
