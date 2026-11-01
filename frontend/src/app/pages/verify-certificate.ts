import { HttpClient } from '@angular/common/http';
import { DatePipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { AuthLayout } from '../shared/auth-layout';

interface Verification {
  valid: boolean;
  status: string;
  masjidName: string;
  city: string | null;
  state: string | null;
  nikahDate: string;
  groomFirstName: string;
  brideFirstName: string;
}

/** Public page (no sign-in): confirms a certificate number, showing only minimal details. */
@Component({
  selector: 'app-verify-certificate',
  imports: [AuthLayout, DatePipe],
  template: `
    <app-auth-layout heading="Verify a certificate" [subheading]="'Certificate ' + number">
      @if (loading()) {
        <p class="text-muted">Checking…</p>
      } @else if (notFound()) {
        <div class="alert alert-danger">No certificate was found with this number. Check the number printed on the certificate.</div>
      } @else if (error()) {
        <div class="alert alert-danger">{{ error() }}</div>
      } @else if (v(); as v) {
        @if (v.valid) {
          <div class="alert alert-success"><i class="mdi mdi-check-decagram"></i> This certificate is valid.</div>
        } @else {
          <div class="alert alert-danger"><i class="mdi mdi-close-circle"></i> This certificate has been voided and is no longer valid.</div>
        }
        <div class="detail-grid">
          <div><span>Masjid</span><strong>{{ v.masjidName }}</strong><small>{{ place(v) }}</small></div>
          <div><span>Nikah date</span><strong>{{ v.nikahDate | date: 'longDate' }}</strong></div>
          <div><span>Couple</span><strong>{{ v.groomFirstName }} &amp; {{ v.brideFirstName }}</strong></div>
        </div>
      }
    </app-auth-layout>
  `,
})
export class VerifyCertificate {
  private http = inject(HttpClient);
  number = inject(ActivatedRoute).snapshot.paramMap.get('number') ?? '';

  v = signal<Verification | null>(null);
  loading = signal(true);
  notFound = signal(false);
  error = signal('');

  constructor() {
    this.http.get<Verification>(`/api/public/certificates/${encodeURIComponent(this.number)}`).subscribe({
      next: (r) => {
        this.v.set(r);
        this.loading.set(false);
      },
      error: (e) => {
        this.loading.set(false);
        if (e.status === 404) this.notFound.set(true);
        else this.error.set('Verification is not available right now. Please try again later.');
      },
    });
  }

  place(v: Verification): string {
    return [v.city, v.state].filter((s) => !!s).join(', ');
  }
}
