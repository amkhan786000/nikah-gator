import { HttpClient } from '@angular/common/http';
import { DatePipe } from '@angular/common';
import { CardTableDirective } from '../shared/card-table.directive';
import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { errorMessage } from '../core/auth.service';
import { Nikah } from './nikah.model';

@Component({
  selector: 'app-nikah-list',
  imports: [CardTableDirective, RouterLink, DatePipe, FormsModule],
  template: `
    <div class="page-title-box d-flex flex-wrap align-items-center justify-content-between">
      <h4 class="page-title">Nikahs</h4>
      <a class="btn btn-primary btn-responsive" routerLink="/imam/nikahs/new"><i class="mdi mdi-plus"></i> Register nikah</a>
    </div>
    @if (error()) { <div class="alert alert-danger">{{ error() }}</div> }

    <div class="card"><div class="card-body">
      <div class="input-icon mb-3" style="max-width:340px">
        <i class="mdi mdi-magnify"></i>
        <input class="form-control" placeholder="Search by name…" [ngModel]="q()" (ngModelChange)="q.set($event)" />
      </div>
      <div class="table-responsive">
        <table class="table table-hover mb-0">
          <thead><tr><th>Date</th><th>Groom</th><th>Bride</th><th>Checklist</th><th>Status</th><th></th></tr></thead>
          <tbody>
            @for (n of shown(); track n.id) {
              <tr>
                <td class="text-nowrap">{{ n.nikahAt | date: 'MMM d, y, h:mm a' }}</td>
                <td>{{ n.groomName }}</td>
                <td>{{ n.brideName }}</td>
                <!-- feature:checklist-progress -->
                <td>@if (progress()[n.id!]; as p) { <span class="badge" [class.badge-success]="p.done === p.total" [class.badge-warning]="p.done !== p.total">{{ p.done }}/{{ p.total }}</span> }</td>
                <!-- /feature:checklist-progress -->
                <td>
                  <span class="badge" [class.badge-success]="n.status === 'COMPLETED'" [class.badge-info]="n.status === 'REGISTERED'"
                        [class.badge-secondary]="n.status === 'CANCELLED'">{{ n.status }}</span>
                </td>
                <td class="text-right"><a class="btn btn-sm btn-outline-primary" [routerLink]="['/imam/nikahs', n.id]">Open</a></td>
              </tr>
            } @empty {
              <tr><td colspan="6"class="text-center text-muted py-4">{{ loaded() ? 'No nikahs found.' : 'Loading…' }}</td></tr>
            }
          </tbody>
        </table>
      </div>
    </div></div>
  `,
})
export class NikahList {
  private http = inject(HttpClient);

  nikahs = signal<Nikah[]>([]);
  q = signal('');
  loaded = signal(false);
  error = signal('');
  shown = computed(() => {
    const q = this.q().trim().toLowerCase();
    return q ? this.nikahs().filter((n) => (n.groomName + ' ' + n.brideName).toLowerCase().includes(q)) : this.nikahs();
  });

  progress = signal<Record<number, { done: number; total: number }>>({});

  constructor() {
    this.http.get<{ nikahId: number; done: number; total: number }[]>('/api/imam/checklist-progress').subscribe({
      next: (r) => this.progress.set(Object.fromEntries(r.map((p) => [p.nikahId, p]))),
    });
    this.http.get<Nikah[]>('/api/imam/nikahs').subscribe({
      next: (r) => {
        this.nikahs.set(r);
        this.loaded.set(true);
      },
      error: (e) => this.error.set(errorMessage(e)),
    });
  }
}
