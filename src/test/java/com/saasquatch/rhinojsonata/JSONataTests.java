package com.saasquatch.rhinojsonata;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory;

public class JSONataTests {

  private static JSONata jsonata;

  @BeforeAll
  public static void beforeAll() {
    jsonata = JSONata.create();
  }

  @Test
  public void testJSONataTimeboxExpressionFunctionLazyInit() {
    assertSame(jsonata.getTimeboxExpressionFunction(), jsonata.getTimeboxExpressionFunction());
  }

  @SuppressWarnings("ConstantConditions")
  @Test
  public void testParseExceptionHandling() {
    assertThrows(NullPointerException.class, () -> jsonata.parse(null));
    assertThrows(JSONataException.class, () -> jsonata.parse("\t"));
  }

  @Test
  public void testJavaClassesInaccessible() {
    final JSONataExpression expression = jsonata.parse("$foo()");
    expression.assign("foo", "() => { java.lang.System.out.println(\"HELLO\"); }");
    try {
      expression.evaluate();
      fail();
    } catch (JSONataException e) {
      assertTrue(e.getMessage().contains("\"java\" is not defined"));
    }
  }

  @Test
  public void testRejectingUnexpectedContextType() {
    @SuppressWarnings("unused") final Context cx = new ContextFactory().enterContext();
    try {
      try {
        jsonata.parse("$foo");
        fail();
      } catch (JSONataException e) {
        assertTrue(e.getMessage().contains("Context type"));
      }
    } finally {
      Context.exit();
    }
  }

}
