package com.saasquatch.rhinojsonata.examples;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.saasquatch.rhinojsonata.JSONata;
import com.saasquatch.rhinojsonata.JSONataExpression;

public class Example1 {

  public static void main(String[] args) throws Exception {
    final ObjectMapper objectMapper = new ObjectMapper();
    final JSONata jsonata = JSONata.create();
    final JsonNode data = objectMapper.readTree(
        "{\"example\":[{\"value\":4},{\"value\":7},{\"value\":13}]}");
    final JSONataExpression expression = jsonata.parse("$sum(example.value)");
    final JsonNode result = expression.evaluate(data);
    System.out.println(result); // prints 24
  }

}
