import { HttpClient } from '@angular/common/http';
import { DatePipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { AuthService, errorMessage } from '../core/auth.service';
import { Nikah } from './nikah.model';
import { NikahCertificate } from './nikah-certificate';
import { NikahDocuments } from './nikah-documents';
import { NikahNotifications } from './nikah-notifications';
import { NikahPayments } from './nikah-payments';

@Component({
  selector: 'app-nikah-view',
  imports: [RouterLink, DatePipe, NikahCertificate, NikahDocuments, NikahNotifications, NikahPayments],
  template: `
    @if (error()) { <div class="alert alert-danger">{{ error() }}</div> }
    @if (n(); as n) {
      <div class="page-title-box no-print d-flex flex-wrap align-items-center justify-content-between">
        <h4 class="page-title">{{ n.groomName }} &amp; {{ n.brideName }}</h4>
        <div class="d-flex flex-wrap" style="gap:.4rem">
          <a class="btn btn-secondary" [routerLink]="['/imam/nikahs', n.id, 'edit']"><i class="mdi mdi-pencil"></i> Edit</a>
          <button class="btn btn-secondary" (click)="setStatus('COMPLETED')" [disabled]="n.status === 'COMPLETED'">Mark completed</button>
          <button class="btn btn-secondary" (click)="setStatus('CANCELLED')" [disabled]="n.status === 'CANCELLED'">Cancel nikah</button>
        </div>
      </div>

      <div class="card no-print"><div class="card-body">
        <span class="card-header-title">Details</span>
        <div class="detail-grid">
          <div><span>Status</span><strong>{{ n.status }}</strong></div>
          <div><span>Groom contact</span><strong>{{ n.groomPhone || '—' }}</strong><small>{{ n.groomEmail }}</small></div>
          <div><span>Bride contact</span><strong>{{ n.bridePhone || '—' }}</strong><small>{{ n.brideEmail }}</small></div>
          <div><span>Registered</span><strong>{{ n.createdAt | date: 'medium' }}</strong></div>
          <div><span>Notes</span><strong>{{ n.notes || '—' }}</strong></div>
        </div>
      </div></div>
      <!-- feature:certificates -->
      <app-nikah-certificate [nikahId]="n.id!" [status]="n.status ?? ''" />
      <!-- /feature:certificates -->
      <!-- feature:documents -->
      <app-nikah-documents [nikahId]="n.id!" />
      <!-- /feature:documents -->
      <!-- feature:payments -->
      <app-nikah-payments [nikahId]="n.id!" />
      <!-- /feature:payments -->
      <!-- feature:notifications -->
      <app-nikah-notifications [nikahId]="n.id!" [refresh]="n.status + n.nikahAt" />
      <!-- /feature:notifications -->
    }
  `,
})
export class NikahView {
  private http = inject(HttpClient);
  protected auth = inject(AuthService);
  private id = inject(ActivatedRoute).snapshot.paramMap.get('id');

  n = signal<Nikah | null>(null);
  error = signal('');

  constructor() {
    this.http.get<Nikah>(`/api/imam/nikahs/${this.id}`).subscribe({
      next: (r) => this.n.set(r),
      error: (e) => this.error.set(errorMessage(e)),
    });
  }

  setStatus(status: string) {
    const n = this.n();
    if (!n) return;
    this.http.put<Nikah>(`/api/imam/nikahs/${this.id}`, { ...n, nikahAt: n.nikahAt.slice(0, 16), status }).subscribe({
      next: (r) => this.n.set(r),
      error: (e) => this.error.set(errorMessage(e)),
    });
  }
}
