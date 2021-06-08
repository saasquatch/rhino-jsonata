package com.saasquatch.rhinojsonata;

import static com.saasquatch.rhinojsonata.TestJSONataHolder.jsonata;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Test;

public class JSONataTests {

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

}
