package com.saasquatch.rhinojsonata;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.time.Duration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

public class JSONataExpressionOptionsTests {

  private static JSONata jsonata;

  @BeforeAll
  public static void beforeAll() {
    jsonata = JSONata.create();
  }

  @Test
  public void testValidation() {
    final JSONataExpressionOptions.Builder builder = JSONataExpressionOptions.newBuilder();
    //noinspection ConstantConditions
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

  @SuppressWarnings("ConstantConditions")
  @Test
  public void testTimeboxValidation() {
    final JSONataExpressionOptions.Builder builder = JSONataExpressionOptions.newBuilder();
    assertThrows(NullPointerException.class, () -> builder.timeboxExpression(null, 1000));
    assertThrows(IllegalArgumentException.class,
        () -> builder.timeboxExpression(Duration.ofSeconds(-1), 1000));
    assertThrows(IllegalArgumentException.class,
        () -> builder.timeboxExpression(Duration.ofSeconds(1), -1000));
    assertDoesNotThrow(() -> builder.timeboxExpression(Duration.ofSeconds(1), 1000));
  }

  @Test
  public void testTimeboxTimeout() {
    final JSONataExpression expression = jsonata.parse("($f1 := function($x) { $f1($x) }; $f1(1))",
        JSONataExpressionOptions.newBuilder()
            .timeboxExpression(Duration.ofMillis(500), Integer.MAX_VALUE).build());
    try {
      expression.evaluate();
      fail();
    } catch (JSONataException e) {
      assertTrue(e.getMessage().contains("timeout"));
    }
  }

  @Test
  public void testTimeboxMaxDepth() {
    {
      final JSONataExpression expression = jsonata.parse(
          "($f1 := function($x) { $f1($x) + $f1($x) }; $f1(1))");
      assertThrows(JSONataException.class, expression::evaluate);
    }
    {
      final JSONataExpression expression = jsonata.parse(
          "($f1 := function($x) { $f1($x) + $f1($x) }; $f1(1))",
          JSONataExpressionOptions.newBuilder()
              .timeboxExpression(Duration.ofDays(1), 10).build());
      try {
        expression.evaluate();
        fail();
      } catch (JSONataException e) {
        assertTrue(e.getMessage().contains("Stack overflow"));
      }
    }
  }

  @Test
  public void testCustomScope() {
    final Context cx = SquatchContextFactory.INSTANCE.enterContext();
    try {
      final Scriptable sharedScope = cx.initStandardObjects();
      ScriptableObject.putProperty(sharedScope, "foo", 123);
      final JSONataExpression ex1 = jsonata.parse("$f1()",
          JSONataExpressionOptions.newBuilder().setScope(sharedScope).build());
      ex1.registerFunction("f1", "() => ++foo", null);
      final JSONataExpression ex2 = jsonata.parse("$f1()",
          JSONataExpressionOptions.newBuilder().setScope(sharedScope).build());
      ex2.registerFunction("f1", "() => ++foo", null);
      assertEquals(124, ex1.evaluate().intValue());
      assertEquals(125, ex2.evaluate().intValue());
      assertEquals(126, ex1.evaluate().intValue());
    } finally {
      Context.exit();
    }
  }

  @Test
  public void testCustomScopeWithJavaAccess() {
    final Context cx = SquatchContextFactory.INSTANCE.enterContext();
    try {
      final Scriptable sharedScope = cx.initStandardObjects();
      final JSONataExpression ex1 = jsonata.parse("$foo");
      assertThrows(JSONataException.class, () -> ex1.assign("foo", "java.lang.Short.MAX_VALUE"));
      final JSONataExpression ex2 = jsonata.parse("$foo",
          JSONataExpressionOptions.newBuilder().setScope(sharedScope).build());
      ex2.assign("foo", "java.lang.Short.MAX_VALUE");
      assertEquals(Short.MAX_VALUE, ex2.evaluate().intValue());
    } finally {
      Context.exit();
    }
  }

}
