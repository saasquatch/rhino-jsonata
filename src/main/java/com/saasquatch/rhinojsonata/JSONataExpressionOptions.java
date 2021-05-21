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

  private JSONataExpressionOptions(long evaluateTimeoutNanos) {
    this.evaluateTimeoutNanos = evaluateTimeoutNanos;
  }

  public static Builder newBuilder() {
    return new Builder();
  }

  public static final class Builder {

    private long evaluateTimeoutNanos;

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

    public JSONataExpressionOptions build() {
      return new JSONataExpressionOptions(evaluateTimeoutNanos);
    }

  }

}
