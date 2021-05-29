package com.saasquatch.rhinojsonata;

import com.saasquatch.rhinojsonata.annotations.Beta;
import java.time.Duration;
import java.util.Objects;
import javax.annotation.Nonnull;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

/**
 * Options for {@link JSONataExpression}
 *
 * @author sli
 * @see #newBuilder()
 * @see JSONata#parse(String, JSONataExpressionOptions)
 */
public final class JSONataExpressionOptions {

  final long evaluateTimeoutNanos;
  final int timeboxExpressionTimeboxMillis;
  final int timeboxExpressionMaxDepth;
  final Scriptable scope;

  private JSONataExpressionOptions(long evaluateTimeoutNanos, int timeboxExpressionTimeboxMillis,
      int timeboxExpressionMaxDepth, Scriptable scope) {
    this.evaluateTimeoutNanos = evaluateTimeoutNanos;
    this.timeboxExpressionTimeboxMillis = timeboxExpressionTimeboxMillis;
    this.timeboxExpressionMaxDepth = timeboxExpressionMaxDepth;
    this.scope = scope;
  }

  public static Builder newBuilder() {
    return new Builder();
  }

  public static final class Builder {

    private long evaluateTimeoutNanos;
    private int timeboxExpressionTimeboxMillis;
    private int timeboxExpressionMaxDepth;
    private Scriptable scope;

    private Builder() {}

    /**
     * Set a timeout for {@link JSONataExpression#evaluate()} methods to protect against infinite
     * loops. Note that this timeout is enforced on the JS runtime level, not within jsonata-js.
     */
    @Beta
    public Builder setEvaluateTimeout(@Nonnull Duration evaluateTimeout) {
      final long evaluateTimeoutNanos = evaluateTimeout.toNanos();
      if (evaluateTimeoutNanos <= 0) {
        throw new IllegalArgumentException("evaluateTimeoutNanos has to be positive");
      }
      this.evaluateTimeoutNanos = evaluateTimeoutNanos;
      return this;
    }

    /**
     * Set a timeout and a max stack depth for {@link JSONataExpression#evaluate()} methods to
     * protect against infinite loops. Note that the timeout and maxDepth are enforced within
     * jsonata-js, not on the JS runtime level.<br>This method has a few caveats:
     * <ul>
     *   <li>This method relies on <a href="https://github.com/jsonata-js/jsonata/blob/97295a6fdf0ed0df7677e5bf36a50bb633eb53a2/test/run-test-suite.js#L158">
     *   internal and potentially unstable APIs from jsonata-js</a>, so it's possible that it won't work with a future version of jsonata-js.</li>
     *   <li>Due to the nature of the jsonata-js internal method, using this method will make the
     *   {@link JSONataExpression#evaluate()} methods not thread safe.</li>
     * </ul>
     */
    @Beta
    public Builder timeboxExpression(@Nonnull Duration timeout, int maxDepth) {
      final int timeoutMillis = (int) timeout.toMillis();
      if (timeoutMillis <= 0) {
        throw new IllegalArgumentException("timeout has to be positive");
      }
      if (maxDepth <= 0) {
        throw new IllegalArgumentException("maxDepth has to be positive");
      }
      this.timeboxExpressionTimeboxMillis = timeoutMillis;
      this.timeboxExpressionMaxDepth = maxDepth;
      return this;
    }

    /**
     * Set the Rhino scope for the {@link JSONataExpression}. By default, every {@link
     * JSONataExpression} gets its own scope with {@link Context#initSafeStandardObjects()}. If
     * having a shared scope across all {@link JSONataExpression}s is desired, then a separate
     * global scope can be created and passed in here. This method can also be useful for preloading
     * JavaScript libraries for the expression to use.
     */
    @Beta
    public Builder setScope(@Nonnull Scriptable scope) {
      this.scope = Objects.requireNonNull(scope);
      return this;
    }

    public JSONataExpressionOptions build() {
      return new JSONataExpressionOptions(evaluateTimeoutNanos, timeboxExpressionTimeboxMillis,
          timeboxExpressionMaxDepth, scope);
    }

  }

}
