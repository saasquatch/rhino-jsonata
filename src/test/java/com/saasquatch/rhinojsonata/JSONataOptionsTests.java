package com.saasquatch.rhinojsonata;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

@SuppressWarnings("ConstantConditions")
public class JSONataOptionsTests {

  @Test
  public void testValidation() {
    final JSONataOptions.Builder builder = JSONataOptions.newBuilder();
    assertThrows(NullPointerException.class, () -> builder.setObjectMapper(null));
    assertThrows(NullPointerException.class, () -> builder.setJSONataJsSource(null));
  }

}
