package com.saasquatch.rhinojsonata;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

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

}
