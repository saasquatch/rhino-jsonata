package com.saasquatch.rhinojsonata;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

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

}
