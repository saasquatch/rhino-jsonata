package com.saasquatch.rhinojsonata;

import static com.saasquatch.rhinojsonata.TestJSONataHolder.jsonata;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.net.URL;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.Scriptable;

public class LuxonBindingTests {

  @SuppressWarnings("SpellCheckingInspection")
  private static final String LUXON_JS_URL = "https://cdn.jsdelivr.net/npm/luxon@1.27.0/build/global/luxon.min.js";
  private static Scriptable luxonDuration;

  @BeforeAll
  public static void beforeAll() {
    luxonDuration = getLuxonDuration();
  }

  @Test
  public void test1() {
    final JSONataExpression expression = jsonata.parse(
        "$Duration.fromISO(\"P2M\").plus({\"months\":3, \"days\":10}).toISO()");
    expression.assign("Duration", luxonDuration);
    assertEquals("P5M10D", expression.evaluate().textValue());
  }

  @Test
  public void test2() {
    final JSONataExpression expression = jsonata.parse(
        "$Duration.fromISO(\"P2M\").plus({\"months\":3, \"days\":10}).toISO()");
    final JsonNode evalResult = expression.evaluate(JsonNodeFactory.instance.missingNode(),
        EvaluateBindings.newBuilder().put("Duration", luxonDuration).build());
    assertEquals("P5M10D", evalResult.textValue());
  }

  private static Scriptable getLuxonDuration() {
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
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    } finally {
      Context.exit();
    }
    return jsonataFriendlyLuxonDuration;
  }

}
