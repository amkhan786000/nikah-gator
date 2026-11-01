import { HttpClient } from '@angular/common/http';
import { DatePipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { errorMessage } from '../../../core/auth.service';
import { downloadPdf, printPdf } from '../pdf-file';
import { DIVORCE_API, Divorce, talaqLabel, typeLabel } from './divorce.model';
import { SendEmail } from '../../../shared/send-email';

@Component({
  selector: 'app-divorce-view',
  imports: [FormsModule, RouterLink, DatePipe, SendEmail],
  template: `
    <div class="page-title-box d-flex flex-wrap align-items-center justify-content-between">
      <h4 class="page-title">Divorce record</h4>
      <a class="btn btn-secondary btn-responsive" routerLink="/imam/svc/divorce"><i class="mdi mdi-arrow-left"></i> All records</a>
    </div>
    @if (error()) { <div class="alert alert-danger">{{ error() }}</div> }

    @if (d(); as d) {
      @if (d.voided) {
        <div class="alert alert-secondary"><strong>Voided.</strong> {{ d.voidReason }}</div>
      }
      <div class="card"><div class="card-body">
        <div class="detail-grid">
          <div><span>Record number</span><strong>{{ d.number }}</strong><small>{{ d.status === 'REVOKED' ? 'Revoked (reconciled)' : 'Recorded' }}</small></div>
          <div><span>Husband</span><strong>{{ d.groomName }}</strong>@if (d.groomFatherName) { <small>son of {{ d.groomFatherName }}</small> }</div>
          <div><span>Wife</span><strong>{{ d.brideName }}</strong>@if (d.brideFatherName) { <small>daughter of {{ d.brideFatherName }}</small> }</div>
          <div><span>Type</span><strong>{{ typeLabel(d.divorceType) }}</strong>@if (d.talaqNumber) { <small>{{ talaqLabel(d.talaqNumber) }}</small> }</div>
          <div><span>Date of divorce</span><strong>{{ d.divorceDate | date: 'longDate' }}</strong></div>
          @if (d.iddahEndDate) { <div><span>Iddah ends</span><strong>{{ d.iddahEndDate | date: 'longDate' }}</strong></div> }
          @if (d.status === 'REVOKED' && d.revokedDate) { <div><span>Reconciled on</span><strong>{{ d.revokedDate | date: 'longDate' }}</strong></div> }
          @if (d.marriageDate || d.marriagePlace) {
            <div><span>Marriage</span><strong>{{ d.marriageDate ? (d.marriageDate | date: 'longDate') : '' }}</strong><small>{{ d.marriagePlace }}</small></div>
          }
          @if (d.witness1Name) { <div><span>Witnesses</span><strong>{{ d.witness1Name }}@if (d.witness2Name) { , {{ d.witness2Name }} }</strong></div> }
          @if (d.nikahId) { <div><span>Nikah</span><strong><a [routerLink]="['/imam/nikahs', d.nikahId]">Open the nikah</a></strong></div> }
        </div>
        @if (d.settlementNotes) { <p class="mt-3 mb-1 text-muted">Mahr and settlement</p><p style="white-space:pre-line">{{ d.settlementNotes }}</p> }
        @if (d.reconciliationNotes) { <p class="mb-1 text-muted">Reconciliation attempts</p><p style="white-space:pre-line">{{ d.reconciliationNotes }}</p> }
        @if (d.privateNotes) { <p class="mb-1 text-muted">Private notes (not printed)</p><p style="white-space:pre-line">{{ d.privateNotes }}</p> }
        <p class="small text-muted mb-0">A religious record only. It does not replace a civil divorce decree.</p>

        <div class="d-flex flex-wrap mt-3" style="gap:.4rem">
          <button class="btn btn-primary btn-responsive" (click)="download()" [disabled]="busy()"><i class="mdi mdi-file-pdf"></i> Download PDF</button>
          <button class="btn btn-secondary btn-responsive" (click)="print()" [disabled]="busy()"><i class="mdi mdi-printer"></i> Print</button>
          @if (!d.voided) {
            <app-send-email [url]="url + '/email'" [sensitive]="true" noun="this divorce record" />
            <a class="btn btn-secondary btn-responsive" [routerLink]="['/imam/svc/divorce', d.id, 'edit']"><i class="mdi mdi-pencil"></i> Edit</a>
            <button class="btn btn-outline-danger btn-responsive" (click)="voiding.set(true)">Void</button>
          }
        </div>

        @if (voiding()) {
          <form class="mt-3 p-3 border rounded" (ngSubmit)="confirmVoid()">
            <strong>Void this record</strong>
            <div class="form-group mt-2"><label>Reason</label><input class="form-control" name="reason" maxlength="500" [(ngModel)]="reason" required /></div>
            <div class="d-flex flex-wrap" style="gap:.4rem">
              <button class="btn btn-danger btn-responsive" [disabled]="busy() || !reason.trim()">Void record</button>
              <button type="button" class="btn btn-secondary btn-responsive" (click)="voiding.set(false)">Cancel</button>
            </div>
          </form>
        }
      </div></div>
    } @else if (!error()) {
      <p class="text-muted">Loading…</p>
    }
  `,
})
export class DivorceView {
  private http = inject(HttpClient);
  private id = inject(ActivatedRoute).snapshot.paramMap.get('id');
  url = `${DIVORCE_API}/${this.id}`;

  typeLabel = typeLabel;
  talaqLabel = talaqLabel;
  d = signal<Divorce | null>(null);
  busy = signal(false);
  error = signal('');
  voiding = signal(false);
  reason = '';

  constructor() {
    this.http.get<Divorce>(this.url).subscribe({
      next: (r) => this.d.set(r),
      error: (e) => this.error.set(errorMessage(e)),
    });
  }

  download() {
    this.run(downloadPdf(this.http, `${this.url}/pdf`, `${this.d()?.number ?? 'divorce-record'}.pdf`));
  }

  print() {
    this.run(printPdf(this.http, `${this.url}/pdf`));
  }

  confirmVoid() {
    this.busy.set(true);
    this.error.set('');
    this.http.post<Divorce>(`${this.url}/void`, { reason: this.reason }).subscribe({
      next: (r) => {
        this.d.set(r);
        this.voiding.set(false);
        this.busy.set(false);
      },
      error: (e) => {
        this.busy.set(false);
        this.error.set(errorMessage(e));
      },
    });
  }

  private run(req: import('rxjs').Observable<void>) {
    this.busy.set(true);
    this.error.set('');
    req.subscribe({
      next: () => this.busy.set(false),
      error: (e) => {
        this.busy.set(false);
        this.error.set(errorMessage(e));
      },
    });
  }
}
