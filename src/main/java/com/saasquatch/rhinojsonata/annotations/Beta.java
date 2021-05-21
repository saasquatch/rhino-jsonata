package com.saasquatch.rhinojsonata.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Marker for things that are in beta and are subject to breaking changes without warning.
 *
 * @author sli
 */
@Retention(RetentionPolicy.SOURCE) // Only used for documentation
@Internal
public @interface Beta {

}
