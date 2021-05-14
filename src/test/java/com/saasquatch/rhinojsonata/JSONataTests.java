package com.saasquatch.rhinojsonata;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Test;

public class JSONataTests {

  @Test
  public void testJSONataTimeboxExpressionFunctionLazyInit() {
    final JSONata jsonata = JSONata.create();
    assertSame(jsonata.getTimeboxExpressionFunction(), jsonata.getTimeboxExpressionFunction());
  }

  @SuppressWarnings("ConstantConditions")
  @Test
  public void testParseExceptionHandling() {
    final JSONata jsonata = JSONata.create();
    assertThrows(NullPointerException.class, () -> jsonata.parse(null));
    assertThrows(JSONataException.class, () -> jsonata.parse("\t"));
  }

  @Test
  public void testJavaClassesInaccessible() {
    final JSONata jsonata = JSONata.create();
    final JSONataExpression expression = jsonata.parse("$foo()");
    expression.assign("foo", "() => { java.lang.System.out.println(\"HELLO\"); }");
    try {
      expression.evaluate();
      fail();
    } catch (JSONataException e) {
      assertTrue(e.getMessage().contains("\"java\" is not defined"));
    }
  }

}
