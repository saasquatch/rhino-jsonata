package com.saasquatch.rhinojsonata;

import static com.saasquatch.rhinojsonata.JunkDrawer.loadDefaultJSONataSource;
import static com.saasquatch.rhinojsonata.JunkDrawer.readerToString;
import static com.saasquatch.rhinojsonata.JunkDrawer.rethrowRhinoException;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.RhinoException;
import org.mozilla.javascript.Scriptable;

public class JunkDrawerTests {

  @Test
  public void testRethrowRhinoException() {
    final ObjectMapper objectMapper = new ObjectMapper();
    final Context cx = new ContextFactory().enterContext();
    try {
      final Scriptable scope = cx.initSafeStandardObjects();
      try {
        cx.evaluateString(scope, "throw {foo:true}", null, 1, null);
        fail();
      } catch (RhinoException e) {
        try {
          rethrowRhinoException(cx, scope, objectMapper, e);
        } catch (JSONataException e2) {
          assertEquals("{\"foo\":true}", e2.getMessage());
          assertEquals(JsonNodeFactory.instance.objectNode().put("foo", true), e2.getErrorJson());
        }
      }
      try {
        cx.evaluateString(scope, "throw 'foo'", null, 1, null);
        fail();
      } catch (RhinoException e) {
        try {
          rethrowRhinoException(cx, scope, objectMapper, e);
        } catch (JSONataException e2) {
          assertEquals("foo", e2.getMessage());
          assertEquals(JsonNodeFactory.instance.textNode("foo"), e2.getErrorJson());
        }
      }
      try {
        cx.evaluateString(scope, "throw new Error('foo')", null, 1, null);
        fail();
      } catch (RhinoException e) {
        try {
          rethrowRhinoException(cx, scope, objectMapper, e);
        } catch (JSONataException e2) {
          assertEquals("Error: foo", e2.getMessage());
          assertEquals(JsonNodeFactory.instance.objectNode().put("message", "foo"),
              e2.getErrorJson());
        }
      }
      try {
        cx.evaluateString(scope, "throw false", null, 1, null);
        fail();
      } catch (RhinoException e) {
        try {
          rethrowRhinoException(cx, scope, objectMapper, e);
        } catch (JSONataException e2) {
          assertEquals("false", e2.getMessage());
          assertEquals(JsonNodeFactory.instance.booleanNode(false), e2.getErrorJson());
        }
      }
      try {
        cx.evaluateString(scope, "throw [1]", null, 1, null);
        fail();
      } catch (RhinoException e) {
        try {
          rethrowRhinoException(cx, scope, objectMapper, e);
        } catch (JSONataException e2) {
          assertEquals("[1]", e2.getMessage());
          assertEquals("[1]", Objects.requireNonNull(e2.getErrorJson()).toString());
        }
      }
      try {
        cx.evaluateString(scope, "throw function() {}", null, 1, null);
        fail();
      } catch (RhinoException e) {
        try {
          rethrowRhinoException(cx, scope, objectMapper, e);
        } catch (JSONataException e2) {
          assertEquals("undefined", e2.getMessage());
          assertNull(e2.getErrorJson());
        }
      }
      try {
        cx.evaluateString(scope, "throw a => {}", null, 1, null);
        fail();
      } catch (RhinoException e) {
        try {
          rethrowRhinoException(cx, scope, objectMapper, e);
        } catch (JSONataException e2) {
          assertEquals("undefined", e2.getMessage());
          assertNull(e2.getErrorJson());
        }
      }
      try {
        cx.evaluateString(scope, "throw null", null, 1, null);
        fail();
      } catch (RhinoException e) {
        try {
          rethrowRhinoException(cx, scope, objectMapper, e);
        } catch (JSONataException e2) {
          assertEquals("null", e2.getMessage());
          assertNull(e2.getErrorJson());
        }
      }
      try {
        cx.evaluateString(scope, "throw undefined", null, 1, null);
        fail();
      } catch (RhinoException e) {
        try {
          rethrowRhinoException(cx, scope, objectMapper, e);
        } catch (JSONataException e2) {
          assertEquals("undefined", e2.getMessage());
          assertNull(e2.getErrorJson());
        }
      }
      try {
        cx.evaluateString(scope, "(", null, 1, null);
        fail();
      } catch (RhinoException e) {
        try {
          rethrowRhinoException(cx, scope, objectMapper, e);
        } catch (JSONataException e2) {
          assertTrue(e2.getMessage().toLowerCase(Locale.ROOT).contains("unexpected"));
          assertNull(e2.getErrorJson());
        }
      }
      try {
        cx.evaluateString(scope, "foo.bar", null, 1, null);
        fail();
      } catch (RhinoException e) {
        try {
          rethrowRhinoException(cx, scope, objectMapper, e);
        } catch (JSONataException e2) {
          assertTrue(e2.getMessage().toLowerCase(Locale.ROOT).contains("not defined"));
          assertNull(e2.getErrorJson());
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
    final String readerToString = readerToString(
        new InputStreamReader(new ByteArrayInputStream(bytes), UTF_8));
    assertEquals(new String(bytes, UTF_8), readerToString);
  }

  @Test
  public void testLoadJSONataSource() {
    final Set<String> sources = IntStream.range(0, 100)
        .parallel()
        .mapToObj(i -> loadDefaultJSONataSource())
        .collect(Collectors.toSet());
    assertEquals(1, sources.size());
    assertFalse(sources.iterator().next().isEmpty());
  }

}
