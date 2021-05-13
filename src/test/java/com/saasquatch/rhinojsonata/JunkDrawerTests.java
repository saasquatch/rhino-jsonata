package com.saasquatch.rhinojsonata;

import static com.saasquatch.rhinojsonata.JunkDrawer.getDefaultJSONataSource;
import static com.saasquatch.rhinojsonata.JunkDrawer.readerToString;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

public class JunkDrawerTests {

//  @Test
//  public void testRethrowPolyglotException() {
//    try {
//      context.eval(JS, "throw {foo:true}");
//    } catch (PolyglotException e) {
//      assertThrows(JSONataException.class, () -> rethrowPolyglotException(context, e));
//      try {
//        rethrowPolyglotException(context, e);
//      } catch (JSONataException e2) {
//        assertEquals("{\"foo\":true}", e2.getMessage());
//      }
//    }
//    try {
//      context.eval(JS, "throw \"foo\"");
//    } catch (PolyglotException e) {
//      assertThrows(JSONataException.class, () -> rethrowPolyglotException(context, e));
//      try {
//        rethrowPolyglotException(context, e);
//      } catch (JSONataException e2) {
//        assertEquals("foo", e2.getMessage());
//      }
//    }
//    try {
//      context.eval(JS, "throw 1");
//    } catch (PolyglotException e) {
//      assertThrows(JSONataException.class, () -> rethrowPolyglotException(context, e));
//      try {
//        rethrowPolyglotException(context, e);
//      } catch (JSONataException e2) {
//        assertEquals("1", e2.getMessage());
//      }
//    }
//  }

  @Test
  public void testReaderToString() throws Exception {
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
        .mapToObj(i -> getDefaultJSONataSource())
        .collect(Collectors.toSet());
    assertEquals(1, sources.size());
    assertFalse(sources.iterator().next().isEmpty());
  }

  @Test
  public void testLoadJSONataSourceCaching() {
    assertSame(getDefaultJSONataSource(), getDefaultJSONataSource());
  }

}
