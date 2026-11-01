import { HttpClient } from '@angular/common/http';
import { DatePipe } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { errorMessage } from '../../../core/auth.service';
import { CardTableDirective } from '../../../shared/card-table.directive';
import { CounselingRow, statusBadge } from './counseling.model';

@Component({
  selector: 'app-counseling-list',
  imports: [CardTableDirective, RouterLink, DatePipe, FormsModule],
  template: `
    <div class="page-title-box d-flex flex-wrap align-items-center justify-content-between">
      <h4 class="page-title">Counseling sessions</h4>
      <a class="btn btn-primary btn-responsive" routerLink="/imam/svc/counseling/new"><i class="mdi mdi-plus"></i> Log session</a>
    </div>
    @if (error()) { <div class="alert alert-danger">{{ error() }}</div> }

    <div class="alert alert-light border"><i class="mdi mdi-lock-outline"></i> Private: only the imam of this masjid can see these sessions. Notes are shown only when a session is opened.</div>

    @if (followUps().length) {
      <div class="card"><div class="card-body">
        <span class="card-header-title">Upcoming follow-ups</span>
        <ul class="list-unstyled mb-0">
          @for (f of followUps(); track f.id) {
            <li class="py-1"><a [routerLink]="['/imam/svc/counseling', f.id]">{{ f.clientName }}</a>
              <span class="text-muted"> — {{ f.followUpOn | date: 'EEE, MMM d' }}</span></li>
          }
        </ul>
      </div></div>
    }

    <div class="card"><div class="card-body">
      <div class="input-icon mb-3" style="max-width:340px">
        <i class="mdi mdi-magnify"></i>
        <input class="form-control" placeholder="Search by client name…" [ngModel]="q()" (ngModelChange)="q.set($event)" />
      </div>
      <div class="table-responsive">
        <table class="table table-hover mb-0">
          <thead><tr><th>Date</th><th>Client</th><th>Type</th><th>Duration</th><th>Follow-up</th><th>Status</th><th></th></tr></thead>
          <tbody>
            @for (s of shown(); track s.id) {
              <tr>
                <td class="text-nowrap">{{ s.sessionAt | date: 'MMM d, y, h:mm a' }}</td>
                <td>{{ s.clientName }}</td>
                <td>{{ s.sessionType }}</td>
                <td>{{ s.durationMin ? s.durationMin + ' min' : '' }}</td>
                <td class="text-nowrap">{{ s.followUpOn ? (s.followUpOn | date: 'MMM d, y') : '' }}</td>
                <td><span [class]="badge(s.status)">{{ s.status }}</span></td>
                <td class="text-right"><a class="btn btn-sm btn-outline-primary" [routerLink]="['/imam/svc/counseling', s.id]">Open</a></td>
              </tr>
            } @empty {
              <tr><td colspan="7" class="text-center text-muted py-4">{{ loaded() ? 'No sessions found.' : 'Loading…' }}</td></tr>
            }
          </tbody>
        </table>
      </div>
    </div></div>
  `,
})
export class CounselingList {
  private http = inject(HttpClient);

  rows = signal<CounselingRow[]>([]);
  followUps = signal<CounselingRow[]>([]);
  q = signal('');
  loaded = signal(false);
  error = signal('');
  badge = statusBadge;
  shown = computed(() => {
    const q = this.q().trim().toLowerCase();
    return q ? this.rows().filter((r) => r.clientName.toLowerCase().includes(q)) : this.rows();
  });

  constructor() {
    this.http.get<CounselingRow[]>('/api/imam/svc/counseling').subscribe({
      next: (r) => {
        this.rows.set(r);
        this.loaded.set(true);
      },
      error: (e) => this.error.set(errorMessage(e)),
    });
    this.http.get<CounselingRow[]>('/api/imam/svc/counseling/follow-ups').subscribe({ next: (r) => this.followUps.set(r) });
  }
}
