package com.saasquatch.rhinojsonata;

import java.time.Duration;
import javax.annotation.Nonnull;

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

    public void setEvaluateTimeout(@Nonnull Duration evaluateTimeout) {
      final long evaluateTimeoutNanos = evaluateTimeout.toNanos();
      if (evaluateTimeoutNanos <= 0) {
        throw new IllegalArgumentException("evaluateTimeoutNanos has to be positive");
      }
      this.evaluateTimeoutNanos = evaluateTimeoutNanos;
    }

    public JSONataExpressionOptions build() {
      return new JSONataExpressionOptions(evaluateTimeoutNanos);
    }

  }

}
