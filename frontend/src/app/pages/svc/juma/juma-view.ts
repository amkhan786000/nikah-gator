import { AuditInfoView } from '../../../shared/audit-info';
import { HttpClient } from '@angular/common/http';
import { DatePipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { errorMessage } from '../../../core/auth.service';
import { CardTableDirective } from '../../../shared/card-table.directive';
import { downloadPdf, printPdf } from '../pdf-file';
import { JUMA_API, JumaLetter, clock } from './juma.model';

@Component({
  selector: 'app-juma-view',
  imports: [AuditInfoView, FormsModule, RouterLink, DatePipe, CardTableDirective],
  template: `
    <div class="page-title-box d-flex flex-wrap align-items-center justify-content-between">
      <h4 class="page-title">Juma letter</h4>
      <a class="btn btn-secondary btn-responsive" routerLink="/imam/svc/juma"><i class="mdi mdi-arrow-left"></i> All letters</a>
    </div>
    @if (error()) { <div class="alert alert-danger">{{ error() }}</div> }

    @if (l(); as l) {
      @if (l.status === 'VOID') {
        <div class="alert alert-secondary"><strong>Voided.</strong> {{ l.voidReason }}</div>
      }
      <div class="card"><div class="card-body">
        <div class="detail-grid">
          <div><span>School</span><strong>{{ l.schoolName }}</strong><small>{{ l.schoolAddress }}</small></div>
          <div><span>Addressed to</span><strong>{{ l.addressee }}</strong><small>{{ l.schoolContact }}</small></div>
          <div><span>Letter date</span><strong>{{ l.letterDate | date: 'longDate' }}</strong></div>
          <div>
            <span>Covers</span>
            <strong>@if (l.standing) { From {{ l.startDate | date: 'longDate' }}, until further notice } @else { {{ l.startDate | date: 'longDate' }} to {{ l.endDate | date: 'longDate' }} }</strong>
          </div>
          <div><span>Friday window</span><strong>{{ clock(l.windowStart) }} to {{ clock(l.windowEnd) }}</strong></div>
        </div>
        @if (l.note) { <p class="mt-3 mb-0"><span class="text-muted">Note:</span> {{ l.note }}</p> }

        <div class="table-responsive mt-3">
          <table class="table mb-0">
            <thead><tr><th>Student</th><th>Grade</th></tr></thead>
            <tbody>@for (s of l.students; track $index) { <tr><td>{{ s.name }}</td><td>{{ s.grade }}</td></tr> }</tbody>
          </table>
        </div>

        <div class="d-flex flex-wrap mt-3" style="gap:.4rem">
          <button class="btn btn-primary btn-responsive" (click)="download()" [disabled]="busy()"><i class="mdi mdi-file-pdf"></i> Download PDF</button>
          <button class="btn btn-secondary btn-responsive" (click)="print()" [disabled]="busy()"><i class="mdi mdi-printer"></i> Print</button>
          @if (l.status === 'ACTIVE') {
            <a class="btn btn-secondary btn-responsive" [routerLink]="['/imam/svc/juma', l.id, 'edit']"><i class="mdi mdi-pencil"></i> Edit</a>
            <button class="btn btn-outline-danger btn-responsive" (click)="voiding.set(true)">Void</button>
          }
        </div>

        @if (voiding()) {
          <form class="mt-3 p-3 border rounded" (ngSubmit)="confirmVoid()">
            <strong>Void this letter</strong>
            <div class="form-group mt-2"><label>Reason</label><input class="form-control" name="reason" maxlength="500" [(ngModel)]="reason" required /></div>
            <div class="d-flex flex-wrap" style="gap:.4rem">
              <button class="btn btn-danger btn-responsive" [disabled]="busy() || !reason.trim()">Void letter</button>
              <button type="button" class="btn btn-secondary btn-responsive" (click)="voiding.set(false)">Cancel</button>
            </div>
          </form>
        }
      </div></div>
      <app-audit-info type="JumaLetter" [id]="l.id" />
    } @else if (!error()) {
      <p class="text-muted">Loading…</p>
    }
  `,
})
export class JumaView {
  private http = inject(HttpClient);
  private id = inject(ActivatedRoute).snapshot.paramMap.get('id');
  private url = `${JUMA_API}/${this.id}`;

  l = signal<JumaLetter | null>(null);
  busy = signal(false);
  error = signal('');
  voiding = signal(false);
  reason = '';
  clock = clock;

  constructor() {
    this.http.get<JumaLetter>(this.url).subscribe({
      next: (r) => this.l.set(r),
      error: (e) => this.error.set(errorMessage(e)),
    });
  }

  download() {
    this.run(downloadPdf(this.http, `${this.url}/pdf`, `juma-letter-${this.id}.pdf`));
  }

  print() {
    this.run(printPdf(this.http, `${this.url}/pdf`));
  }

  confirmVoid() {
    this.busy.set(true);
    this.error.set('');
    this.http.post<JumaLetter>(`${this.url}/void`, { reason: this.reason }).subscribe({
      next: (r) => {
        this.l.set(r);
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
