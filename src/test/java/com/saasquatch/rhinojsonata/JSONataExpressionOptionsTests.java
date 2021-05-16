package com.saasquatch.rhinojsonata;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

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

  @Test
  public void testEvaluateTimeout() {
    final JSONata jsonata = JSONata.create();
    final JSONataExpression expression = jsonata.parse("($f1 := function($x) { $f1($x) }; $f1(1))",
        JSONataExpressionOptions.newBuilder().setEvaluateTimeout(Duration.ofMillis(500)).build());
    try {
      expression.evaluate();
      fail();
    } catch (JSONataException e) {
      assertTrue(e.getMessage().contains("timeout"));
    }
  }

}
