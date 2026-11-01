import { HttpClient } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { errorMessage } from '../core/auth.service';
import { dollars, toCents } from './payment.model';

interface MasjidSettings {
  name: string;
  addressLine: string | null;
  city: string | null;
  state: string | null;
  zip: string | null;
  phone: string | null;
  email: string | null;
  nikahFeeCents: number;
  suggestedDonationCents: number;
}

@Component({
  selector: 'app-masjid-settings',
  imports: [FormsModule],
  template: `
    <div class="page-title-box"><h4 class="page-title">Masjid settings</h4></div>
    @if (message()) { <div class="alert alert-success">{{ message() }}</div> }
    @if (error()) { <div class="alert alert-danger">{{ error() }}</div> }

    @if (m(); as m) {
      <div class="card"><div class="card-body">
        <span class="card-header-title">Masjid</span>
        <div class="detail-grid">
          <div><span>Name</span><strong>{{ m.name }}</strong></div>
          <div><span>Address</span><strong>{{ address(m) || '—' }}</strong></div>
          <div><span>Phone</span><strong>{{ m.phone || '—' }}</strong></div>
          <div><span>Email</span><strong>{{ m.email || '—' }}</strong></div>
        </div>
        <small class="text-muted">Name and address are managed by the platform administrator.</small>
      </div></div>

      <form class="card" (ngSubmit)="save()"><div class="card-body">
        <span class="card-header-title">Fees</span>
        <div class="row">
          <div class="form-group col-md-6">
            <label for="fee">Default nikah fee (USD)</label>
            <input id="fee" class="form-control" name="fee" type="number" inputmode="decimal" min="0" max="100000" step="0.01"
                   [(ngModel)]="fee" />
            <small class="text-muted">Charged per nikah; 0 means no fee. Nikahs that already have a payment or waiver keep their fee.</small>
          </div>
          <div class="form-group col-md-6">
            <label for="don">Suggested donation (USD, optional)</label>
            <input id="don" class="form-control" name="don" type="number" inputmode="decimal" min="0" max="100000" step="0.01"
                   [(ngModel)]="donation" />
            <small class="text-muted">Pre-filled when recording a donation.</small>
          </div>
        </div>
        <button class="btn btn-primary btn-responsive" [disabled]="busy()"><i class="mdi mdi-content-save"></i> Save settings</button>
      </div></form>
    }
  `,
})
export class MasjidSettingsPage {
  private http = inject(HttpClient);

  m = signal<MasjidSettings | null>(null);
  fee: number | null = 0;
  donation: number | null = 0;
  busy = signal(false);
  message = signal('');
  error = signal('');

  constructor() {
    this.http.get<MasjidSettings>('/api/imam/masjid').subscribe({
      next: (r) => this.load(r),
      error: (e) => this.error.set(errorMessage(e)),
    });
  }

  private load(r: MasjidSettings) {
    this.m.set(r);
    this.fee = r.nikahFeeCents / 100;
    this.donation = r.suggestedDonationCents / 100;
  }

  address(m: MasjidSettings): string {
    return [m.addressLine, m.city, [m.state, m.zip].filter(Boolean).join(' ')].filter(Boolean).join(', ');
  }

  save() {
    this.message.set('');
    this.error.set('');
    const fee = toCents(this.fee ?? 0, true);
    const don = toCents(this.donation ?? 0, true);
    if (fee === null || don === null) {
      this.error.set('Enter amounts between $0 and $100,000.');
      return;
    }
    this.busy.set(true);
    this.http.put<MasjidSettings>('/api/imam/masjid', { ...this.m(), nikahFeeCents: fee, suggestedDonationCents: don }).subscribe({
      next: (r) => {
        this.load(r);
        this.busy.set(false);
        this.message.set(`Saved. Default fee is ${dollars(r.nikahFeeCents)}.`);
      },
      error: (e) => {
        this.busy.set(false);
        this.error.set(errorMessage(e));
      },
    });
  }
}
