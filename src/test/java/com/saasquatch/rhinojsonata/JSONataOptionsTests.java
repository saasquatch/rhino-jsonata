package com.saasquatch.rhinojsonata;

import static com.saasquatch.rhinojsonata.JunkDrawer.readToString;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;
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
    try (InputStream sourceStream = new URL(
        "https://cdn.jsdelivr.net/npm/jsonata@1.8.3/jsonata-es5.min.js").openStream()) {
      sourceString = readToString(sourceStream, UTF_8);
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
