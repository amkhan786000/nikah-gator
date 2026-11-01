import { HttpClient } from '@angular/common/http';
import { Component, inject, input, output, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { errorMessage } from '../core/auth.service';

export interface EmailOutcome {
  sent: string[];
  failed: { to: string; error: string }[];
}

/**
 * "Send by e-mail" for a document that has a PDF. Put it inside the row of action buttons: the button joins the
 * row and the form opens full-width underneath. POSTs {to, message} to `url` (the record's /email endpoint).
 */
@Component({
  selector: 'app-send-email',
  imports: [FormsModule],
  styles: [':host { display: contents; }'],
  template: `
    @if (!inline()) {
      <button type="button" class="btn btn-secondary btn-responsive needs-manage" (click)="open.set(!open())">
        <i class="mdi mdi-email-send-outline"></i> Send by e-mail
      </button>
    }
    @if (open() || inline()) {
      <div class="p-3 border rounded no-print needs-manage" style="flex: 0 0 100%">
        <strong>Send {{ noun() }} by e-mail</strong>
        <p class="small text-muted mb-2">The PDF is attached. Each person gets their own copy, and every e-mail is kept in the e-mail log.</p>
        @for (r of recipients(); track $index) {
          <div class="form-group mb-2 d-flex" style="gap:.4rem">
            <input class="form-control" type="email" [ngModelOptions]="{ standalone: true }" [ngModel]="r" (ngModelChange)="setRecipient($index, $event)"
                   maxlength="190" placeholder="name@example.com" [attr.aria-label]="'Recipient ' + ($index + 1)" />
            @if (recipients().length > 1) {
              <button type="button" class="btn btn-outline-secondary" (click)="removeRecipient($index)" aria-label="Remove recipient"><i class="mdi mdi-close"></i></button>
            }
          </div>
        }
        @if (recipients().length < 5) {
          <button type="button" class="btn btn-link p-0 mb-2" (click)="addRecipient()">+ Add another address</button>
        }
        <div class="form-group">
          <label>Message (optional)</label>
          <textarea class="form-control" rows="3" maxlength="1000" [ngModelOptions]="{ standalone: true }" [(ngModel)]="message" placeholder="A short note to go with the document"></textarea>
        </div>
        @if (sensitive()) {
          <div class="alert alert-warning py-2">
            <i class="mdi mdi-shield-lock-outline"></i> This is a private record. E-mail can be seen by others if it is sent to the wrong address.
            <label class="d-block mt-1 mb-0"><input type="checkbox" [ngModelOptions]="{ standalone: true }" [(ngModel)]="confirmed" /> I want to send this private record by e-mail.</label>
          </div>
        }
        @if (error()) { <div class="alert alert-danger py-2">{{ error() }}</div> }
        @if (result(); as r) {
          @if (r.sent.length) { <div class="alert alert-success py-2">Sent to {{ r.sent.join(', ') }}.</div> }
          @if (r.failed.length) {
            <div class="alert alert-danger py-2">Could not be delivered to {{ failedList(r) }}. The attempt is in the e-mail log.</div>
          }
        }
        <div class="d-flex flex-wrap" style="gap:.4rem">
          <button type="button" class="btn btn-primary btn-responsive" (click)="send()" [disabled]="busy() || (sensitive() && !confirmed)">
            <i class="mdi mdi-send"></i> {{ busy() ? 'Sending…' : 'Send' }}
          </button>
          <button type="button" class="btn btn-secondary btn-responsive" (click)="close()">Close</button>
        </div>
      </div>
    }
  `,
})
export class SendEmail {
  private http = inject(HttpClient);

  /** The record's e-mail endpoint, e.g. /api/imam/svc/juma/3/email. */
  readonly url = input.required<string>();
  /** Private records (divorce, shahada) ask for a tick before sending. */
  readonly sensitive = input(false);
  readonly noun = input('this document');
  /** Always open, with no button of its own (for a table row); `closed` fires on Close. */
  readonly inline = input(false);
  readonly closed = output<void>();

  open = signal(false);
  busy = signal(false);
  error = signal('');
  result = signal<EmailOutcome | null>(null);
  recipients = signal<string[]>(['']);
  message = '';
  confirmed = false;

  close() {
    this.open.set(false);
    this.closed.emit();
  }

  setRecipient(i: number, v: string) {
    this.recipients.update((r) => r.map((x, j) => (j === i ? v : x)));
  }

  addRecipient() {
    this.recipients.update((r) => (r.length < 5 ? [...r, ''] : r));
  }

  removeRecipient(i: number) {
    this.recipients.update((r) => r.filter((_, j) => j !== i));
  }

  failedList(r: EmailOutcome): string {
    return r.failed.map((f) => f.to).join(', ');
  }

  send() {
    const to = this.recipients().map((x) => x.trim()).filter((x) => x);
    this.error.set('');
    this.result.set(null);
    if (!to.length) {
      this.error.set('Enter at least one e-mail address.');
      return;
    }
    this.busy.set(true);
    this.http.post<EmailOutcome>(this.url(), { to, message: this.message.trim() || null }).subscribe({
      next: (r) => {
        this.busy.set(false);
        this.result.set(r);
        if (!r.failed.length) {
          this.recipients.set(['']);
          this.message = '';
          this.confirmed = false;
        }
      },
      error: (e) => {
        this.busy.set(false);
        this.error.set(errorMessage(e));
      },
    });
  }
}
