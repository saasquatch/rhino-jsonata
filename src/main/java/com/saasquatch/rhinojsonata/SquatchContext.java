package com.saasquatch.rhinojsonata;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory;

final class SquatchContext extends Context {

  long startTimeNanos;
  long timeoutNanos;

  public SquatchContext(ContextFactory factory) {
    super(factory);
  }

}
