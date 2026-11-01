import { HttpClient } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { errorMessage } from '../../core/auth.service';
import { CatalogService } from '../../core/catalog.service';
import { CardTableDirective } from '../../shared/card-table.directive';
import { StaffView } from './staff.model';

@Component({
  selector: 'app-staff-list',
  imports: [RouterLink, CardTableDirective],
  template: `
    <div class="page-title-box d-flex flex-wrap align-items-center justify-content-between">
      <h4 class="page-title">Team</h4>
      <a class="btn btn-primary btn-responsive" routerLink="/imam/staff/new"><i class="mdi mdi-plus"></i> Add team member</a>
    </div>
    @if (message()) { <div class="alert alert-success">{{ message() }}</div> }
    @if (error()) { <div class="alert alert-danger">{{ error() }}</div> }
    <p class="text-muted">People you add can sign in and use only the services you give them, either to view or to manage.</p>

    <div class="card"><div class="card-body p-0">
      <div class="table-responsive">
        <table class="table table-hover mb-0">
          <thead><tr><th>Name</th><th>Title</th><th>Access</th><th>Status</th><th></th></tr></thead>
          <tbody>
            @for (s of staff(); track s.id) {
              <tr [style.opacity]="s.enabled ? 1 : .55">
                <td><strong>{{ s.fullName }}</strong><br /><small class="text-muted">{{ s.email }}</small></td>
                <td>{{ s.jobTitle }}</td>
                <td>
                  @if (count(s, 'MANAGE') + count(s, 'VIEW') === 0) { <span class="text-muted">No access yet</span> }
                  @if (count(s, 'MANAGE')) { <span class="badge badge-success" [title]="names(s, 'MANAGE')">Manage {{ count(s, 'MANAGE') }}</span> }
                  @if (count(s, 'VIEW')) { <span class="badge badge-info ml-1" [title]="names(s, 'VIEW')">View {{ count(s, 'VIEW') }}</span> }
                </td>
                <td>
                  <span class="badge" [class.badge-success]="s.enabled" [class.badge-secondary]="!s.enabled">{{ s.enabled ? 'Active' : 'Inactive' }}</span>
                  @if (s.mustChangePassword) { <span class="badge badge-warning ml-1">Not signed in yet</span> }
                </td>
                <td class="text-nowrap text-right">
                  <a class="btn btn-sm btn-outline-primary" [routerLink]="['/imam/staff', s.id]">Edit</a>
                  <button class="btn btn-sm btn-outline-primary ml-1" (click)="resend(s)">Resend login</button>
                  <button class="btn btn-sm btn-secondary ml-1" (click)="toggle(s)">{{ s.enabled ? 'Deactivate' : 'Activate' }}</button>
                </td>
              </tr>
            } @empty {
              <tr><td colspan="5" class="text-center text-muted py-4">{{ loaded() ? 'No team members yet.' : 'Loading…' }}</td></tr>
            }
          </tbody>
        </table>
      </div>
    </div></div>
  `,
})
export class StaffList {
  private http = inject(HttpClient);
  private catalog = inject(CatalogService);

  staff = signal<StaffView[]>([]);
  loaded = signal(false);
  message = signal('');
  error = signal('');

  constructor() {
    this.load();
  }

  load() {
    this.http.get<StaffView[]>('/api/imam/staff').subscribe({
      next: (r) => {
        this.staff.set(r);
        this.loaded.set(true);
      },
      error: (e) => this.error.set(errorMessage(e)),
    });
  }

  count(s: StaffView, level: 'VIEW' | 'MANAGE'): number {
    return Object.values(s.permissions).filter((l) => l === level).length;
  }

  /** The service names behind a count, shown on hover. */
  names(s: StaffView, level: 'VIEW' | 'MANAGE'): string {
    const labels = new Map((this.catalog.services() ?? []).map((x) => [x.key, x.label]));
    return Object.entries(s.permissions).filter(([, l]) => l === level).map(([k]) => labels.get(k) ?? k).join(', ');
  }

  resend(s: StaffView) {
    if (!confirm(`Send ${s.fullName} a new temporary password? Their current password stops working.`)) return;
    this.http.post<StaffView>(`/api/imam/staff/${s.id}/resend-credentials`, {}).subscribe({
      next: (r) => {
        this.message.set(r.credentialsEmailed ? `A new login email was sent to ${r.email}.` : `The email to ${r.email} could not be sent.`);
        this.load();
      },
      error: (e) => this.error.set(errorMessage(e)),
    });
  }

  toggle(s: StaffView) {
    this.http.patch<StaffView>(`/api/imam/staff/${s.id}/active`, { active: !s.enabled }).subscribe({
      next: () => this.load(),
      error: (e) => this.error.set(errorMessage(e)),
    });
  }
}
