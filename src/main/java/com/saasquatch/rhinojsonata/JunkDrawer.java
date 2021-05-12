package com.saasquatch.rhinojsonata;

import java.io.IOException;
import java.io.Reader;
import java.util.concurrent.ThreadLocalRandom;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.JavaScriptException;
import org.mozilla.javascript.NativeJSON;
import org.mozilla.javascript.RhinoException;
import org.mozilla.javascript.Scriptable;

final class JunkDrawer {

  private JunkDrawer() {}

  public static <T> T rethrowRhinoException(Context cx, Scriptable scope, RhinoException e) {
    if (e instanceof JavaScriptException) {
      final Object embeddedJsValue = ((JavaScriptException) e).getValue();
      throw new JSONataException(
          NativeJSON.stringify(cx, scope, embeddedJsValue, null, null).toString(), e);
    } else {
      throw new JSONataException(e.getMessage(), e);
    }
  }

  public static String readerToString(Reader reader) throws IOException {
    final char[] buf = new char[8 * 1024];
    final StringBuilder sb = new StringBuilder();
    int numCharsRead;
    while ((numCharsRead = reader.read(buf, 0, buf.length)) != -1) {
      sb.append(buf, 0, numCharsRead);
    }
    return sb.toString();
  }

  public static String randomVarName() {
    return "rand_var_" + ThreadLocalRandom.current().nextLong();
  }

}
