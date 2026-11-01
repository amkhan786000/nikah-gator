import { DatePipe } from '@angular/common';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { errorMessage } from '../core/auth.service';
import { CardTableDirective } from '../shared/card-table.directive';

interface LogRow {
  id: number;
  createdAt: string;
  kind: string;
  status: 'SENT' | 'FAILED';
  recipient: string;
  subject: string;
  sentByName: string | null;
  masjidId: number | null;
  masjidName: string | null;
  attachmentName: string | null;
}

interface LogDetail extends LogRow {
  body: string | null;
  error: string | null;
  canResend: boolean;
}

interface LogPage {
  items: LogRow[];
  total: number;
  page: number;
  size: number;
}

const KINDS: [string, string][] = [
  ['CERTIFICATE', 'Document'],
  ['OTP', 'Sign-in code'],
  ['PASSWORD', 'Password'],
  ['REMINDER', 'Reminder'],
  ['WELCOME', 'New account'],
  ['OTHER', 'Other'],
];

/** The e-mail log. The imam sees their masjid's e-mails (/imam/email-log); the platform admin sees every masjid's. */
@Component({
  selector: 'app-email-log',
  imports: [FormsModule, DatePipe, CardTableDirective],
  template: `
    <div class="page-title-box d-flex flex-wrap align-items-center justify-content-between">
      <h4 class="page-title">E-mail log</h4>
    </div>
    <p class="text-muted">Every e-mail sent from Masjidly, newest first. Entries are a permanent record and cannot be deleted. Sign-in codes and passwords are never stored.</p>
    @if (message()) { <div class="alert alert-success">{{ message() }}</div> }
    @if (error()) { <div class="alert alert-danger">{{ error() }}</div> }

    <div class="card"><div class="card-body">
      <div class="row">
        <div class="form-group col-6 col-lg-2"><label>From</label><input class="form-control" type="date" [(ngModel)]="from" name="from" /></div>
        <div class="form-group col-6 col-lg-2"><label>To</label><input class="form-control" type="date" [(ngModel)]="to" name="to" /></div>
        <div class="form-group col-12 col-lg-2"><label>Recipient</label><input class="form-control" [(ngModel)]="recipient" name="recipient" placeholder="Part of an address" (keyup.enter)="search()" /></div>
        <div class="form-group col-6 col-lg-2">
          <label>Type</label>
          <select class="form-control" [(ngModel)]="kind" name="kind">
            <option value="">All</option>
            @for (k of kinds; track k[0]) { <option [value]="k[0]">{{ k[1] }}</option> }
          </select>
        </div>
        <div class="form-group col-6 col-lg-2">
          <label>Status</label>
          <select class="form-control" [(ngModel)]="status" name="status">
            <option value="">All</option><option value="SENT">Sent</option><option value="FAILED">Failed</option>
          </select>
        </div>
        <div class="form-group col-12 col-lg-2 d-flex align-items-end"><button class="btn btn-primary btn-responsive w-100" (click)="search()">Filter</button></div>
      </div>
    </div></div>

    <div class="card"><div class="card-body p-0">
      <div class="table-responsive">
        <table class="table table-hover mb-0">
          <thead>
            <tr><th>When</th><th>Type</th><th>To</th><th>Subject</th>@if (admin) { <th>Masjid</th> }<th>Sent by</th><th>Status</th></tr>
          </thead>
          <tbody>
            @for (r of rows(); track r.id) {
              <tr style="cursor:pointer" (click)="open(r)">
                <td class="text-nowrap">{{ r.createdAt | date: 'MMM d, y, h:mm a' }}</td>
                <td>{{ kindLabel(r.kind) }}</td>
                <td style="word-break:break-all">{{ r.recipient }}</td>
                <td>{{ r.subject }}@if (r.attachmentName) { <br /><small class="text-muted"><i class="mdi mdi-paperclip"></i> {{ r.attachmentName }}</small> }</td>
                @if (admin) { <td>{{ r.masjidName }}</td> }
                <td>{{ r.sentByName || 'System' }}</td>
                <td><span class="badge" [class.badge-success]="r.status === 'SENT'" [class.badge-danger]="r.status === 'FAILED'">{{ r.status === 'SENT' ? 'Sent' : 'Failed' }}</span></td>
              </tr>
            } @empty {
              <tr><td [attr.colspan]="admin ? 7 : 6" class="text-center text-muted py-4">{{ loaded() ? 'No e-mails match.' : 'Loading…' }}</td></tr>
            }
          </tbody>
        </table>
      </div>
      @if (total() > 0) {
        <div class="d-flex flex-wrap align-items-center justify-content-between p-3" style="gap:.5rem">
          <small class="text-muted">{{ total() }} e-mail{{ total() === 1 ? '' : 's' }}, page {{ page() + 1 }} of {{ pages() }}</small>
          <div class="d-flex" style="gap:.4rem">
            <button class="btn btn-secondary btn-responsive" (click)="go(page() - 1)" [disabled]="page() === 0">Previous</button>
            <button class="btn btn-secondary btn-responsive" (click)="go(page() + 1)" [disabled]="page() + 1 >= pages()">Next</button>
          </div>
        </div>
      }
    </div></div>

    @if (detail(); as d) {
      <div class="modal d-block" tabindex="-1" (click)="detail.set(null)">
        <div class="modal-dialog modal-lg" (click)="$event.stopPropagation()">
          <div class="modal-content">
            <div class="modal-header">
              <h5 class="modal-title">{{ d.subject }}</h5>
              <button type="button" class="close" aria-label="Close" (click)="detail.set(null)"><span aria-hidden="true">&times;</span></button>
            </div>
            <div class="modal-body">
              <div class="detail-grid">
                <div><span>To</span><strong style="word-break:break-all">{{ d.recipient }}</strong></div>
                <div><span>Sent by</span><strong>{{ d.sentByName || 'System' }}</strong></div>
                <div><span>When</span><strong>{{ d.createdAt | date: 'medium' }}</strong></div>
                <div><span>Type</span><strong>{{ kindLabel(d.kind) }}</strong></div>
                <div><span>Status</span><strong>{{ d.status === 'SENT' ? 'Sent' : 'Failed' }}</strong></div>
                @if (admin && d.masjidName) { <div><span>Masjid</span><strong>{{ d.masjidName }}</strong></div> }
                @if (d.attachmentName) { <div><span>Attachment</span><strong>{{ d.attachmentName }}</strong></div> }
              </div>
              @if (d.error) { <div class="alert alert-danger mt-3 mb-0"><strong>Error:</strong> {{ d.error }}</div> }
              <p class="text-muted mt-3 mb-1">Message</p>
              <pre class="p-3 border rounded mb-0" style="white-space:pre-wrap;word-break:break-word;font-family:inherit">{{ d.body || '(not stored)' }}</pre>
            </div>
            <div class="modal-footer">
              @if (d.canResend && !admin) {
                <button class="btn btn-primary btn-responsive" (click)="resend(d)" [disabled]="busy()"><i class="mdi mdi-refresh"></i> Resend</button>
              }
              <button class="btn btn-secondary btn-responsive" (click)="detail.set(null)">Close</button>
            </div>
          </div>
        </div>
      </div>
    }
  `,
})
export class EmailLogPage {
  private http = inject(HttpClient);

