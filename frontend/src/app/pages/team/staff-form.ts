import { HttpClient } from '@angular/common/http';
import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { errorMessage } from '../../core/auth.service';
import { CatalogService } from '../../core/catalog.service';
import { Level, StaffView } from './staff.model';

type Choice = 'NONE' | Level;

/** Private records the imam would normally keep to themselves. */
const PRIVATE = ['COUNSELING', 'SHAHADA', 'DIVORCE'];
/** Day-to-day services a front desk person usually manages. */
const FRONT_DESK = ['NIKAH', 'JUMA_LETTER', 'COMMUNITY_SERVICE', 'HALL_BOOKING', 'GENERAL_LETTER'];

@Component({
  selector: 'app-staff-form',
  imports: [FormsModule, RouterLink],
  template: `
    <div class="page-title-box d-flex flex-wrap align-items-center justify-content-between">
      <h4 class="page-title">{{ id ? 'Edit team member' : 'Add team member' }}</h4>
      <a class="btn btn-secondary btn-responsive" routerLink="/imam/staff"><i class="mdi mdi-arrow-left"></i> Team</a>
    </div>
    @if (error()) { <div class="alert alert-danger">{{ error() }}</div> }

    <form class="card" (ngSubmit)="save()"><div class="card-body">
      <span class="card-header-title">Person</span>
      <div class="row">
        <div class="form-group col-md-6"><label>Full name</label><input class="form-control" name="fn" [(ngModel)]="f.fullName" required maxlength="120" /></div>
        <div class="form-group col-md-6"><label>E-mail (their sign-in)</label>
          <input class="form-control" type="email" name="em" [(ngModel)]="f.email" [disabled]="!!id" required /></div>
        <div class="form-group col-md-6"><label>Title</label><input class="form-control" name="jt" [(ngModel)]="f.jobTitle" maxlength="80" placeholder="e.g. Secretary, Treasurer" /></div>
        <div class="form-group col-md-6"><label>Phone</label><input class="form-control" type="tel" name="ph" [(ngModel)]="f.phone" maxlength="30" /></div>
      </div>

      <span class="card-header-title mt-2">What they can do</span>
      <div class="form-group">
        <label>Start from</label>
        <select class="form-control" style="max-width:420px" [ngModel]="preset" (ngModelChange)="applyPreset($event)" name="preset">
          <option value="custom">Custom (pick below)</option>
          <option value="viewer">Viewer: look at everything except private records</option>
          <option value="frontdesk">Front desk: manage nikahs, letters and hall; view the rest</option>
        </select>
      </div>
      <div class="table-responsive">
        <table class="table mb-3">
          <thead><tr><th>Service</th><th class="text-center">No access</th><th class="text-center">View</th><th class="text-center">Manage</th></tr></thead>
          <tbody>
            @for (s of services(); track s.key) {
              <tr>
                <td><i class="mdi mr-1" [class]="s.icon"></i>{{ s.label }}</td>
                @for (c of choices; track c.value) {
                  <td class="text-center">
                    <input type="radio" [name]="'p-' + s.key" [checked]="level(s.key) === c.value" (change)="set(s.key, c.value)"
                           [attr.aria-label]="s.label + ': ' + c.label" />
                  </td>
                }
              </tr>
            }
          </tbody>
        </table>
      </div>
      <p class="text-muted small">
        <strong>View</strong> lets them open and print records. <strong>Manage</strong> also lets them add, edit and void.
        Only you can change the masjid settings, the certificate design and this team.
      </p>
      @if (!id) {
        <p class="text-muted small">They get an email with a temporary password. At first sign-in they get a code by email and choose a new password.</p>
      }
      <button class="btn btn-primary btn-responsive" [disabled]="busy()">{{ id ? 'Save changes' : 'Add and send login email' }}</button>
    </div></form>
  `,
})
export class StaffForm {
  private http = inject(HttpClient);
  private router = inject(Router);
  private catalog = inject(CatalogService);

  id = inject(ActivatedRoute).snapshot.paramMap.get('id');
  readonly choices: { value: Choice; label: string }[] = [
    { value: 'NONE', label: 'No access' },
    { value: 'VIEW', label: 'View' },
    { value: 'MANAGE', label: 'Manage' },
  ];

  /** The services this imam has; only these can be granted. */
  services = computed(() => this.catalog.services() ?? []);
  f = { fullName: '', email: '', phone: '', jobTitle: '' };
  grants = signal<Record<string, Choice>>({});
  preset = 'custom';
  busy = signal(false);
  error = signal('');

  constructor() {
    this.catalog.load().subscribe();
    if (this.id) {
      this.http.get<StaffView>(`/api/imam/staff/${this.id}`).subscribe({
        next: (s) => {
          this.f = { fullName: s.fullName, email: s.email, phone: s.phone ?? '', jobTitle: s.jobTitle ?? '' };
          this.grants.set({ ...s.permissions });
        },
        error: (e) => this.error.set(errorMessage(e)),
      });
    }
  }

  level(key: string): Choice {
    return this.grants()[key] ?? 'NONE';
  }

  set(key: string, c: Choice) {
    this.preset = 'custom';
    this.grants.update((g) => ({ ...g, [key]: c }));
  }

  applyPreset(p: string) {
    this.preset = p;
    const g: Record<string, Choice> = {};
    for (const s of this.services()) {
      if (p === 'viewer') g[s.key] = PRIVATE.includes(s.key) ? 'NONE' : 'VIEW';
      else if (p === 'frontdesk') g[s.key] = PRIVATE.includes(s.key) ? 'NONE' : FRONT_DESK.includes(s.key) ? 'MANAGE' : 'VIEW';
      else g[s.key] = this.level(s.key);
    }
    this.grants.set(g);
  }

  save() {
    this.busy.set(true);
    this.error.set('');
    const body = { ...this.f, permissions: this.grants() };
    const req = this.id ? this.http.put<StaffView>(`/api/imam/staff/${this.id}`, body) : this.http.post<StaffView>('/api/imam/staff', body);
    req.subscribe({
      next: () => this.router.navigateByUrl('/imam/staff'),
      error: (e) => {
        this.busy.set(false);
        this.error.set(errorMessage(e));
      },
    });
  }
}
