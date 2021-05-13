package com.saasquatch.rhinojsonata;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;

public class JSONataExpressionTests {

  @Test
  public void testParse() {
    try {
      JSONataExpression.parse("");
      fail();
    } catch (JSONataException e) {
      assertTrue(e.getMessage().contains("Unexpected end of expression"));
    }
    try {
      JSONataExpression.parse("(");
      fail();
    } catch (JSONataException e) {
      assertTrue(e.getMessage().contains("before end of expression"));
    }
    assertDoesNotThrow(() -> JSONataExpression.parse("$foo"));
  }

  @Test
  public void testEvaluateBasic() {
    assertEquals(JsonNodeFactory.instance.missingNode(),
        JSONataExpression.parse("foo").evaluate(null));
    assertEquals(JsonNodeFactory.instance.missingNode(),
        JSONataExpression.parse("foo").evaluate(JsonNodeFactory.instance.textNode("foo")));
    assertEquals(JsonNodeFactory.instance.nullNode(),
        JSONataExpression.parse("null").evaluate(JsonNodeFactory.instance.textNode("foo")));
    assertEquals(JsonNodeFactory.instance.objectNode(),
        JSONataExpression.parse("{\"foo\":bar}").evaluate(null));
  }

  @Test
  public void testAssignJsExpression() {
    final Supplier<JSONataExpression> supplier = () -> JSONataExpression.parse("$foo");
    {
      final JSONataExpression expression = supplier.get();
      assertEquals(JsonNodeFactory.instance.missingNode(), expression.evaluate(null));
    }
    {
      final JSONataExpression expression = supplier.get();
      expression.assignJsExpression("foo", "1");
      assertEquals(JsonNodeFactory.instance.numberNode(1.0), expression.evaluate(null));
    }
    {
      final JSONataExpression expression = supplier.get();
      expression.assignJsExpression("foo", "'1'");
      assertEquals(JsonNodeFactory.instance.textNode("1"), expression.evaluate(null));
    }
    {
      final JSONataExpression expression = supplier.get();
      expression.assignJsExpression("foo", "a => a + a");
      assertThrows(JSONataException.class, () -> expression.evaluate(null));
    }
  }

}
