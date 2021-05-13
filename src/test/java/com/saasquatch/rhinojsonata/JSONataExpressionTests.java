package com.saasquatch.rhinojsonata;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
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

}
