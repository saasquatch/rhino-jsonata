package com.saasquatch.rhinojsonata;

import java.time.Duration;
import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import org.mozilla.javascript.ScriptableObject;

public final class JSONataOptions {

  private final Duration timeout;
  private final int maxDepth;

  private JSONataOptions(Duration timeout, int maxDepth) {
    this.timeout = timeout;
    this.maxDepth = maxDepth;
  }

  void mutateJSONata(JSONata jsonata) {
    if (timeout != null) {
      /*
       * The code comes from https://github.com/jsonata-js/jsonata/blob/97295a6fdf0ed0df7677e5bf36a50bb633eb53a2/test/run-test-suite.js#L158
       * It is licenced under MIT License
       */
      jsonata.cx.evaluateString(jsonata.scope, ""
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
      ScriptableObject.callMethod(jsonata.scope, "timeboxExpression",
          new Object[]{jsonata.jsonataObject, timeout, maxDepth});
    }
  }

  public static Builder newBuilder() {
    return new Builder();
  }

  public static final class Builder {

    private Duration timeout;
    private int maxDepth;

    private Builder() {}

    private Builder timeboxExpression(@Nonnull Duration timeout, @Nonnegative int maxDepth) {
      if (timeout.isNegative()) {
        throw new IllegalArgumentException("timeout cannot be negative");
      }
      if (maxDepth < 0) {
        throw new IllegalArgumentException("maxDepth cannot be negative");
      }
      this.timeout = timeout;
      this.maxDepth = maxDepth;
      return this;
    }

    public JSONataOptions build() {
      return new JSONataOptions(timeout, maxDepth);
    }

  }

}
