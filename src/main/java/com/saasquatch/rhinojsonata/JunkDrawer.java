package com.saasquatch.rhinojsonata;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Objects;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.JavaScriptException;
import org.mozilla.javascript.NativeJSON;
import org.mozilla.javascript.RhinoException;
import org.mozilla.javascript.Scriptable;

final class JunkDrawer {

  public static final String JSONATA = "jsonata", EVALUATE = "evaluate", ASSIGN = "assign",
      REGISTER_FUNCTION = "registerFunction";

  /**
   * The code comes from https://github.com/jsonata-js/jsonata/blob/97295a6fdf0ed0df7677e5bf36a50bb633eb53a2/test/run-test-suite.js#L158
   * It is licenced under MIT License
   */
  public static final String TIMEBOX_EXPRESSION_JS = ""
      + "(expr, timeout, maxDepth) => {\n"
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
      + "}";

  // Lazy init
  private static String defaultJSONataSource;

  private JunkDrawer() {}

  public static <T> T rethrowRhinoException(Context cx, Scriptable scope, RhinoException e) {
    if (e instanceof JavaScriptException) {
      final Object embeddedJsValue = ((JavaScriptException) e).getValue();
      final String message;
      if (embeddedJsValue == null) {
        message = e.getMessage();
      } else if (embeddedJsValue instanceof CharSequence) {
        message = embeddedJsValue.toString();
      } else if (embeddedJsValue.getClass().getSimpleName().equals("NativeError")) {
        // The NativeError class isn't accessible
        message = e.getMessage();
      } else {
        message = NativeJSON.stringify(cx, scope, embeddedJsValue, null, null).toString();
      }
      throw new JSONataException(message, e);
    } else {
      throw new JSONataException(e.getMessage(), e);
    }
  }

  public static String readerToString(Reader reader) throws IOException {
    final char[] buf = new char[8192];
    final StringBuilder sb = new StringBuilder();
    int numCharsRead;
    while ((numCharsRead = reader.read(buf, 0, buf.length)) != -1) {
      sb.append(buf, 0, numCharsRead);
    }
    return sb.toString();
  }

  public static String getDefaultJSONataSource() {
    String s = defaultJSONataSource;
    if (s == null) {
      defaultJSONataSource = s = loadDefaultJSONataJsSource();
    }
    return s;
  }

  private static String loadDefaultJSONataJsSource() {
    try (
        InputStream jsonataSourceStream = JSONataExpression.class.getResourceAsStream(
            "/saasquatch-jsonata-es5.min.js");
        Reader jsonataSourceReader = new InputStreamReader(
            Objects.requireNonNull(jsonataSourceStream), UTF_8)
    ) {
      return readerToString(jsonataSourceReader);
    } catch (IOException e) {
      throw new JSONataException(e.getMessage(), e);
    }
  }

}
