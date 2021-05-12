package com.saasquatch.jsonata;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.EvaluatorException;
import org.mozilla.javascript.JavaScriptException;
import org.mozilla.javascript.NativeJSON;
import org.mozilla.javascript.RhinoException;
import org.mozilla.javascript.Scriptable;

final class JunkDrawer {

  private JunkDrawer() {
  }

  public static <T> T rethrowRhinoException(Context cx, Scriptable scope, RhinoException e) {
    if (e instanceof JavaScriptException) {
      final Object embeddedJsValue = ((JavaScriptException) e).getValue();
      throw new JSONataException(
          NativeJSON.stringify(cx, scope, embeddedJsValue, null, null).toString(), e);
    } else if (e instanceof EvaluatorException) {
      throw new JSONataException(e.getMessage(), e);
    }
    throw e;
  }

}
