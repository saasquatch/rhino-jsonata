package com.saasquatch.rhinojsonata;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import java.nio.ByteBuffer;
import java.util.function.UnaryOperator;
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
  public void testAssignJsExpression() {
    {
      final JSONataExpression expression = jsonata.parse("$foo");
      assertEquals(JsonNodeFactory.instance.missingNode(), expression.evaluate(null));
    }
    {
      final JSONataExpression expression = jsonata.parse("$foo");
      expression.assignJsExpression("foo", "1");
      assertEquals(JsonNodeFactory.instance.numberNode(1.0), expression.evaluate(null));
    }
    {
      final JSONataExpression expression = jsonata.parse("$foo");
      expression.assignJsExpression("foo", "'1'");
      assertEquals(JsonNodeFactory.instance.textNode("1"), expression.evaluate(null));
    }
    {
      final JSONataExpression expression = jsonata.parse("$foo");
      expression.assignJsExpression("foo", "a => a + a");
      assertThrows(JSONataException.class, () -> expression.evaluate(null));
    }
    {
      final JSONataExpression expression = jsonata.parse("$foo(1)");
      expression.assignJsExpression("foo", "a => a + a");
      assertEquals(JsonNodeFactory.instance.numberNode(2.0), expression.evaluate(null));
    }
    {
      final JSONataExpression expression = jsonata.parse("$foo('1')");
      expression.assignJsExpression("foo", "a => a + a");
      assertEquals(JsonNodeFactory.instance.textNode("11"), expression.evaluate(null));
    }
  }

  @Test
  public void testAssignJavaObject() {
    {
      final JSONataExpression expression = jsonata.parse("$foo");
      assertEquals(JsonNodeFactory.instance.missingNode(), expression.evaluate(null));
    }
    {
      final JSONataExpression expression = jsonata.parse("$foo");
      expression.assignJavaObject("foo", 1);
      assertEquals(JsonNodeFactory.instance.numberNode(1), expression.evaluate(null));
    }
    {
      final JSONataExpression expression = jsonata.parse("$foo");
      expression.assignJavaObject("foo", "1");
      assertEquals(JsonNodeFactory.instance.textNode("1"), expression.evaluate(null));
    }
    {
      final JSONataExpression expression = jsonata.parse("$foo");
      expression.assignJavaObject("foo", ByteBuffer.allocate(1));
      assertThrows(JSONataException.class, () -> expression.evaluate(null));
    }
    {
      final JSONataExpression expression = jsonata.parse("$foo");
      expression.assignJavaObject("foo", (UnaryOperator<String>) s -> s + s);
      assertThrows(JSONataException.class, () -> expression.evaluate(null));
    }
  }

  @Test
  public void testRegisterJsFunction() {
    {
      final JSONataExpression expression = jsonata.parse("$foo(1)");
      expression.registerJsFunction("foo", "function(a) {return a + a}", "<n:n>");
      assertEquals(JsonNodeFactory.instance.numberNode(2.0), expression.evaluate(null));
    }
    {
      final JSONataExpression expression = jsonata.parse("$foo(1)");
      expression.registerJsFunction("foo", "function(a) {return a + a}", null);
      assertEquals(JsonNodeFactory.instance.numberNode(2.0), expression.evaluate(null));
    }
    {
      final JSONataExpression expression = jsonata.parse("$foo(1)");
      expression.registerJsFunction("foo", "function(a) {return a + a}", "<s:s>");
      assertThrows(JSONataException.class, () -> expression.evaluate(null));
    }
  }

  @Test
  public void testRegisterJsArrowFunction() {
    {
      final JSONataExpression expression = jsonata.parse("$foo(1)");
      expression.registerJsArrowFunction("foo", "a => a + a", "<n:n>");
      assertEquals(JsonNodeFactory.instance.numberNode(2.0), expression.evaluate(null));
    }
    {
      final JSONataExpression expression = jsonata.parse("$foo(1)");
      expression.registerJsArrowFunction("foo", "a => a + a", null);
      assertEquals(JsonNodeFactory.instance.numberNode(2.0), expression.evaluate(null));
    }
    {
      final JSONataExpression expression = jsonata.parse("$foo(1)");
      expression.registerJsArrowFunction("foo", "a => a + a", "<s:s>");
      assertThrows(JSONataException.class, () -> expression.evaluate(null));
    }
  }

}
