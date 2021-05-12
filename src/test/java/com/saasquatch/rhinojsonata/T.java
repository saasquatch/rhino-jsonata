package com.saasquatch.rhinojsonata;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import java.util.function.Function;
import org.junit.jupiter.api.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.NativeJavaObject;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.json.JsonParser;

public class T {

  @Test
  public void t1() {
    final Context cx = Context.enter();
    final Scriptable scope = cx.initSafeStandardObjects();
    Function<String, String> f1 = new Function<String, String>() {
      @Override
      public String apply(String s) {
        return s + s;
      }
    };
    ScriptableObject.putProperty(scope, "foo", f1);
//    System.out.println(cx.evaluateString(scope, "var a = foo.apply;a;", null, 1, null));
//    System.out.println(cx.evaluateString(scope, "foo;", null, 1, null));
//    ScriptableObject.deleteProperty(scope, "foo");
//    System.out.println(cx.evaluateString(scope, "a;", null, 1, null));
//    System.out.println(cx.evaluateString(scope, "var b = foo;b;", null, 1, null));
//    Object result = cx.evaluateString(scope, "foo.apply('a')", null, 1, null);
    Object tojs = Context.javaToJS(f1, scope);
    System.out.println(tojs);
    if (tojs instanceof NativeJavaObject) {
      System.out.println(((NativeJavaObject) tojs).get("apply", scope));
      ScriptableObject.putProperty(scope, "foo2", ((NativeJavaObject) tojs).get("apply", scope));
    }
    System.out.println("eeeeeeeeeeeeeeeeee");
    Object r1 = cx.evaluateString(scope, "foo2('a')", null, 1, null);
    System.out.println(r1);
  }

  @Test
  public void t2() throws Exception {
    JSONata jsonata = JSONata.parse("{\"bar\":$foo('a')}");
//    jsonata.assignJavaMember("foo", T.class.getDeclaredMethod("f1", String.class));
//    jsonata.assignJsExpression("foo", "a => a + a");
    jsonata.assignJsExpression("foo", "function(a) {return a + a;};");
    System.out.println("ccccccccccccccccccc");
    Object evalResult = jsonata.evaluate(null);
    System.out.println(evalResult);
    System.out.println(evalResult.getClass());
    System.out.println(new ObjectMapper().writeValueAsString(evalResult));
  }

  static String f1(String a) {
    return a + a;
  }

  @Test
  public void t3() throws Exception {
//    JSONata jsonata = JSONata.parse("foo");
////    jsonata.assignJavaMember("foo", T.class.getDeclaredMethod("f1", String.class));
////    jsonata.assignJsExpression("foo", "a => a + a");
////    jsonata.assignJsExpression("foo", "function(a) {return a + a;};");
////    jsonata.evaluate(JsonNodeFactory.instance.objectNode().put("foo", 123));
//    System.out.println("ccccccccccccccccccc");
//    Object evalResult = jsonata.evaluate(JsonNodeFactory.instance.objectNode().put("foo", 123));
//    System.out.println(evalResult);
//    System.out.println(evalResult.getClass());
//    System.out.println(new ObjectMapper().writeValueAsString(evalResult));
  }

  @Test
  public void t4() throws Exception {
    final Context cx = Context.enter();
    final Scriptable scope = cx.initSafeStandardObjects();
    ScriptableObject.putProperty(scope, "foo", new JsonParser(cx, scope).parseValue(JsonNodeFactory.instance.objectNode().put("foo", 1).toString()));
    Object r = cx.evaluateString(scope, "foo", null, 1, null);
    System.out.println(r.getClass());
    System.out.println(r);
    System.out.println(new ObjectMapper().writeValueAsString(r));
  }

}
