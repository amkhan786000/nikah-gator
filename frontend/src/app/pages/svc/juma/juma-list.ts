import { HttpClient } from '@angular/common/http';
import { DatePipe } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { errorMessage } from '../../../core/auth.service';
import { CardTableDirective } from '../../../shared/card-table.directive';
import { JUMA_API, JumaLetter } from './juma.model';

@Component({
  selector: 'app-juma-list',
  imports: [CardTableDirective, RouterLink, DatePipe, FormsModule],
  template: `
    <div class="page-title-box d-flex flex-wrap align-items-center justify-content-between">
      <h4 class="page-title">Juma request letters</h4>
      <a class="btn btn-primary btn-responsive" routerLink="/imam/svc/juma/new"><i class="mdi mdi-plus"></i> New letter</a>
    </div>
    @if (error()) { <div class="alert alert-danger">{{ error() }}</div> }

    <div class="card"><div class="card-body">
      <div class="input-icon mb-3" style="max-width:340px">
        <i class="mdi mdi-magnify"></i>
        <input class="form-control" placeholder="Search school or student…" [ngModel]="q()" (ngModelChange)="q.set($event)" />
      </div>
      <div class="table-responsive">
        <table class="table table-hover mb-0">
          <thead><tr><th>Date</th><th>School</th><th>Students</th><th>Covers</th><th>Status</th><th></th></tr></thead>
          <tbody>
            @for (l of shown(); track l.id) {
              <tr>
                <td class="text-nowrap">{{ l.letterDate | date: 'MMM d, y' }}</td>
                <td>{{ l.schoolName }}</td>
                <td>{{ l.students.length }}</td>
                <td>@if (l.standing) { Standing } @else { {{ l.startDate | date: 'MMM d, y' }} to {{ l.endDate | date: 'MMM d, y' }} }</td>
                <td><span class="badge" [class.badge-success]="l.status === 'ACTIVE'" [class.badge-secondary]="l.status === 'VOID'">{{ l.status }}</span></td>
                <td class="text-right"><a class="btn btn-sm btn-outline-primary" [routerLink]="['/imam/svc/juma', l.id]">Open</a></td>
              </tr>
            } @empty {
              <tr><td colspan="6" class="text-center text-muted py-4">{{ loaded() ? 'No letters yet.' : 'Loading…' }}</td></tr>
            }
          </tbody>
        </table>
      </div>
    </div></div>
  `,
})
export class JumaList {
  private http = inject(HttpClient);

  letters = signal<JumaLetter[]>([]);
  q = signal('');
  loaded = signal(false);
  error = signal('');
  shown = computed(() => {
    const q = this.q().trim().toLowerCase();
    if (!q) return this.letters();
    return this.letters().filter((l) => (l.schoolName + ' ' + l.students.map((s) => s.name).join(' ')).toLowerCase().includes(q));
  });

  constructor() {
    this.http.get<JumaLetter[]>(JUMA_API).subscribe({
      next: (r) => {
        this.letters.set(r);
        this.loaded.set(true);
      },
      error: (e) => this.error.set(errorMessage(e)),
    });
  }
}
