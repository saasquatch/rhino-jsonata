package com.saasquatch.rhinojsonata;

import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

public class JSONataTests {

  @Test
  public void testJSONataTimeboxExpressionFunctionLazyInit() {
    final JSONata jsonata = JSONata.create();
    assertSame(jsonata.getTimeboxExpressionFunction(), jsonata.getTimeboxExpressionFunction());
  }

}
