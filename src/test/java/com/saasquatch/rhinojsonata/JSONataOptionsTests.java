package com.saasquatch.rhinojsonata;

import static com.saasquatch.rhinojsonata.JunkDrawer.readerToString;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import org.junit.jupiter.api.Test;

public class JSONataOptionsTests {

  @SuppressWarnings("ConstantConditions")
  @Test
  public void testValidation() {
    final JSONataOptions.Builder builder = JSONataOptions.newBuilder();
    assertThrows(NullPointerException.class, () -> builder.setObjectMapper(null));
    assertThrows(NullPointerException.class, () -> builder.setJSONataJsSource(null));
  }

  @Test
  public void testCustomObjectMapper() {
    final JSONata jsonata = JSONata.create(JSONataOptions.newBuilder()
        .setObjectMapper(new ObjectMapper())
        .build());
    assertEquals(2, jsonata.parse("1 + 1").evaluate().intValue());
  }

  @Test
  public void testCustomJsSource() throws Exception {
    final String sourceString;
    try (
        InputStream sourceStream = new URL(
            "https://cdn.jsdelivr.net/npm/jsonata@1.8.3/jsonata-es5.min.js").openStream();
        Reader sourceReader = new InputStreamReader(sourceStream, UTF_8)) {
      sourceString = readerToString(sourceReader, 1 << 16);
    }
    final JSONata jsonata = JSONata.create(JSONataOptions.newBuilder()
        .setJSONataJsSource(sourceString)
        .build());
    assertEquals(2, jsonata.parse("1 + 1").evaluate().intValue());
  }

  @Test
  public void testInvalidJsSource() {
    assertThrows(JSONataException.class, () -> JSONata.create(JSONataOptions.newBuilder()
        .setJSONataJsSource("I am invalid")
        .build()));
  }

}
