package com.saasquatch.rhinojsonata;

import org.mozilla.javascript.Callable;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.Scriptable;

final class SquatchContextFactory extends ContextFactory {

  public static final SquatchContextFactory INSTANCE = new SquatchContextFactory();

  @Override
  protected Context makeContext() {
    final SquatchContext squatchContext = new SquatchContext(this);
    squatchContext.setInstructionObserverThreshold(10000);
    return squatchContext;
  }

  @Override
  protected void observeInstructionCount(Context cx, int instructionCount) {
    final SquatchContext squatchContext = (SquatchContext) cx;
    if (squatchContext.timeoutNanos > 0) {
      final long elapsedNanos = System.nanoTime() - squatchContext.startTimeNanos;
      if (elapsedNanos >= squatchContext.timeoutNanos) {
        // This has to an Error instead of an Exception
        throw new SquatchTimeoutError();
      }
    }
    super.observeInstructionCount(cx, instructionCount);
  }

  @Override
  protected Object doTopCall(Callable callable, Context cx, Scriptable scope, Scriptable thisObj,
      Object[] args) {
    final SquatchContext squatchContext = (SquatchContext) cx;
    squatchContext.startTimeNanos = System.nanoTime();
    return super.doTopCall(callable, cx, scope, thisObj, args);
  }

}
