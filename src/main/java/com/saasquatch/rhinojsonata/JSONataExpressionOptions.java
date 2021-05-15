package com.saasquatch.rhinojsonata;

import java.time.Duration;
import javax.annotation.Nonnull;

public final class JSONataExpressionOptions {

  final long timeoutNanos;
  final int maxStackDepth;

  private JSONataExpressionOptions(long timeoutNanos, int maxStackDepth) {
    this.timeoutNanos = timeoutNanos;
    this.maxStackDepth = maxStackDepth;
  }

  public static final class Builder {

    private long timeoutNanos = 0;
    private int maxStackDepth = 0;

    private Builder() {}

    public Builder setTimeout(@Nonnull Duration timeout) {
      final long timeoutNanos = timeout.toNanos();
      if (timeoutNanos <= 0) {
        throw new IllegalArgumentException("timeout has to be positive");
      }
      this.timeoutNanos = timeoutNanos;
      return this;
    }

    public Builder setMaxStackDepth(int maxStackDepth) {
      if (maxStackDepth <= 0) {
        throw new IllegalArgumentException("maxStackDepth has to be positive");
      }
      this.maxStackDepth = maxStackDepth;
      return this;
    }

    public JSONataExpressionOptions build() {
      return new JSONataExpressionOptions(timeoutNanos, maxStackDepth);
    }

  }

}
