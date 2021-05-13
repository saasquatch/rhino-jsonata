package com.saasquatch.rhinojsonata;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Duration;
import org.junit.jupiter.api.Test;

@SuppressWarnings("ConstantConditions")
public class JSONataOptionsTests {

  @Test
  public void testValidation() {
    final JSONataOptions.Builder builder = JSONataOptions.newBuilder();
    assertThrows(NullPointerException.class, () -> builder.setObjectMapper(null));
    assertThrows(NullPointerException.class, () -> builder.setJSONataJsSource(null));
    assertThrows(NullPointerException.class, () -> builder.timeboxExpression(null, 1));
    assertThrows(IllegalArgumentException.class,
        () -> builder.timeboxExpression(Duration.ofSeconds(-1), 1));
    assertThrows(IllegalArgumentException.class,
        () -> builder.timeboxExpression(Duration.ofSeconds(1), -1));
  }

}
