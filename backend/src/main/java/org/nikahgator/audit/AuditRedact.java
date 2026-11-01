package org.nikahgator.audit;

import java.lang.annotation.*;

/**
 * The field is private (a hash, private notes): the activity log records THAT it changed, never its value.
 * Fields whose name looks like a password, hash, token, secret or OTP are redacted without needing this.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface AuditRedact {
}
