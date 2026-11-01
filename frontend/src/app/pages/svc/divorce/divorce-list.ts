import { HttpClient } from '@angular/common/http';
import { DatePipe } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { errorMessage } from '../../../core/auth.service';
import { CardTableDirective } from '../../../shared/card-table.directive';
import { DIVORCE_API, Divorce, talaqLabel, typeLabel } from './divorce.model';

@Component({
  selector: 'app-divorce-list',
  imports: [CardTableDirective, RouterLink, DatePipe, FormsModule],
  template: `
    <div class="page-title-box d-flex flex-wrap align-items-center justify-content-between">
      <h4 class="page-title">Divorce records</h4>
      <a class="btn btn-primary btn-responsive" routerLink="/imam/svc/divorce/new"><i class="mdi mdi-plus"></i> New divorce record</a>
    </div>
    @if (error()) { <div class="alert alert-danger">{{ error() }}</div> }
    <p class="text-muted small">Private religious records. They are only visible to the imam of this masjid and do not replace a civil divorce decree.</p>

    <div class="card"><div class="card-body">
      <div class="input-icon mb-3" style="max-width:340px">
        <i class="mdi mdi-magnify"></i>
        <input class="form-control" placeholder="Search name or number…" [ngModel]="q()" (ngModelChange)="q.set($event)" />
      </div>
      <div class="table-responsive">
        <table class="table table-hover mb-0">
          <thead><tr><th>Number</th><th>Couple</th><th>Type</th><th>Date</th><th>Status</th><th></th></tr></thead>
          <tbody>
            @for (d of shown(); track d.id) {
              <tr>
                <td class="text-nowrap">{{ d.number }}</td>
                <td>{{ d.groomName }} &amp; {{ d.brideName }}</td>
                <td>{{ typeLabel(d.divorceType) }}@if (d.talaqNumber) { <small class="text-muted"> ({{ talaqLabel(d.talaqNumber) }})</small> }</td>
                <td class="text-nowrap">{{ d.divorceDate | date: 'MMM d, y' }}</td>
                <td>
                  @if (d.voided) { <span class="badge badge-secondary">VOID</span> }
                  @else if (d.status === 'REVOKED') { <span class="badge badge-info">REVOKED</span> }
                  @else { <span class="badge badge-success">RECORDED</span> }
                </td>
                <td class="text-right"><a class="btn btn-sm btn-outline-primary" [routerLink]="['/imam/svc/divorce', d.id]">Open</a></td>
              </tr>
            } @empty {
              <tr><td colspan="6" class="text-center text-muted py-4">{{ loaded() ? 'No divorce records yet.' : 'Loading…' }}</td></tr>
            }
          </tbody>
        </table>
      </div>
    </div></div>
  `,
})
export class DivorceList {
  private http = inject(HttpClient);

  typeLabel = typeLabel;
  talaqLabel = talaqLabel;
  items = signal<Divorce[]>([]);
  q = signal('');
  loaded = signal(false);
  error = signal('');
  shown = computed(() => {
    const q = this.q().trim().toLowerCase();
    return q
      ? this.items().filter((d) => (d.groomName + ' ' + d.brideName + ' ' + (d.number ?? '')).toLowerCase().includes(q))
      : this.items();
  });

  constructor() {
    this.http.get<Divorce[]>(DIVORCE_API).subscribe({
      next: (r) => {
        this.items.set(r);
        this.loaded.set(true);
      },
      error: (e) => this.error.set(errorMessage(e)),
    });
  }
}
