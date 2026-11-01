import { HttpClient } from '@angular/common/http';
import { DatePipe } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { errorMessage } from '../../../core/auth.service';
import { CardTableDirective } from '../../../shared/card-table.directive';
import { CS_API, ServiceCert } from './cs.model';

@Component({
  selector: 'app-cs-list',
  imports: [CardTableDirective, RouterLink, DatePipe, FormsModule],
  template: `
    <div class="page-title-box d-flex flex-wrap align-items-center justify-content-between">
      <h4 class="page-title">Community service certificates</h4>
      <a class="btn btn-primary btn-responsive" routerLink="/imam/svc/community-service/new"><i class="mdi mdi-plus"></i> New certificate</a>
    </div>
    @if (error()) { <div class="alert alert-danger">{{ error() }}</div> }

    <div class="card"><div class="card-body">
      <div class="input-icon mb-3" style="max-width:340px">
        <i class="mdi mdi-magnify"></i>
        <input class="form-control" placeholder="Search name or number…" [ngModel]="q()" (ngModelChange)="q.set($event)" />
      </div>
      <div class="table-responsive">
        <table class="table table-hover mb-0">
          <thead><tr><th>Number</th><th>Name</th><th>Type</th><th>Hours</th><th>Period</th><th>Status</th><th></th></tr></thead>
          <tbody>
            @for (c of shown(); track c.id) {
              <tr>
                <td class="text-nowrap">{{ c.certNumber }}</td>
                <td>{{ c.personName }}</td>
                <td>{{ c.personType === 'STUDENT' ? 'Student' : 'Volunteer' }}</td>
                <td>{{ c.hours }}</td>
                <td class="text-nowrap">{{ c.startDate | date: 'MMM d, y' }} to {{ c.endDate | date: 'MMM d, y' }}</td>
                <td><span class="badge" [class.badge-success]="c.status === 'VALID'" [class.badge-secondary]="c.status === 'VOID'">{{ c.status }}</span></td>
                <td class="text-right"><a class="btn btn-sm btn-outline-primary" [routerLink]="['/imam/svc/community-service', c.id]">Open</a></td>
              </tr>
            } @empty {
              <tr><td colspan="7" class="text-center text-muted py-4">{{ loaded() ? 'No certificates yet.' : 'Loading…' }}</td></tr>
            }
          </tbody>
        </table>
      </div>
    </div></div>
  `,
})
export class CsList {
  private http = inject(HttpClient);

  certs = signal<ServiceCert[]>([]);
  q = signal('');
  loaded = signal(false);
  error = signal('');
  shown = computed(() => {
    const q = this.q().trim().toLowerCase();
    return q ? this.certs().filter((c) => (c.personName + ' ' + c.certNumber).toLowerCase().includes(q)) : this.certs();
  });

  constructor() {
    this.http.get<ServiceCert[]>(CS_API).subscribe({
      next: (r) => {
        this.certs.set(r);
        this.loaded.set(true);
      },
      error: (e) => this.error.set(errorMessage(e)),
    });
  }
}
