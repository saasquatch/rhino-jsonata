package com.saasquatch.jsonata;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Objects;
import org.apache.commons.io.IOUtils;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.NativeObject;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

public final class JSONata {

  private final Context cx;
  private final Scriptable scope;
  private final NativeObject jsonataObject;

  private JSONata(Context cx, Scriptable scope, NativeObject jsonataObject) {
    this.cx = cx;
    this.scope = scope;
    this.jsonataObject = jsonataObject;
  }

  public Object evaluate(Object input) {
    return ScriptableObject.callMethod(jsonataObject, "evaluate",
        new Object[]{Context.javaToJS(input, scope)});
  }

  public void assignJavaScriptExpression(String name, String jsExpression) {
    ScriptableObject.callMethod(jsonataObject, "assign",
        new Object[]{name, cx.evaluateString(scope, jsExpression, "", 1, null)});
  }

  public void assignJavaObject(String name, Object javaObject) {
    ScriptableObject.callMethod(jsonataObject, "assign",
        new Object[]{name, Context.javaToJS(javaObject, scope)});
  }

  public void timeboxExpression(int timeout, int maxDepth) {
    ScriptableObject.callMethod(scope, "timeboxExpression",
        new Object[]{jsonataObject, timeout, maxDepth});
  }

  public static JSONata parse(String expression) {
    final String jsonataJsString;
    try {
      jsonataJsString = IOUtils.toString(Objects.requireNonNull(
          JSONata.class.getResourceAsStream("/saasquatch-jsonata-es5.min.js")), UTF_8);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
    final Context cx = Context.enter();
    final Scriptable scope = cx.initSafeStandardObjects();
    cx.evaluateString(scope, ""
        + "function timeboxExpression(expr, timeout, maxDepth) {\n"
        + "    var depth = 0;\n"
        + "    var time = Date.now();\n"
        + "\n"
        + "    var checkRunnaway = function() {\n"
        + "        if (depth > maxDepth) {\n"
        + "            // stack too deep\n"
        + "            throw {\n"
        + "                message:\n"
        + "                    \"Stack overflow error: Check for non-terminating recursive function.  Consider rewriting as tail-recursive.\",\n"
        + "                stack: new Error().stack,\n"
        + "                code: \"U1001\"\n"
        + "            };\n"
        + "        }\n"
        + "        if (Date.now() - time > timeout) {\n"
        + "            // expression has run for too long\n"
        + "            throw {\n"
        + "                message: \"Expression evaluation timeout: Check for infinite loop\",\n"
        + "                stack: new Error().stack,\n"
        + "                code: \"U1001\"\n"
        + "            };\n"
        + "        }\n"
        + "    };\n"
        + "\n"
        + "    // register callbacks\n"
        + "    expr.assign(\"__evaluate_entry\", function() {\n"
        + "        depth++;\n"
        + "        checkRunnaway();\n"
        + "    });\n"
        + "    expr.assign(\"__evaluate_exit\", function() {\n"
        + "        depth--;\n"
        + "        checkRunnaway();\n"
        + "    });\n"
        + "}", null, 1, null);
    cx.evaluateString(scope, jsonataJsString, null, 1, null);
//    final NativeObject jsonataObject = (NativeObject) cx.evaluateString(
//        scope, "jsonata(" + new JsonPrimitive(expression) + ");", "jsonataObject", 0, null);
    final NativeObject jsonataObject = (NativeObject) ScriptableObject.callMethod(
        scope, "jsonata", new Object[]{expression});
    return new JSONata(cx, scope, jsonataObject);
  }

}
