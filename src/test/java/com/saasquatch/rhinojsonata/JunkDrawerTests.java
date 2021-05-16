package com.saasquatch.rhinojsonata;

import static com.saasquatch.rhinojsonata.JunkDrawer.getDefaultJSONataSource;
import static com.saasquatch.rhinojsonata.JunkDrawer.readToString;
import static com.saasquatch.rhinojsonata.JunkDrawer.rethrowRhinoException;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.ByteArrayInputStream;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.RhinoException;
import org.mozilla.javascript.Scriptable;

public class JunkDrawerTests {

  @Test
  public void testRethrowRhinoException() {
    final Context cx = Context.enter();
    try {
      final Scriptable scope = cx.initSafeStandardObjects();
      try {
        cx.evaluateString(scope, "throw {foo:true}", null, 1, null);
        fail();
      } catch (RhinoException e) {
        try {
          rethrowRhinoException(cx, scope, e);
        } catch (JSONataException e2) {
          assertEquals("{\"foo\":true}", e2.getMessage());
        }
      }
      try {
        cx.evaluateString(scope, "throw 'foo'", null, 1, null);
        fail();
      } catch (RhinoException e) {
        try {
          rethrowRhinoException(cx, scope, e);
        } catch (JSONataException e2) {
          assertEquals("foo", e2.getMessage());
        }
      }
      try {
        cx.evaluateString(scope, "throw new Error('foo')", null, 1, null);
        fail();
      } catch (RhinoException e) {
        try {
          rethrowRhinoException(cx, scope, e);
        } catch (JSONataException e2) {
          assertTrue(e2.getMessage().startsWith("Error: foo"));
        }
      }
      try {
        cx.evaluateString(scope, "throw 1", null, 1, null);
        fail();
      } catch (RhinoException e) {
        try {
          rethrowRhinoException(cx, scope, e);
        } catch (JSONataException e2) {
          assertEquals("1", e2.getMessage());
        }
      }
      try {
        cx.evaluateString(scope, "(", null, 1, null);
        fail();
      } catch (RhinoException e) {
        try {
          rethrowRhinoException(cx, scope, e);
        } catch (JSONataException e2) {
          assertTrue(e2.getMessage().toLowerCase(Locale.ROOT).contains("unexpected"));
        }
      }
      try {
        cx.evaluateString(scope, "foo.bar", null, 1, null);
        fail();
      } catch (RhinoException e) {
        try {
          rethrowRhinoException(cx, scope, e);
        } catch (JSONataException e2) {
          assertTrue(e2.getMessage().toLowerCase(Locale.ROOT).contains("not defined"));
        }
      }
    } finally {
      Context.exit();
    }
  }

  @Test
  public void testReadToString() throws Exception {
    final byte[] bytes = new byte[100_000];
    ThreadLocalRandom.current().nextBytes(bytes);
    final String readerToString = readToString(new ByteArrayInputStream(bytes), UTF_8);
    assertEquals(new String(bytes, UTF_8), readerToString);
  }

  @Test
  public void testLoadJSONataSource() {
    final Set<String> sources = IntStream.range(0, 100)
        .parallel()
        .mapToObj(i -> getDefaultJSONataSource())
        .collect(Collectors.toSet());
    assertEquals(1, sources.size());
    assertFalse(sources.iterator().next().isEmpty());
  }

}
