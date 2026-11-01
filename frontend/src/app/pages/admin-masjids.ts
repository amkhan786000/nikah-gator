import { HttpClient } from '@angular/common/http';
import { CardTableDirective } from '../shared/card-table.directive';
import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { errorMessage } from '../core/auth.service';

interface MasjidView {
  id: number;
  name: string;
  city: string | null;
  state: string | null;
  active: boolean;
  createdAt: string;
  imamId: number | null;
  imamName: string | null;
  imamEmail: string | null;
  imamMustChangePassword: boolean;
  credentialsEmailed: boolean;
}

const EMPTY = {
  name: '', addressLine: '', city: '', state: '', zip: '', phone: '', email: '',
  imamName: '', imamEmail: '', imamPhone: '',
};

@Component({
  selector: 'app-admin-masjids',
  imports: [CardTableDirective, FormsModule, DatePipe, RouterLink],
  template: `
    <div class="page-title-box d-flex flex-wrap align-items-center justify-content-between">
      <h4 class="page-title">Masjids</h4>
      <button class="btn btn-primary btn-responsive" (click)="showForm.set(!showForm())">
        <i class="mdi" [class]="showForm() ? 'mdi-close' : 'mdi-plus'"></i> {{ showForm() ? 'Close' : 'Add masjid and imam' }}
      </button>
    </div>

    @if (message()) { <div class="alert alert-success">{{ message() }}</div> }
    @if (error()) { <div class="alert alert-danger">{{ error() }}</div> }

    @if (showForm()) {
      <form class="card" (ngSubmit)="create()"><div class="card-body">
        <span class="card-header-title">Masjid</span>
        <div class="row">
          <div class="form-group col-12"><label>Name</label><input class="form-control" name="name" [(ngModel)]="f.name" required /></div>
          <div class="form-group col-12"><label>Address</label><input class="form-control" name="addr" [(ngModel)]="f.addressLine" /></div>
          <div class="form-group col-md-5"><label>City</label><input class="form-control" name="city" [(ngModel)]="f.city" /></div>
          <div class="form-group col-6 col-md-3"><label>State</label><input class="form-control" name="state" [(ngModel)]="f.state" maxlength="2" placeholder="FL" /></div>
          <div class="form-group col-6 col-md-4"><label>ZIP</label><input class="form-control" name="zip" [(ngModel)]="f.zip" /></div>
          <div class="form-group col-md-6"><label>Phone</label><input class="form-control" name="phone" [(ngModel)]="f.phone" type="tel" /></div>
          <div class="form-group col-md-6"><label>Masjid email</label><input class="form-control" type="email" name="memail" [(ngModel)]="f.email" /></div>
        </div>
        <span class="card-header-title mt-2">Imam (manages this masjid)</span>
        <div class="row">
          <div class="form-group col-md-4"><label>Full name</label><input class="form-control" name="iname" [(ngModel)]="f.imamName" required /></div>
          <div class="form-group col-md-4"><label>Email (sign-in)</label><input class="form-control" type="email" name="iemail" [(ngModel)]="f.imamEmail" required /></div>
          <div class="form-group col-md-4"><label>Phone</label><input class="form-control" name="iphone" [(ngModel)]="f.imamPhone" type="tel" /></div>
        </div>
        <p class="text-muted small">The imam receives an email with a temporary password. At first sign-in they get a code by email and choose a new password.</p>
        <button class="btn btn-primary btn-responsive" [disabled]="busy()">Create masjid and send login email</button>
      </div></form>
    }

    <div class="card"><div class="card-body p-0">
      <div class="table-responsive">
        <table class="table table-hover mb-0">
          <thead><tr><th>Masjid</th><th>City</th><th>Imam</th><th>Status</th><th>Added</th><th></th></tr></thead>
          <tbody>
            @for (m of masjids(); track m.id) {
              <tr [style.opacity]="m.active ? 1 : .55">
                <td><strong>{{ m.name }}</strong></td>
                <td>{{ m.city }}{{ m.state ? ', ' + m.state : '' }}</td>
                <td>{{ m.imamName }}<br /><small class="text-muted">{{ m.imamEmail }}</small></td>
                <td>
                  <span class="badge" [class.badge-success]="m.active" [class.badge-secondary]="!m.active">{{ m.active ? 'Active' : 'Inactive' }}</span>
                  @if (m.imamMustChangePassword) { <span class="badge badge-warning ml-1">Not signed in yet</span> }
                </td>
                <td>{{ m.createdAt | date: 'mediumDate' }}</td>
                <td class="text-nowrap text-right">
                  <a class="btn btn-sm btn-outline-primary" [routerLink]="['/admin/masjids', m.id, 'services']">Services</a>
                  <button class="btn btn-sm btn-outline-primary ml-1" (click)="resend(m)">Resend login</button>
                  <button class="btn btn-sm btn-secondary ml-1" (click)="toggle(m)">{{ m.active ? 'Deactivate' : 'Activate' }}</button>
                </td>
              </tr>
            } @empty {
              <tr><td colspan="6" class="text-center text-muted py-4">No masjids yet. Add the first one.</td></tr>
            }
          </tbody>
        </table>
      </div>
    </div></div>
  `,
})
export class AdminMasjids {
  private http = inject(HttpClient);

  f = { ...EMPTY };
  masjids = signal<MasjidView[]>([]);
  showForm = signal(false);
  busy = signal(false);
  message = signal('');
  error = signal('');

  constructor() {
    this.load();
  }

  load() {
    this.http.get<MasjidView[]>('/api/admin/masjids').subscribe({
      next: (r) => this.masjids.set(r),
      error: (e) => this.error.set(errorMessage(e)),
    });
  }

  create() {
    this.busy.set(true);
    this.error.set('');
    this.message.set('');
    this.http.post<MasjidView>('/api/admin/masjids', this.f).subscribe({
      next: (m) => {
        this.busy.set(false);
        this.message.set(
          m.credentialsEmailed
            ? `${m.name} created. A login email was sent to ${m.imamEmail}.`
            : `${m.name} created, but the email to ${m.imamEmail} could not be sent. Use "Resend login" once e-mail works.`,
        );
        this.f = { ...EMPTY };
        this.showForm.set(false);
        this.load();
      },
      error: (e) => {
        this.busy.set(false);
        this.error.set(errorMessage(e));
      },
    });
  }

  resend(m: MasjidView) {
    if (!confirm(`Send ${m.imamName} a new temporary password? Their current password stops working.`)) return;
    this.error.set('');
    this.http.post<MasjidView>(`/api/admin/masjids/${m.id}/resend-credentials`, {}).subscribe({
      next: (r) => {
        this.message.set(r.credentialsEmailed ? `A new login email was sent to ${r.imamEmail}.` : `The email to ${r.imamEmail} could not be sent.`);
        this.load();
      },
      error: (e) => this.error.set(errorMessage(e)),
    });
  }

  toggle(m: MasjidView) {
    this.http.patch<MasjidView>(`/api/admin/masjids/${m.id}/active`, { active: !m.active }).subscribe({
      next: () => this.load(),
      error: (e) => this.error.set(errorMessage(e)),
    });
  }
}