  readonly admin = !!inject(ActivatedRoute).snapshot.data['admin'];
  private base = this.admin ? '/api/admin/email-log' : '/api/imam/email-log';
  kinds = KINDS;

  from = '';
  to = '';
  recipient = '';
  kind = '';
  status = '';

  rows = signal<LogRow[]>([]);
  total = signal(0);
  page = signal(0);
  size = 20;
  loaded = signal(false);
  busy = signal(false);
  error = signal('');
  message = signal('');
  detail = signal<LogDetail | null>(null);

  constructor() {
    this.load();
  }

  pages() {
    return Math.max(1, Math.ceil(this.total() / this.size));
  }

  kindLabel(k: string) {
    return KINDS.find((x) => x[0] === k)?.[1] ?? k;
  }

  search() {
    this.page.set(0);
    this.load();
  }

  go(p: number) {
    this.page.set(Math.max(0, p));
    this.load();
  }

  load() {
    let params = new HttpParams().set('page', this.page()).set('size', this.size);
    if (this.from) params = params.set('from', this.from);
    if (this.to) params = params.set('to', this.to);
    if (this.recipient.trim()) params = params.set('recipient', this.recipient.trim());
    if (this.kind) params = params.set('kind', this.kind);
    if (this.status) params = params.set('status', this.status);
    this.http.get<LogPage>(this.base, { params }).subscribe({
      next: (r) => {
        this.rows.set(r.items);
        this.total.set(r.total);
        this.loaded.set(true);
        this.error.set('');
      },
      error: (e) => {
        this.loaded.set(true);
        this.error.set(errorMessage(e));
      },
    });
  }

  open(r: LogRow) {
    this.http.get<LogDetail>(`${this.base}/${r.id}`).subscribe({
      next: (d) => this.detail.set(d),
      error: (e) => this.error.set(errorMessage(e)),
    });
  }

  resend(d: LogDetail) {
    this.busy.set(true);
    this.http.post<{ sent: string[]; failed: unknown[] }>(`${this.base}/${d.id}/resend`, {}).subscribe({
      next: (r) => {
        this.busy.set(false);
        this.detail.set(null);
        this.error.set('');
        this.message.set(r.sent.length ? `Sent again to ${r.sent.join(', ')}.` : '');
        if (!r.sent.length) this.error.set('It could not be sent again. The attempt is in the log.');
        this.load();
      },
      error: (e) => {
        this.busy.set(false);
        this.detail.set(null);
        this.error.set(errorMessage(e));
      },
    });
  }
}
