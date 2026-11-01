import { HttpClient } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { Observable } from 'rxjs';
import { errorMessage } from '../core/auth.service';

interface ServiceView {
  key: string;
  label: string;
  description: string;
  icon: string;
  masjidEnabled: boolean;
  /** null = the imam has no setting of their own and follows the masjid. */
  imamEnabled: boolean | null;
  effective: boolean;
}

@Component({
  selector: 'app-admin-services',
  imports: [RouterLink],
  template: `
    <div class="page-title-box d-flex flex-wrap align-items-center justify-content-between">
      <h4 class="page-title">Services</h4>
      <a class="btn btn-secondary btn-responsive" routerLink="/admin/masjids"><i class="mdi mdi-arrow-left"></i> Masjids</a>
    </div>
    @if (error()) { <div class="alert alert-danger">{{ error() }}</div> }
    <p class="text-muted">
      A service is available to the imam only when it is on for the masjid <strong>and</strong> not turned off for the imam.
      Changes apply immediately, also to the imam's menu and API.
    </p>

    <div class="card"><div class="card-body p-0">
      <div class="table-responsive">
        <table class="table mb-0">
          <thead><tr><th>Service</th><th class="text-center">Masjid</th><th class="text-center">This imam</th><th class="text-center">Result</th></tr></thead>
          <tbody>
            @for (s of services(); track s.key) {
              <tr>
                <td><i class="mdi mr-1" [class]="s.icon"></i><strong>{{ s.label }}</strong><br /><small class="text-muted">{{ s.description }}</small></td>
                <td class="text-center">
                  <div class="custom-control custom-switch d-inline-block">
                    <input type="checkbox" class="custom-control-input" [id]="'m-' + s.key" [checked]="s.masjidEnabled" (change)="setMasjid(s, $any($event.target).checked)" />
                    <label class="custom-control-label" [for]="'m-' + s.key"><span class="sr-only">{{ s.label }} for the masjid</span></label>
                  </div>
                </td>
                <td class="text-center">
                  <select class="form-control form-control-sm d-inline-block" style="width:auto" [value]="imamValue(s)" (change)="setImam(s, $any($event.target).value)"
                          [attr.aria-label]="s.label + ' for the imam'">
                    <option value="inherit">Same as masjid</option>
                    <option value="on">Always on</option>
                    <option value="off">Off</option>
                  </select>
                </td>
                <td class="text-center">
                  <span class="badge" [class.badge-success]="s.effective" [class.badge-secondary]="!s.effective">{{ s.effective ? 'Enabled' : 'Disabled' }}</span>
                </td>
              </tr>
            } @empty {
              <tr><td colspan="4" class="text-center text-muted py-4">Loading…</td></tr>
            }
          </tbody>
        </table>
      </div>
    </div></div>
  `,
})
export class AdminServices {
  private http = inject(HttpClient);
  private id = inject(ActivatedRoute).snapshot.paramMap.get('id');

  services = signal<ServiceView[]>([]);
  error = signal('');

  constructor() {
    this.http.get<ServiceView[]>(this.url()).subscribe({ next: (r) => this.services.set(r), error: (e) => this.error.set(errorMessage(e)) });
  }

  imamValue(s: ServiceView): string {
    return s.imamEnabled === null ? 'inherit' : s.imamEnabled ? 'on' : 'off';
  }

  setMasjid(s: ServiceView, enabled: boolean) {
    this.save(this.http.put<ServiceView[]>(`${this.url()}/${s.key}`, { enabled }));
  }

  setImam(s: ServiceView, v: string) {
    this.save(this.http.put<ServiceView[]>(`${this.url()}/${s.key}/imam`, { enabled: v === 'inherit' ? null : v === 'on' }));
  }

  private save(req: Observable<ServiceView[]>) {
    this.error.set('');
    req.subscribe({
      next: (r) => this.services.set(r),
      error: (e) => {
        this.error.set(errorMessage(e));
        this.reload();
      },
    });
  }

  private reload() {
    this.http.get<ServiceView[]>(this.url()).subscribe((r) => this.services.set(r));
  }

  private url() {
    return `/api/admin/masjids/${this.id}/services`;
  }
}
