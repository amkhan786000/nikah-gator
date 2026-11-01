import { HttpClient } from '@angular/common/http';
import { DatePipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { errorMessage } from '../../../core/auth.service';
import { downloadPdf, printPdf } from '../pdf-file';
import { CS_API, ServiceCert } from './cs.model';

@Component({
  selector: 'app-cs-view',
  imports: [FormsModule, RouterLink, DatePipe],
  template: `
    <div class="page-title-box d-flex flex-wrap align-items-center justify-content-between">
      <h4 class="page-title">Community service certificate</h4>
      <a class="btn btn-secondary btn-responsive" routerLink="/imam/svc/community-service"><i class="mdi mdi-arrow-left"></i> All certificates</a>
    </div>
    @if (error()) { <div class="alert alert-danger">{{ error() }}</div> }

    @if (c(); as c) {
      @if (c.status === 'VOID') {
        <div class="alert alert-secondary"><strong>Voided.</strong> {{ c.voidReason }}</div>
      }
      <div class="card"><div class="card-body">
        <div class="detail-grid">
          <div><span>Certificate number</span><strong>{{ c.certNumber }}</strong><small>Issued {{ c.issueDate | date: 'longDate' }}</small></div>
          <div><span>Name</span><strong>{{ c.personName }}</strong><small>{{ c.personType === 'STUDENT' ? 'Student' : 'Volunteer' }}@if (c.organization) { , {{ c.organization }} }</small></div>
          <div><span>Hours</span><strong>{{ c.hours }}</strong></div>
          <div><span>Period</span><strong>{{ c.startDate | date: 'longDate' }} to {{ c.endDate | date: 'longDate' }}</strong></div>
          <div><span>Supervisor</span><strong>{{ c.supervisor }}</strong></div>
        </div>
        <p class="mt-3 mb-1 text-muted">Activities</p>
        <p style="white-space:pre-line">{{ c.activities }}</p>
        @if (c.notes) { <p class="mb-0"><span class="text-muted">Notes:</span> {{ c.notes }}</p> }

        <div class="d-flex flex-wrap mt-3" style="gap:.4rem">
          <button class="btn btn-primary btn-responsive" (click)="download()" [disabled]="busy()"><i class="mdi mdi-file-pdf"></i> Download PDF</button>
          <button class="btn btn-secondary btn-responsive" (click)="print()" [disabled]="busy()"><i class="mdi mdi-printer"></i> Print</button>
          @if (c.status === 'VALID') {
            <a class="btn btn-secondary btn-responsive" [routerLink]="['/imam/svc/community-service', c.id, 'edit']"><i class="mdi mdi-pencil"></i> Edit</a>
            <button class="btn btn-outline-danger btn-responsive" (click)="voiding.set(true)">Void</button>
          }
        </div>

        @if (voiding()) {
          <form class="mt-3 p-3 border rounded" (ngSubmit)="confirmVoid()">
            <strong>Void this certificate</strong>
            <div class="form-group mt-2"><label>Reason</label><input class="form-control" name="reason" maxlength="500" [(ngModel)]="reason" required /></div>
            <div class="d-flex flex-wrap" style="gap:.4rem">
              <button class="btn btn-danger btn-responsive" [disabled]="busy() || !reason.trim()">Void certificate</button>
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
export class CsView {
  private http = inject(HttpClient);
  private id = inject(ActivatedRoute).snapshot.paramMap.get('id');
  private url = `${CS_API}/${this.id}`;

  c = signal<ServiceCert | null>(null);
  busy = signal(false);
  error = signal('');
  voiding = signal(false);
  reason = '';

  constructor() {
    this.http.get<ServiceCert>(this.url).subscribe({
      next: (r) => this.c.set(r),
      error: (e) => this.error.set(errorMessage(e)),
    });
  }

  download() {
    this.run(downloadPdf(this.http, `${this.url}/pdf`, `${this.c()?.certNumber ?? 'certificate'}.pdf`));
  }

  print() {
    this.run(printPdf(this.http, `${this.url}/pdf`));
  }

  confirmVoid() {
    this.busy.set(true);
    this.error.set('');
    this.http.post<ServiceCert>(`${this.url}/void`, { reason: this.reason }).subscribe({
      next: (r) => {
        this.c.set(r);
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
