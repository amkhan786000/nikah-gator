import { HttpClient } from '@angular/common/http';
import { DatePipe } from '@angular/common';
import { Component, effect, inject, input, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { errorMessage } from '../core/auth.service';
import { CardTableDirective } from '../shared/card-table.directive';
import { METHODS, PaymentSummary, PaymentView, dollars, statusBadge, statusLabel, toCents, today } from './payment.model';

type Action = { mode: 'refund' | 'void' | 'waive'; payment?: PaymentView };

/** Fee, balance and manual payments of one nikah. Self-contained: give it the nikah id and it loads everything itself. */
@Component({
  selector: 'app-nikah-payments',
  imports: [FormsModule, DatePipe, RouterLink, CardTableDirective],
  template: `
    <div class="card no-print"><div class="card-body">
      <div class="d-flex flex-wrap align-items-center justify-content-between mb-2" style="gap:.5rem">
        <span class="card-header-title mb-0">Fee and payments</span>
        @if (s(); as s) { <span class="badge" [class]="badge(s.status)">{{ label(s.status) }}</span> }
      </div>
      @if (error()) { <div class="alert alert-danger">{{ error() }}</div> }

      @if (s(); as s) {
        <div class="detail-grid">
          <div><span>Nikah fee</span><strong>{{ money(s.feeCents) }}</strong>@if (s.feeWaived) { <small>Waived: {{ s.waivedReason }}</small> }</div>
          <div><span>Paid</span><strong>{{ money(s.paidCents) }}</strong></div>
          <div><span>Balance due</span><strong>{{ money(s.balanceCents) }}</strong></div>
          <div><span>Donations</span><strong>{{ money(s.donationCents) }}</strong></div>
        </div>

        <div class="d-flex flex-wrap mt-3" style="gap:.4rem">
          <button class="btn btn-primary btn-responsive" (click)="openForm('FEE')"><i class="mdi mdi-cash-multiple"></i> Record payment</button>
          <button class="btn btn-secondary btn-responsive" (click)="openForm('DONATION')"><i class="mdi mdi-hand-heart"></i> Record donation</button>
          @if (s.feeWaived) {
            <button class="btn btn-secondary btn-responsive" (click)="unwaive()">Remove waiver</button>
          } @else {
            <button class="btn btn-secondary btn-responsive" (click)="ask({ mode: 'waive' })">Waive fee</button>
          }
        </div>

        @if (form(); as f) {
          <form class="mt-3 p-3 border rounded" (ngSubmit)="save()">
            <strong>{{ f.kind === 'FEE' ? 'Record fee payment' : 'Record donation' }}</strong>
            <div class="row mt-2">
              <div class="form-group col-6 col-md-3">
                <label>Amount (USD)</label>
                <input class="form-control" name="amt" type="number" inputmode="decimal" min="0.01" max="100000" step="0.01" [(ngModel)]="f.amount" required />
              </div>
              <div class="form-group col-6 col-md-3">
                <label>Method</label>
                <select class="form-control" name="method" [(ngModel)]="f.method">
                  @for (m of methods; track m) { <option [value]="m">{{ m }}</option> }
                </select>
              </div>
              <div class="form-group col-12 col-md-3">
                <label>Date received</label>
                <input class="form-control" name="date" type="date" [max]="maxDate" [(ngModel)]="f.paidAt" required />
              </div>
              <div class="form-group col-12 col-md-3">
                <label>Paid by</label>
                <input class="form-control" name="payer" [(ngModel)]="f.payerName" maxlength="200" placeholder="Groom (default)" />
              </div>
              <div class="form-group col-12">
                <label>Note (optional)</label>
                <input class="form-control" name="note" [(ngModel)]="f.note" maxlength="500" placeholder="e.g. check no. 1042" />
              </div>
            </div>
            <div class="d-flex flex-wrap" style="gap:.4rem">
              <button class="btn btn-primary btn-responsive" [disabled]="busy()">Save payment</button>
              <button type="button" class="btn btn-secondary btn-responsive" (click)="form.set(null)">Cancel</button>
            </div>
          </form>
        }

        @if (action(); as a) {
          <form class="mt-3 p-3 border rounded" (ngSubmit)="confirm()">
            <strong>
              @switch (a.mode) {
                @case ('waive') { Waive the nikah fee }
                @case ('refund') { Mark {{ money(a.payment!.amountCents) }} ({{ a.payment!.receiptNumber }}) as refunded }
                @case ('void') { Void mistaken entry {{ a.payment!.receiptNumber }} }
              }
            </strong>
            <div class="form-group mt-2">
              <label>Reason (kept in the record)</label>
              <input class="form-control" name="reason" [(ngModel)]="reason" maxlength="500" required />
            </div>
            <div class="d-flex flex-wrap" style="gap:.4rem">
              <button class="btn btn-primary btn-responsive" [disabled]="busy() || !reason.trim()">Confirm</button>
              <button type="button" class="btn btn-secondary btn-responsive" (click)="action.set(null)">Cancel</button>
            </div>
          </form>
        }

        <div class="table-responsive mt-3">
          <table class="table table-hover mb-0">
            <thead><tr><th>Date</th><th>Type</th><th>Amount</th><th>Method</th><th>Paid by</th><th>Status</th><th></th></tr></thead>
            <tbody>
              @for (p of s.payments; track p.id) {
                <tr [class.text-muted]="p.status !== 'PAID'">
                  <td class="text-nowrap">{{ p.paidAt | date: 'MMM d, y' }}</td>
                  <td>{{ p.kind === 'FEE' ? 'Fee' : 'Donation' }}</td>
                  <td class="text-nowrap" [style.text-decoration]="p.status === 'PAID' ? null : 'line-through'">{{ money(p.amountCents) }}</td>
                  <td>{{ p.method }}</td>
                  <td>{{ p.payerName }}@if (p.note) { <br /><small>{{ p.note }}</small> }</td>
                  <td>
                    @if (p.status === 'PAID') { <span class="badge badge-success">Paid</span> }
                    @else { <span class="badge badge-secondary">{{ p.status === 'VOID' ? 'Void' : 'Refunded' }}</span>
                      <br /><small>{{ p.statusReason }}</small> }
                  </td>
                  <td class="text-right">
                    @if (p.status === 'PAID') {
                      <div class="d-flex flex-wrap justify-content-end" style="gap:.3rem">
                        <a class="btn btn-sm btn-outline-primary" [routerLink]="['/imam/nikahs', nikahId(), 'payments', p.id, 'receipt']"><i class="mdi mdi-receipt"></i> Receipt</a>
                        <button type="button" class="btn btn-sm btn-outline-secondary" (click)="ask({ mode: 'refund', payment: p })">Refund</button>
                        <button type="button" class="btn btn-sm btn-outline-danger" (click)="ask({ mode: 'void', payment: p })">Void</button>
                      </div>
                    }
                  </td>
                </tr>
              } @empty {
                <tr><td colspan="7" class="text-center text-muted py-3">No payments recorded yet.</td></tr>
              }
            </tbody>
          </table>
        </div>
      } @else if (!error()) {
        <p class="text-muted mb-0">Loading…</p>
      }
    </div></div>
  `,
})
export class NikahPayments {
  nikahId = input.required<number | string>();
  private http = inject(HttpClient);

  s = signal<PaymentSummary | null>(null);
  error = signal('');
  busy = signal(false);
  form = signal<{ kind: 'FEE' | 'DONATION'; amount: number | null; method: string; paidAt: string; payerName: string; note: string } | null>(null);
  action = signal<Action | null>(null);
  reason = '';

  readonly methods = METHODS;
  readonly maxDate = today();
  money = dollars;
  label = statusLabel;
  badge = statusBadge;

  constructor() {
    effect(() => {
      this.nikahId();
      this.reload();
    });
  }

  private url(suffix = '') {
    return `/api/imam/nikahs/${this.nikahId()}/payments${suffix}`;
  }

  reload() {
    this.http.get<PaymentSummary>(this.url()).subscribe({
      next: (r) => this.s.set(r),
      error: (e) => this.error.set(errorMessage(e)),
    });
  }

  openForm(kind: 'FEE' | 'DONATION') {
    const s = this.s();
    if (!s) return;
    this.action.set(null);
    const amount = kind === 'FEE' ? s.balanceCents / 100 : s.suggestedDonationCents / 100;
    this.form.set({ kind, amount: amount > 0 ? amount : null, method: 'CASH', paidAt: today(), payerName: '', note: '' });
  }

  save() {
    const f = this.form();
    if (!f) return;
    const cents = toCents(f.amount);
    if (cents === null) {
      this.error.set('Enter an amount between $0.01 and $100,000.');
      return;
    }
    this.run(this.http.post(this.url(), { kind: f.kind, amountCents: cents, method: f.method, paidAt: f.paidAt, payerName: f.payerName, note: f.note }), () => this.form.set(null));
  }

  ask(a: Action) {
    this.form.set(null);
    this.reason = '';
    this.action.set(a);
  }

  confirm() {
    const a = this.action();
    if (!a || !this.reason.trim()) return;
    const body = { reason: this.reason.trim() };
    const req = a.mode === 'waive'
      ? this.http.put(this.url('/waiver'), { waived: true, reason: body.reason })
      : this.http.post(this.url(`/${a.payment!.id}/${a.mode}`), body);
    this.run(req, () => this.action.set(null));
  }

  unwaive() {
    this.run(this.http.put(this.url('/waiver'), { waived: false }), () => {});
  }

  private run(req: import('rxjs').Observable<unknown>, done: () => void) {
    this.error.set('');
    this.busy.set(true);
    req.subscribe({
      next: () => {
        this.busy.set(false);
        done();
        this.reload();
      },
      error: (e) => {
        this.busy.set(false);
        this.error.set(errorMessage(e));
      },
    });
  }
}
