import { HttpClient } from '@angular/common/http';
import { DatePipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { AuthService, errorMessage } from '../core/auth.service';
import { Nikah } from './nikah.model';
import { NikahDocuments } from './nikah-documents';
import { NikahNotifications } from './nikah-notifications';
import { NikahPayments } from './nikah-payments';

@Component({
  selector: 'app-nikah-view',
  imports: [RouterLink, DatePipe, NikahDocuments, NikahNotifications, NikahPayments],
  template: `
    @if (error()) { <div class="alert alert-danger">{{ error() }}</div> }
    @if (n(); as n) {
      <div class="page-title-box no-print d-flex flex-wrap align-items-center justify-content-between">
        <h4 class="page-title">{{ n.groomName }} &amp; {{ n.brideName }}</h4>
        <div class="d-flex flex-wrap" style="gap:.4rem">
          <a class="btn btn-secondary" [routerLink]="['/imam/nikahs', n.id, 'edit']"><i class="mdi mdi-pencil"></i> Edit</a>
          <button class="btn btn-secondary" (click)="setStatus('COMPLETED')" [disabled]="n.status === 'COMPLETED'">Mark completed</button>
          <button class="btn btn-secondary" (click)="setStatus('CANCELLED')" [disabled]="n.status === 'CANCELLED'">Cancel nikah</button>
          <button class="btn btn-primary" (click)="print()"><i class="mdi mdi-printer"></i> Print certificate</button>
        </div>
      </div>

      <div class="certificate">
        <p class="bismillah">بسم الله الرحمن الرحيم</p>
        <h1>Nikah Certificate</h1>
        <p class="center">{{ auth.me()?.masjidName }}<br /><small>{{ auth.me()?.masjidAddress }}</small></p>
        <p>This is to certify that the marriage (<em>nikah</em>) of</p>
        <p class="names"><strong>{{ n.groomName }}</strong>@if (n.groomFatherName) { <small> son of {{ n.groomFatherName }}</small> }</p>
        <p>with</p>
        <p class="names"><strong>{{ n.brideName }}</strong>@if (n.brideFatherName) { <small> daughter of {{ n.brideFatherName }}</small> }</p>
        <p>was solemnized according to the Islamic rites on <strong>{{ n.nikahAt | date: 'fullDate' }}</strong> at {{ n.nikahAt | date: 'shortTime' }}@if (n.mahrDescription) { , with mahr: <strong>{{ n.mahrDescription }}</strong> }.</p>
        @if (n.waliName || n.witness1Name || n.witness2Name) {
          <p><small>
            @if (n.waliName) { Wali: {{ n.waliName }}@if (n.waliRelation) { ({{ n.waliRelation }}) }. }
            @if (n.witness1Name || n.witness2Name) { Witnesses: {{ witnesses(n) }}. }
          </small></p>
        }
        <div class="signatures">
          <div><span class="line"></span>Imam / Officiant<br />{{ auth.me()?.name }}</div>
          <div><span class="line"></span>Date</div>
        </div>
        <p><small>Record no. {{ n.id }}</small></p>
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

  witnesses(n: Nikah): string {
    return [n.witness1Name, n.witness2Name].filter((w) => !!w).join(' and ');
  }

  print() {
    window.print();
  }
}
