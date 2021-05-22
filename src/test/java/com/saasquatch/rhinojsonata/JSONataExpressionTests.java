package com.saasquatch.rhinojsonata;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.google.common.collect.ImmutableMap;
import java.util.HashMap;
import java.util.Map;
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
  public void testEvaluateWithBindings() {
    final JSONataExpression expression = jsonata.parse("$foo");
    //noinspection ConstantConditions
    assertThrows(NullPointerException.class, () -> expression.evaluate(null, null));
    assertThrows(NullPointerException.class,
        () -> {
          final Map<String, Object> bindings = new HashMap<>();
          bindings.put("foo", null);
          expression.evaluate(null, bindings);
        });
    assertThrows(JSONataException.class,
        () -> expression.evaluate(null, ImmutableMap.of("foo", 1)));
    assertEquals(JsonNodeFactory.instance.missingNode(),
        expression.evaluate(null, ImmutableMap.of()));
    assertEquals(JsonNodeFactory.instance.numberNode(1),
        expression.evaluate(null, ImmutableMap.of("foo", "1")));
    assertEquals(JsonNodeFactory.instance.numberNode(1), expression
        .evaluate(null, ImmutableMap.of("foo", JsonNodeFactory.instance.numberNode(1))));
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
      expression.assign("foo", "null");
      assertEquals(JsonNodeFactory.instance.nullNode(), expression.evaluate(null));
    }
    {
      final JSONataExpression expression = jsonata.parse("$foo");
      expression.assign("foo", "undefined");
      assertEquals(JsonNodeFactory.instance.missingNode(), expression.evaluate(null));
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
    {
      final JSONataExpression expression = jsonata.parse("$foo");
      expression.assign("foo", JsonNodeFactory.instance.nullNode());
      assertEquals(JsonNodeFactory.instance.nullNode(), expression.evaluate(null));
    }
    {
      final JSONataExpression expression = jsonata.parse("$foo");
      expression.assign("foo", (JsonNode) null);
      assertEquals(JsonNodeFactory.instance.nullNode(), expression.evaluate(null));
    }
    {
      final JSONataExpression expression = jsonata.parse("$foo");
      expression.assign("foo", JsonNodeFactory.instance.missingNode());
      assertEquals(JsonNodeFactory.instance.missingNode(), expression.evaluate(null));
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

}
