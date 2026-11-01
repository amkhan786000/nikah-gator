package org.nikahgator.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.nikahgator.entity.AppUser;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class AuditTest {

    /** A made-up entity: adopting the activity log takes one "extends Auditable". */
    static class Thing extends Auditable {
        Long id = 7L;
        Long masjidId = 3L;
        String name = "Ali";
        String passwordHash = "$2a$10$secret";
        @AuditRedact String privateNote = "very private";
        @AuditIgnore int counter;
        BigDecimal amount = new BigDecimal("10.00");
        boolean voided;
        LocalDate day = LocalDate.of(2026, 1, 2);
        List<String> notSimple = List.of("x");

        public Long getId() {
            return id;
        }

        public Long getMasjidId() {
            return masjidId;
        }
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    private static void signIn(long id, String role) {
        AppUser u = new AppUser();
        u.setId(id);
        u.setFullName("Staff " + id);
        u.setRole(role);
        u.setMasjidId(3L);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(u, null, List.of()));
    }

    // ---- the diff and redaction ----------------------------------------------------------------------

    @Test
    void snapshotKeepsOnlyPlainFieldsAndNeverTheSecrets() {
        Map<String, Object> s = ChangeDiff.snapshot(new Thing());
        assertThat(s).containsKeys("name", "amount", "voided", "day", "masjidId");
        assertThat(s).doesNotContainKeys("notSimple", "counter", "createdBy", "updatedAt");
        assertThat(s.toString()).doesNotContain("secret").doesNotContain("very private");
    }

    @Test
    void diffListsOnlyWhatChangedAndRedactsSecrets() {
        Thing t = new Thing();
        Map<String, Object> before = ChangeDiff.snapshot(t);
        t.name = "Omar";
        t.passwordHash = "$2a$10$another";
        t.privateNote = "other note";
        t.amount = new BigDecimal("10.0"); // same number, different scale: not a change
        t.counter = 9;                      // ignored
        Map<String, String[]> d = ChangeDiff.diff(before, ChangeDiff.snapshot(t));

        assertThat(d.keySet()).containsExactlyInAnyOrder("name", "passwordHash", "privateNote");
        assertThat(d.get("name")).containsExactly("Ali", "Omar");
        assertThat(d.get("passwordHash")).containsExactly(ChangeDiff.CHANGED, ChangeDiff.CHANGED);
        String json = ChangeDiff.toJson(d);
        assertThat(json).doesNotContain("secret").doesNotContain("another").doesNotContain("note");
        assertThat(json).contains("\"name\":{\"old\":\"Ali\",\"new\":\"Omar\"}");
    }

    @Test
    void secretLookingNamesAreRedacted() {
        for (String n : List.of("passwordHash", "resetToken", "codeHash", "apiSecret", "otp", "loginOtp")) {
            assertThat(ChangeDiff.isRedactedName(n)).as(n).isTrue();
        }
        for (String n : List.of("name", "hotel", "notes", "status", "mustChangePassword")) {
            assertThat(ChangeDiff.isRedactedName(n)).as(n).isFalse();
        }
    }

    @Test
    void longValuesAreShortenedAndJsonIsEscaped() {
        String json = ChangeDiff.toJson(Map.of("x", new String[] { null, "a\"b\\c\nd" }));
        assertThat(json).isEqualTo("{\"x\":{\"old\":null,\"new\":\"a\\\"b\\\\c\\nd\"}}");
        assertThat(ChangeDiff.show("z".repeat(500))).hasSizeLessThan(120).contains("500 characters");
        assertThat(ChangeDiff.toJson(Map.of())).isNull();
    }

    // ---- the entity listener -------------------------------------------------------------------------

    @Test
    void stampsTheSignedInUserAndLogsTheCreation() {
        ActivityLogService log = mock(ActivityLogService.class);
        AuditListener l = new AuditListener(log);
        signIn(11, "STAFF");
        Thing t = new Thing();
        l.creating(t);
        l.created(t);

        assertThat(t.getCreatedBy()).isEqualTo(11L);
        assertThat(t.getUpdatedBy()).isEqualTo(11L);
        verify(log).record(eq(ActivityLogService.CREATE), eq(3L), eq("Thing"), eq(7L), eq("Created thing #7"), isNull());
    }

    @Test
    void systemJobsWriteNullAndKeepAnExistingCreator() {
        AuditListener l = new AuditListener(mock(ActivityLogService.class));
        Thing t = new Thing();
        l.creating(t);
        assertThat(t.getCreatedBy()).isNull();
        assertThat(t.getUpdatedBy()).isNull();

        t.setCreatedBy(5L); // set explicitly by the service that registered it
        signIn(11, "IMAM");
        l.creating(t);
        assertThat(t.getCreatedBy()).isEqualTo(5L);
        l.updating(t);
        assertThat(t.getUpdatedBy()).isEqualTo(11L);
    }

    @Test
    void anUpdateLogsOldAndNewValuesButSkipsChangesToIgnoredFields() {
        ActivityLogService log = mock(ActivityLogService.class);
        AuditListener l = new AuditListener(log);
        Thing t = new Thing();
        l.loaded(t);

        t.counter = 4; // only an ignored field changed: no entry
        l.updated(t);
        verify(log, never()).record(any(), any(), any(), any(), any(), any());

        t.name = "Omar";
        l.updated(t);
        ArgumentCaptor<String> json = ArgumentCaptor.forClass(String.class);
        verify(log).record(eq(ActivityLogService.UPDATE), eq(3L), eq("Thing"), eq(7L), eq("Updated thing #7 (name)"), json.capture());
        assertThat(json.getValue()).isEqualTo("{\"name\":{\"old\":\"Ali\",\"new\":\"Omar\"}}");

        l.updated(t); // nothing new since the last flush: no second entry
        verify(log).record(eq(ActivityLogService.UPDATE), any(), any(), any(), any(), any());
    }

    @Test
    void voidingIsLoggedAsVoid() {
        ActivityLogService log = mock(ActivityLogService.class);
        AuditListener l = new AuditListener(log);
        Thing t = new Thing();
        l.loaded(t);
        t.voided = true;
        l.updated(t);
        verify(log).record(eq(ActivityLogService.VOID), eq(3L), eq("Thing"), eq(7L), eq("Voided thing #7"), any());
    }

    @Test
    void aDeletionKeepsTheLastValuesInTheLogWithoutSecrets() {
        ActivityLogService log = mock(ActivityLogService.class);
        AuditListener l = new AuditListener(log);
        Thing t = new Thing();
        l.loaded(t);
        l.removed(t);
        ArgumentCaptor<String> json = ArgumentCaptor.forClass(String.class);
        verify(log).record(eq(ActivityLogService.DELETE), eq(3L), eq("Thing"), eq(7L), eq("Deleted thing #7"), json.capture());
        assertThat(json.getValue()).contains("\"name\":{\"old\":\"Ali\",\"new\":null}")
                .doesNotContain("secret").doesNotContain("very private");
    }

    @Test
    void aBrokenLogNeverBreaksTheChange() {
        ActivityLogService log = mock(ActivityLogService.class);
        org.mockito.Mockito.doThrow(new IllegalStateException("db down")).when(log).record(any(), any(), any(), any(), any(), any());
        AuditListener l = new AuditListener(log);
        Thing t = new Thing();
        l.created(t); // must not throw
        l.removed(t);
    }

    @Test
    void entitiesMarkedNoActivityLogAreNotLogged() {
        @NoActivityLog
        class Quiet extends Auditable {
            public Long getId() {
                return 1L;
            }
        }
        ActivityLogService log = mock(ActivityLogService.class);
        new AuditListener(log).created(new Quiet());
        verify(log, never()).record(any(), any(), any(), any(), any(), any());
    }

    // ---- the access log and the CSV ------------------------------------------------------------------

    @Test
    void onlyOpeningASensitiveRecordIsAnAccessEvent() {
        assertThat(SensitiveViewLogger.describe("GET", "/api/imam/svc/counseling/5")[1]).isEqualTo("Opened counseling session #5");
        assertThat(SensitiveViewLogger.describe("GET", "/api/imam/svc/divorce/9/pdf")[1]).isEqualTo("Downloaded PDF of divorce record #9");
        assertThat(SensitiveViewLogger.describe("GET", "/api/imam/svc/shahada/2")[0]).isEqualTo("LifeEvent");
        assertThat(SensitiveViewLogger.describe("GET", "/api/imam/svc/counseling")).isNull();
        assertThat(SensitiveViewLogger.describe("GET", "/api/imam/svc/counseling/follow-ups")).isNull();
        assertThat(SensitiveViewLogger.describe("GET", "/api/imam/svc/juma/5")).isNull();
        assertThat(SensitiveViewLogger.describe("PUT", "/api/imam/svc/counseling/5")).isNull();
    }

    @Test
    void csvCellsAreQuotedAndFormulasDefanged() {
        assertThat(ActivityLogService.csv("say \"hi\"")).isEqualTo("\"say \"\"hi\"\"\"");
        assertThat(ActivityLogService.csv("=HYPERLINK(\"x\")")).startsWith("\"'=");
        assertThat(ActivityLogService.csv(null)).isEmpty();
    }
}
