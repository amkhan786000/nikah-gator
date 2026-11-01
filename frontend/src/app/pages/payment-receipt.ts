import { HttpClient } from '@angular/common/http';
import { DatePipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { errorMessage } from '../core/auth.service';
import { dollars } from './payment.model';

interface Receipt {
  receiptNumber: string;
  masjidName: string;
  masjidAddress: string;
  masjidPhone: string | null;
  payerName: string;
  groomName: string;
  brideName: string;
  kind: string;
  amountCents: number;
  currency: string;
  method: string;
  paidAt: string;
  note: string | null;
  receivedByName: string | null;
}

/** Printable receipt (use the browser's print dialog to print or save as PDF). */
@Component({
  selector: 'app-payment-receipt',
  imports: [RouterLink, DatePipe],
  styles: `
    .receipt { max-width: 560px; margin: 0 auto; background: #fff; border: 1px solid #ccd3cc; border-radius: 8px; padding: 28px 24px; }
    .receipt h1 { font-family: Georgia, serif; font-size: 26px; margin: 0 0 2px; }
    .receipt .head { text-align: center; border-bottom: 2px solid #333; padding-bottom: 12px; margin-bottom: 16px; }
    .receipt .row-line { display: flex; justify-content: space-between; gap: 12px; padding: 6px 0; border-bottom: 1px dotted #bbb; }
    .receipt .row-line span:first-child { color: #666; }
    .receipt .row-line strong { text-align: right; overflow-wrap: anywhere; }
    .receipt .total { font-size: 22px; margin-top: 12px; text-align: center; }
    .receipt .sign { margin-top: 36px; text-align: right; font-size: 13px; }
    .receipt .sign .line { display: inline-block; width: 200px; max-width: 100%; border-top: 1px solid #333; margin-bottom: 4px; }
    @media (max-width: 575px) { .receipt { padding: 18px 14px; } }
    @media print { .receipt { border: 0; max-width: none; } }
  `,
  template: `
    @if (error()) { <div class="alert alert-danger">{{ error() }}</div> }
    <div class="page-title-box no-print d-flex flex-wrap align-items-center justify-content-between">
      <h4 class="page-title">Receipt</h4>
      <div class="d-flex flex-wrap" style="gap:.4rem">
        <a class="btn btn-secondary" [routerLink]="['/imam/nikahs', id]"><i class="mdi mdi-arrow-left"></i> Back</a>
        @if (r()) { <button class="btn btn-primary" (click)="print()"><i class="mdi mdi-printer"></i> Print / save PDF</button> }
      </div>
    </div>
    @if (r(); as r) {
      <div class="receipt">
        <div class="head">
          <h1>{{ r.masjidName }}</h1>
          <div>{{ r.masjidAddress }}@if (r.masjidPhone) { · {{ r.masjidPhone }} }</div>
          <strong>{{ r.kind === 'FEE' ? 'Payment receipt' : 'Donation receipt' }}</strong>
        </div>
        <div class="row-line"><span>Receipt no.</span><strong>{{ r.receiptNumber }}</strong></div>
        <div class="row-line"><span>Date received</span><strong>{{ r.paidAt | date: 'longDate' }}</strong></div>
        <div class="row-line"><span>Received from</span><strong>{{ r.payerName }}</strong></div>
        <div class="row-line"><span>For</span><strong>{{ r.kind === 'FEE' ? 'Nikah fee' : 'Donation' }}: {{ r.groomName }} &amp; {{ r.brideName }}</strong></div>
        <div class="row-line"><span>Method</span><strong>{{ r.method }}</strong></div>
        @if (r.note) { <div class="row-line"><span>Note</span><strong>{{ r.note }}</strong></div> }
        <div class="total">Amount: <strong>{{ money(r.amountCents) }}</strong> {{ r.currency }}</div>
        <div class="sign"><span class="line"></span><br />Received by {{ r.receivedByName || 'Imam' }}</div>
      </div>
    }
  `,
})
export class PaymentReceipt {
  private http = inject(HttpClient);
  private route = inject(ActivatedRoute).snapshot.paramMap;
  id = this.route.get('id');

  r = signal<Receipt | null>(null);
  error = signal('');
  money = dollars;

  constructor() {
    this.http.get<Receipt>(`/api/imam/nikahs/${this.id}/payments/${this.route.get('pid')}/receipt`).subscribe({
      next: (r) => this.r.set(r),
      error: (e) => this.error.set(errorMessage(e)),
    });
  }

  print() {
    window.print();
  }
}
