package com.saasquatch.rhinojsonata;

import java.time.Duration;
import javax.annotation.Nonnull;

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

  private JSONataExpressionOptions(long evaluateTimeoutNanos, int timeboxExpressionTimeboxMillis,
      int timeboxExpressionMaxDepth) {
    this.evaluateTimeoutNanos = evaluateTimeoutNanos;
    this.timeboxExpressionTimeboxMillis = timeboxExpressionTimeboxMillis;
    this.timeboxExpressionMaxDepth = timeboxExpressionMaxDepth;
  }

  boolean isTimeboxExpressions() {
    return timeboxExpressionTimeboxMillis > 0;
  }

  public static Builder newBuilder() {
    return new Builder();
  }

  public static final class Builder {

    private long evaluateTimeoutNanos;
    private int timeboxExpressionTimeboxMillis;
    private int timeboxExpressionMaxDepth;

    private Builder() {}

    /**
     * Set a timeout for {@link JSONataExpression#evaluate()} methods to protect against infinite
     * loops. Note that this timeout is enforced on the JS runtime level, not within jsonata-js.
     */
    public Builder setEvaluateTimeout(@Nonnull Duration evaluateTimeout) {
      final long evaluateTimeoutNanos = evaluateTimeout.toNanos();
      if (evaluateTimeoutNanos <= 0) {
        throw new IllegalArgumentException("evaluateTimeoutNanos has to be positive");
      }
      this.evaluateTimeoutNanos = evaluateTimeoutNanos;
      return this;
    }

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

    public JSONataExpressionOptions build() {
      return new JSONataExpressionOptions(evaluateTimeoutNanos, timeboxExpressionTimeboxMillis,
          timeboxExpressionMaxDepth);
    }

  }

}
