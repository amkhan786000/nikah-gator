import { HttpClient } from '@angular/common/http';
import { DatePipe } from '@angular/common';
import { Component, effect, inject, input, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { errorMessage } from '../core/auth.service';
import { CardTableDirective } from '../shared/card-table.directive';

interface NotificationView {
  id: number;
  kind: string | null;
  channel: string;
  recipient: string;
  subject: string | null;
  status: 'PENDING' | 'SENDING' | 'SENT' | 'FAILED' | 'CANCELLED';
  sendAt: string;
  sentAt: string | null;
  error: string | null;
  attempts: number;
}

interface ReminderSettings {
  enabled: boolean;
  daysBefore: number;
}

const KINDS: Record<string, string> = {
  CONFIRMATION: 'Confirmation to the couple',
  IMAM_1D: 'Reminder to you, 1 day before',
};

/** Reminder e-mails of one nikah (scheduled, sent, failed) with Send now / Cancel, plus the masjid's reminder settings. */
@Component({
  selector: 'app-nikah-notifications',
  imports: [CardTableDirective, DatePipe, FormsModule],
  template: `
    <div class="card no-print"><div class="card-body">
      <div class="d-flex flex-wrap align-items-center justify-content-between mb-2" style="gap:.4rem">
        <span class="card-header-title mb-0"><i class="mdi mdi-bell-outline"></i> Notifications</span>
        <button class="btn btn-sm btn-secondary" (click)="toggleSettings()">
          <i class="mdi mdi-settings"></i> Reminder settings
        </button>
      </div>

      @if (showSettings()) {
        <form class="border rounded p-3 mb-3" (ngSubmit)="saveSettings()">
          <div class="form-group form-check">
            <input id="rem-enabled" type="checkbox" class="form-check-input" name="enabled" [(ngModel)]="settings.enabled" />
            <label for="rem-enabled" class="form-check-label">E-mail reminders to couples (when they have an e-mail address)</label>
          </div>
          <div class="form-group">
            <label for="rem-days">First reminder, days before the nikah (a reminder the day before is always sent)</label>
            <input id="rem-days" type="number" min="1" max="30" class="form-control" style="max-width:8rem" name="days"
                   [(ngModel)]="settings.daysBefore" [disabled]="!settings.enabled" />
          </div>
          <p class="text-muted small mb-2">Reminders go out at 9:00 AM masjid time. You always get one the day before. This applies to all upcoming nikahs.</p>
          <button type="submit" class="btn btn-primary btn-sm" [disabled]="savingSettings()">Save settings</button>
          @if (settingsMessage()) { <span class="ml-2 text-success">{{ settingsMessage() }}</span> }
        </form>
      }

      @if (error()) { <div class="alert alert-danger">{{ error() }}</div> }
      @if (loaded() && items().length === 0) {
        <p class="text-muted mb-0">No e-mails are scheduled. They are planned when a nikah has a date in the future and the couple's e-mail addresses are filled in.</p>
      }
      @if (items().length > 0) {
        <div class="table-responsive">
          <table class="table table-hover mb-0">
            <thead><tr><th>Message</th><th>To</th><th>When</th><th>Status</th><th></th></tr></thead>
            <tbody>
              @for (m of items(); track m.id) {
                <tr>
                  <td>{{ label(m) }}</td>
                  <td>{{ m.recipient }}</td>
                  <td>{{ (m.status === 'SENT' && m.sentAt ? m.sentAt : m.sendAt) + 'Z' | date: 'MMM d, y, h:mm a' }}</td>
                  <td>
                    <span class="badge" [class]="badge(m.status)">{{ statusText(m.status) }}</span>
                    @if (m.error && (m.status === 'FAILED' || m.status === 'PENDING')) { <br /><small class="text-danger">{{ m.error }}</small> }
                  </td>
                  <td class="text-nowrap">
                    @if (m.status === 'PENDING' || m.status === 'FAILED') {
                      <button class="btn btn-sm btn-primary" (click)="sendNow(m)" [disabled]="busy() === m.id">
                        {{ m.status === 'FAILED' ? 'Try again' : 'Send now' }}
                      </button>
                      <button class="btn btn-sm btn-secondary ml-1" (click)="cancel(m)" [disabled]="busy() === m.id">Cancel</button>
                    }
                  </td>
                </tr>
              }
            </tbody>
          </table>
        </div>
      }
    </div></div>
  `,
})
export class NikahNotifications {
  private http = inject(HttpClient);

  readonly nikahId = input.required<number>();
  /** Any value that changes when the nikah changes (e.g. its status): the list is reloaded then. */
  readonly refresh = input<unknown>();

  items = signal<NotificationView[]>([]);
  loaded = signal(false);
  error = signal('');
  busy = signal<number | null>(null);
  showSettings = signal(false);
  savingSettings = signal(false);
  settingsMessage = signal('');
  settings: ReminderSettings = { enabled: true, daysBefore: 7 };

  constructor() {
    effect(() => {
      this.refresh();
      this.load(this.nikahId());
    });
  }

  private url(path = '') {
    return `/api/imam/nikahs/${this.nikahId()}/notifications${path}`;
  }

  load(id = this.nikahId()) {
    this.http.get<NotificationView[]>(`/api/imam/nikahs/${id}/notifications`).subscribe({
      next: (r) => {
        this.items.set(r);
        this.loaded.set(true);
        this.error.set('');
      },
      error: (e) => this.error.set(errorMessage(e)),
    });
  }

  label(m: NotificationView): string {
    const k = m.kind ?? '';
    const days = /^COUPLE_(\d+)D$/.exec(k);
    if (days) return `Reminder to the couple, ${days[1]} day${days[1] === '1' ? '' : 's'} before`;
    return KINDS[k] ?? m.subject ?? 'Message';
  }

  statusText(s: string): string {
    return { PENDING: 'Scheduled', SENDING: 'Sending', SENT: 'Sent', FAILED: 'Failed', CANCELLED: 'Cancelled' }[s] ?? s;
  }

  badge(s: string): string {
    return 'badge-' + ({ PENDING: 'info', SENDING: 'info', SENT: 'success', FAILED: 'danger', CANCELLED: 'secondary' }[s] ?? 'secondary');
  }

  sendNow(m: NotificationView) {
    this.act(m, '/send-now');
  }

  cancel(m: NotificationView) {
    this.act(m, '/cancel');
  }

  private act(m: NotificationView, action: string) {
    this.busy.set(m.id);
    this.http.post<NotificationView>(this.url(`/${m.id}${action}`), {}).subscribe({
      next: (r) => {
        this.items.update((list) => list.map((x) => (x.id === r.id ? r : x)));
        this.busy.set(null);
        this.error.set('');
      },
      error: (e) => {
        this.busy.set(null);
        this.error.set(errorMessage(e));
        this.load();
      },
    });
  }

  toggleSettings() {
    const open = !this.showSettings();
    this.showSettings.set(open);
    this.settingsMessage.set('');
    if (open) {
      this.http.get<ReminderSettings>('/api/imam/masjid/reminders').subscribe({
        next: (r) => (this.settings = r),
        error: (e) => this.error.set(errorMessage(e)),
      });
    }
  }

  saveSettings() {
    this.savingSettings.set(true);
    this.http.put<ReminderSettings>('/api/imam/masjid/reminders', this.settings).subscribe({
      next: (r) => {
        this.settings = r;
        this.savingSettings.set(false);
        this.settingsMessage.set('Saved.');
        this.error.set('');
        this.load();
      },
      error: (e) => {
        this.savingSettings.set(false);
        this.error.set(errorMessage(e));
      },
    });
  }
}
