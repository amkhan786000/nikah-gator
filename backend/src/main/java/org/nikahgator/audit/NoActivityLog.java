package org.nikahgator.audit;

import java.lang.annotation.*;

/**
 * Entities that system jobs rewrite constantly (the reminder queue) still record created_by/updated_by,
 * but do not write an activity-log entry for every change.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface NoActivityLog {
}
