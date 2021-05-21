package com.saasquatch.rhinojsonata.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Marker for things that are considered internal and are subject to breaking changes without
 * warning, even if they are public in Java.
 *
 * @author sli
 */
@Retention(RetentionPolicy.SOURCE) // Only used for documentation
@Internal // This annotation itself is internal
public @interface Internal {

}
