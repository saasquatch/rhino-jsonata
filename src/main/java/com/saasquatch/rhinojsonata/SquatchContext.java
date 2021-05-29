package com.saasquatch.rhinojsonata;

import org.mozilla.javascript.Callable;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.Scriptable;

final class SquatchContext extends Context {

  /**
   * ONLY to be set by {@link SquatchContextFactory#doTopCall(Callable, Context, Scriptable,
   * Scriptable, Object[])}. The example provided by Rhino uses {@link System#currentTimeMillis()},
   * but {@link System#nanoTime()} is better for obvious reasons.
   */
  long startTimeNanos;
  /**
   * The execution timeout in nanos. Set it to a positive number to enforce the timeout.
   */
  long timeoutNanos;

  SquatchContext(ContextFactory factory) {
    super(factory);
  }

}
