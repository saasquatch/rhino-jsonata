package com.saasquatch.rhinojsonata;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Duration;
import org.junit.jupiter.api.Test;

public class JSONataExpressionOptionsTests {

  @SuppressWarnings("ConstantConditions")
  @Test
  public void testValidation() {
    final JSONataExpressionOptions.Builder builder = JSONataExpressionOptions.newBuilder();
    assertThrows(NullPointerException.class, () -> builder.setEvaluateTimeout(null));
    assertThrows(IllegalArgumentException.class, () -> builder.setEvaluateTimeout(Duration.ZERO));
    assertThrows(IllegalArgumentException.class,
        () -> builder.setEvaluateTimeout(Duration.ofSeconds(-1)));
    assertDoesNotThrow(() -> builder.setEvaluateTimeout(Duration.ofNanos(1)));
  }

}
