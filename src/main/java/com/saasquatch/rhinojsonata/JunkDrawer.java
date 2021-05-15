package com.saasquatch.rhinojsonata;

import static java.nio.charset.StandardCharsets.UTF_8;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.WillNotClose;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.JavaScriptException;
import org.mozilla.javascript.NativeJSON;
import org.mozilla.javascript.RhinoException;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.Undefined;
import org.mozilla.javascript.json.JsonParser;

/**
 * Internal utils
 *
 * @author sli
 */
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

  private JunkDrawer() {}

  public static <T> T rethrowRhinoException(@Nonnull Context cx, @Nonnull Scriptable scope,
      @Nonnull RhinoException e) {
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

  public static Object jsonNodeToJs(@Nonnull Context cx, @Nonnull Scriptable scope,
      @Nonnull ObjectMapper objectMapper, @Nullable JsonNode jsonNode) {
    if (jsonNode == null) {
      return null;
    }
    switch (jsonNode.getNodeType()) {
      case NULL:
        return null;
      case MISSING:
        return Undefined.instance;
      case BOOLEAN:
        return jsonNode.booleanValue();
      case STRING:
        return jsonNode.textValue();
      default:
        break;
    }
    // Not handling numbers separately because Rhino has some peculiar ways of dealing with numbers
    try {
      return new JsonParser(cx, scope).parseValue(objectMapper.writeValueAsString(jsonNode));
    } catch (JsonParser.ParseException | IOException e) {
      throw new JSONataException(e.getMessage(), e);
    }
  }

  public static String readToString(@Nonnull @WillNotClose InputStream inputStream,
      @Nonnull Charset charset) throws IOException {
    final ByteArrayOutputStream result = new ByteArrayOutputStream();
    final byte[] buffer = new byte[8192];
    int bytesRead;
    while ((bytesRead = inputStream.read(buffer)) != -1) {
      result.write(buffer, 0, bytesRead);
    }
    return result.toString(charset.name());
  }

  public static String getDefaultJSONataSource() {
    try (InputStream jsonataSourceStream = JSONataExpression.class.getResourceAsStream(
        "/saasquatch-jsonata-es5.min.js")) {
      // The source for jsonata-es5.min.js is just over 100 KB
      return readToString(Objects.requireNonNull(jsonataSourceStream), UTF_8);
    } catch (IOException e) {
      throw new JSONataException(e.getMessage(), e);
    }
  }

}
