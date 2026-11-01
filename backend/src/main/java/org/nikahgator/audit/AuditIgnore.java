package org.nikahgator.audit;

import java.lang.annotation.*;

/** The field is bookkeeping noise (counters, lock timestamps): a change to it alone is not worth an activity-log entry. */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface AuditIgnore {
}
