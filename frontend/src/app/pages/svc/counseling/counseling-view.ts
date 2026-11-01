import { HttpClient } from '@angular/common/http';
import { DatePipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { errorMessage } from '../../../core/auth.service';
import { CounselingDetail, statusBadge } from './counseling.model';

@Component({
  selector: 'app-counseling-view',
  imports: [RouterLink, DatePipe],
  template: `
    @if (error()) { <div class="alert alert-danger">{{ error() }}</div> }
    @if (s(); as s) {
      <div class="page-title-box d-flex flex-wrap align-items-center justify-content-between">
        <h4 class="page-title">{{ s.clientName }}</h4>
        <div class="d-flex flex-wrap" style="gap:.4rem">
          <a class="btn btn-secondary" [routerLink]="['/imam/svc/counseling', s.id, 'edit']"><i class="mdi mdi-pencil"></i> Edit</a>
          <button class="btn btn-outline-danger" (click)="remove()"><i class="mdi mdi-delete-outline"></i> Delete</button>
          <a class="btn btn-secondary" routerLink="/imam/svc/counseling">Back</a>
        </div>
      </div>
      <div class="card"><div class="card-body">
        <span class="card-header-title">Session</span>
        <div class="detail-grid">
          <div><span>Type</span><strong>{{ s.sessionType }}</strong></div>
          <div><span>When</span><strong>{{ s.sessionAt | date: 'medium' }}</strong></div>
          <div><span>Duration</span><strong>{{ s.durationMin ? s.durationMin + ' min' : '—' }}</strong></div>
          <div><span>Follow-up</span><strong>{{ s.followUpOn ? (s.followUpOn | date: 'mediumDate') : '—' }}</strong></div>
          <div><span>Status</span><strong><span [class]="badge(s.status)">{{ s.status }}</span></strong></div>
        </div>
        <span class="card-header-title mt-3">Private notes</span>
        <p style="white-space:pre-wrap" class="mb-0">{{ s.notes || '—' }}</p>
      </div></div>
    }
  `,
})
export class CounselingView {
  private http = inject(HttpClient);
  private router = inject(Router);
  private id = inject(ActivatedRoute).snapshot.paramMap.get('id');

  s = signal<CounselingDetail | null>(null);
  error = signal('');
  badge = statusBadge;

  constructor() {
    this.http.get<CounselingDetail>(`/api/imam/svc/counseling/${this.id}`).subscribe({
      next: (r) => this.s.set(r),
      error: (e) => this.error.set(errorMessage(e)),
    });
  }

  remove() {
    if (!confirm('Delete this session record? This cannot be undone.')) return;
    this.http.delete(`/api/imam/svc/counseling/${this.id}`).subscribe({
      next: () => this.router.navigate(['/imam/svc/counseling']),
      error: (e) => this.error.set(errorMessage(e)),
    });
  }
}
