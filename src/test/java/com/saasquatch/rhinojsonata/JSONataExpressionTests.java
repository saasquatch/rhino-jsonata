package com.saasquatch.rhinojsonata;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import java.time.Duration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class JSONataExpressionTests {

  private static JSONata jsonata;

  @BeforeAll
  public static void beforeAll() {
    jsonata = JSONata.create();
  }

  @Test
  public void testParse() {
    try {
      jsonata.parse("");
      fail();
    } catch (JSONataException e) {
      assertTrue(e.getMessage().contains("Unexpected end of expression"));
    }
    try {
      jsonata.parse("(");
      fail();
    } catch (JSONataException e) {
      assertTrue(e.getMessage().contains("before end of expression"));
    }
    assertDoesNotThrow(() -> jsonata.parse("$foo"));
  }

  @Test
  public void testEvaluateBasic() {
    assertEquals(JsonNodeFactory.instance.missingNode(),
        jsonata.parse("foo").evaluate());
    assertEquals(JsonNodeFactory.instance.missingNode(),
        jsonata.parse("foo").evaluate(JsonNodeFactory.instance.textNode("foo")));
    assertEquals(JsonNodeFactory.instance.nullNode(),
        jsonata.parse("null").evaluate(JsonNodeFactory.instance.textNode("foo")));
    assertEquals(JsonNodeFactory.instance.objectNode(),
        jsonata.parse("{\"foo\":bar}").evaluate());
  }

  @Test
  public void testEvaluationOfDifferentTypes() {
    assertEquals(JsonNodeFactory.instance.missingNode(), jsonata.parse("$$").evaluate());
    assertEquals(JsonNodeFactory.instance.nullNode(), jsonata.parse("$$").evaluate(null));
    assertEquals(JsonNodeFactory.instance.missingNode(),
        jsonata.parse("$$").evaluate(JsonNodeFactory.instance.missingNode()));
    assertEquals(JsonNodeFactory.instance.nullNode(),
        jsonata.parse("$$").evaluate(JsonNodeFactory.instance.nullNode()));
    assertEquals(JsonNodeFactory.instance.booleanNode(true),
        jsonata.parse("$$").evaluate(JsonNodeFactory.instance.booleanNode(true)));
    assertEquals(JsonNodeFactory.instance.textNode("lol"),
        jsonata.parse("$$").evaluate(JsonNodeFactory.instance.textNode("lol")));
    assertEquals(JsonNodeFactory.instance.numberNode(1),
        jsonata.parse("$$").evaluate(JsonNodeFactory.instance.numberNode(1)));
    assertEquals(JsonNodeFactory.instance.nullNode(),
        jsonata.parse("foo").evaluate(JsonNodeFactory.instance.objectNode()
            .put("foo", (String) null)));
    assertEquals(JsonNodeFactory.instance.missingNode(),
        jsonata.parse("foo").evaluate(JsonNodeFactory.instance.objectNode()
            .put("bar", (String) null)));
  }

  @Test
  public void testAssignJsExpression() {
    {
      final JSONataExpression expression = jsonata.parse("$foo");
      assertEquals(JsonNodeFactory.instance.missingNode(), expression.evaluate(null));
    }
    {
      final JSONataExpression expression = jsonata.parse("$foo");
      expression.assign("foo", "1");
      assertEquals(JsonNodeFactory.instance.numberNode(1), expression.evaluate(null));
    }
    {
      final JSONataExpression expression = jsonata.parse("$foo");
      expression.assign("foo", "'1'");
      assertEquals(JsonNodeFactory.instance.textNode("1"), expression.evaluate(null));
    }
    {
      final JSONataExpression expression = jsonata.parse("$foo");
      expression.assign("foo", "a => a + a");
      assertThrows(JSONataException.class, () -> expression.evaluate(null));
    }
    {
      final JSONataExpression expression = jsonata.parse("$foo(1)");
      expression.assign("foo", "a => a + a");
      assertEquals(JsonNodeFactory.instance.numberNode(2), expression.evaluate(null));
    }
    {
      final JSONataExpression expression = jsonata.parse("$foo('1')");
      expression.assign("foo", "a => a + a");
      assertEquals(JsonNodeFactory.instance.textNode("11"), expression.evaluate(null));
    }
  }

  @Test
  public void testAssignJsonNode() {
    {
      final JSONataExpression expression = jsonata.parse("$foo");
      assertEquals(JsonNodeFactory.instance.missingNode(), expression.evaluate(null));
    }
    {
      final JSONataExpression expression = jsonata.parse("$foo");
      expression.assign("foo", JsonNodeFactory.instance.numberNode(1));
      assertEquals(JsonNodeFactory.instance.numberNode(1), expression.evaluate(null));
    }
    {
      final JSONataExpression expression = jsonata.parse("$foo");
      expression.assign("foo", JsonNodeFactory.instance.textNode("1"));
      assertEquals(JsonNodeFactory.instance.textNode("1"), expression.evaluate(null));
    }
  }

  @Test
  public void testRegisterFunction() {
    {
      final JSONataExpression expression = jsonata.parse("$foo(1)");
      expression.registerFunction("foo", "a => a + a", "<n:n>");
      assertEquals(JsonNodeFactory.instance.numberNode(2), expression.evaluate(null));
    }
    {
      final JSONataExpression expression = jsonata.parse("$foo(1)");
      expression.registerFunction("foo", "(function(a) {return a + a;})", "<n:n>");
      assertEquals(JsonNodeFactory.instance.numberNode(2), expression.evaluate(null));
    }
    {
      final JSONataExpression expression = jsonata.parse("$foo(1)");
      expression.registerFunction("foo", "function(a) {return a + a;}", "<n:n>");
      assertThrows(JSONataException.class, () -> expression.evaluate(null));
    }
    {
      final JSONataExpression expression = jsonata.parse("$foo(1)");
      expression.registerFunction("foo", "a => a + a", null);
      assertEquals(JsonNodeFactory.instance.numberNode(2), expression.evaluate(null));
    }
    {
      final JSONataExpression expression = jsonata.parse("$foo(1)");
      expression.registerFunction("foo", "(function(a) {return a + a;})", null);
      assertEquals(JsonNodeFactory.instance.numberNode(2), expression.evaluate(null));
    }
    {
      final JSONataExpression expression = jsonata.parse("$foo(\"1\")");
      expression.registerFunction("foo", "a => a + a", "<s:s>");
      assertEquals(JsonNodeFactory.instance.textNode("11"), expression.evaluate(null));
    }
    {
      final JSONataExpression expression = jsonata.parse("$foo(1)");
      expression.registerFunction("foo", "a => a + a", "<s:s>");
      assertThrows(JSONataException.class, () -> expression.evaluate(null));
    }
  }

  @SuppressWarnings("ConstantConditions")
  @Test
  public void testTimeboxValidation() {
    final JSONataExpression expression = jsonata.parse("1");
    assertThrows(NullPointerException.class, () -> expression.timeboxExpression(null, 1000));
    assertThrows(IllegalArgumentException.class,
        () -> expression.timeboxExpression(Duration.ofSeconds(-1), 1000));
    assertThrows(IllegalArgumentException.class,
        () -> expression.timeboxExpression(Duration.ofSeconds(1), -1000));
    assertDoesNotThrow(() -> expression.timeboxExpression(Duration.ofSeconds(1), 1000));
  }

  @Test
  public void testTimeboxTimeout() {
    final JSONataExpression expression = jsonata.parse("($f1 := function($x) { $f1($x) }; $f1(1))");
    expression.timeboxExpression(Duration.ofMillis(500), Integer.MAX_VALUE);
    try {
      expression.evaluate();
      fail();
    } catch (JSONataException e) {
      assertTrue(e.getMessage().contains("timeout"));
    }
  }

  @Test
  public void testTimeboxMaxDepth() {
    final JSONataExpression expression = jsonata.parse(
        "($f1 := function($x) { $f1($x) + $f1($x) }; $f1(1))");
    expression.timeboxExpression(Duration.ofDays(1), 10);
    try {
      expression.evaluate();
      fail();
    } catch (JSONataException e) {
      assertTrue(e.getMessage().contains("Stack overflow"));
    }
  }

}
