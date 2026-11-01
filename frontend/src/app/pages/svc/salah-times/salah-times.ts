import { HttpClient } from '@angular/common/http';
import { DatePipe } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { AuthService, errorMessage } from '../../../core/auth.service';
import { fmt12 } from '../../../shared/time';
import { Overview, Period, Settings } from './salah.model';

const API = '/api/imam/svc/salah-times';

@Component({
  selector: 'app-salah-times',
  imports: [FormsModule, RouterLink, DatePipe],
  template: `
    <div class="page-title-box d-flex flex-wrap align-items-center justify-content-between">
      <h4 class="page-title">Salah timetable</h4>
      <div class="title-actions">
        <a class="btn btn-secondary btn-responsive needs-manage" routerLink="/imam/svc/salah-times/import"><i class="mdi mdi-file-upload-outline"></i> Import CSV</a>
        <a class="btn btn-primary btn-responsive" routerLink="/imam/svc/salah-times/new"><i class="mdi mdi-plus"></i> Add period</a>
      </div>
    </div>
    @if (message()) { <div class="alert alert-success">{{ message() }}</div> }
    @if (error()) { <div class="alert alert-danger">{{ error() }}</div> }

    @if (o(); as ov) {
      @if (!ov.periods.length) {
        <div class="alert alert-info"><i class="mdi mdi-information-outline"></i> No times yet. Add a period for the days your times apply to, or import a CSV with all of them.</div>
      } @else if (!ov.todayCovered) {
        <div class="alert alert-warning"><i class="mdi mdi-alert-outline"></i> There are no times for today. People opening your link see "not published yet".</div>
      } @else if (ov.daysLeft !== null && ov.daysLeft <= 7) {
        <div class="alert alert-warning"><i class="mdi mdi-alert-outline"></i>
          Your schedule ends on {{ ov.coveredThrough | date: 'mediumDate' }} ({{ ov.daysLeft }} day{{ ov.daysLeft === 1 ? '' : 's' }} left). Add the next period.</div>
      }

      <div class="card"><div class="card-body">
        <span class="card-header-title">Share with people</span>
        @if (ov.settings.publicEnabled) {
          <div class="input-group mb-2">
            <input class="form-control" [value]="link()" readonly (focus)="$any($event.target).select()" aria-label="Link to your prayer times" />
            <div class="input-group-append"><button class="btn btn-primary" type="button" (click)="copy()"><i class="mdi mdi-content-copy"></i> {{ copied() ? 'Copied' : 'Copy' }}</button></div>
          </div>
          <div class="title-actions">
            <a class="btn btn-sm btn-outline-primary" [href]="link()" target="_blank" rel="noopener"><i class="mdi mdi-open-in-new"></i> Open</a>
            <a class="btn btn-sm btn-outline-primary" [href]="whatsapp()" target="_blank" rel="noopener"><i class="mdi mdi-whatsapp"></i> WhatsApp</a>
            <a class="btn btn-sm btn-outline-primary" [href]="sms()"><i class="mdi mdi-message-text-outline"></i> Text</a>
            <a class="btn btn-sm btn-outline-primary" [href]="mail()"><i class="mdi mdi-email-outline"></i> E-mail</a>
          </div>
          <p class="text-muted small mt-2 mb-0">The link always shows the right day, so you only need to send it once. It shows
            {{ ov.settings.publicDays === 1 ? "today's times" : 'today and the next ' + (ov.settings.publicDays - 1) + ' days' }}.</p>
        } @else {
          <p class="text-muted mb-0">The link is switched off. Turn it on below to share your times.</p>
        }
      </div></div>

      <form class="card" (ngSubmit)="saveSettings()"><div class="card-body">
        <span class="card-header-title">How your times are published</span>
        <div class="row">
          <div class="form-group col-md-6">
            <label>My times change every</label>
            <div class="d-flex" style="gap:.4rem">
              <input class="form-control" style="max-width:90px" type="number" min="1" max="366" name="ev" [(ngModel)]="s.changeEvery" />
              <select class="form-control" style="max-width:160px" name="un" [(ngModel)]="s.changeUnit">
                <option value="DAYS">day(s)</option><option value="WEEKS">week(s)</option><option value="MONTHS">month(s)</option>
              </select>
            </div>
            <small class="form-text text-muted">For example 10 days, 2 weeks or 1 month. This only suggests the dates of your next period.</small>
          </div>
          <div class="form-group col-md-6">
            <label>The link shows</label>
            <div class="d-flex align-items-center" style="gap:.4rem">
              <input class="form-control" style="max-width:90px" type="number" min="1" max="31" name="pd" [(ngModel)]="s.publicDays" />
              <span>day(s) starting today</span>
            </div>
            <small class="form-text text-muted">1 shows only today. 7 shows the whole week, 14 two weeks.</small>
          </div>
        </div>
        <div class="custom-control custom-checkbox mb-1">
          <input type="checkbox" class="custom-control-input" id="se" name="se" [(ngModel)]="s.publicEnabled" />
          <label class="custom-control-label" for="se">The link is on</label>
        </div>
        <div class="custom-control custom-checkbox mb-3">
          <input type="checkbox" class="custom-control-input" id="ss" name="ss" [(ngModel)]="s.showSunrise" />
          <label class="custom-control-label" for="ss">Show sunrise</label>
        </div>
        <button class="btn btn-primary btn-responsive needs-manage" [disabled]="busy()">Save</button>
      </div></form>

      <div class="card"><div class="card-body p-0">
        <div class="table-responsive">
          <table class="table table-hover mb-0">
            <thead><tr><th>Dates</th><th>Fajr</th><th>Sunrise</th><th>Dhuhr</th><th>Asr</th><th>Maghrib</th><th>Isha</th><th>Jumu'ah</th><th></th></tr></thead>
            <tbody>
              @for (p of ov.periods; track p.id) {
                <tr>
                  <td class="text-nowrap">
                    {{ p.startDate | date: 'MMM d' }}{{ p.endDate === p.startDate ? '' : ' – ' + (p.endDate | date: 'MMM d') }}
                    @if (p.current) { <span class="badge badge-success ml-1">Now</span> }
                    @if (p.notes) { <br /><small class="text-muted">{{ p.notes }}</small> }
                  </td>
                  <td>{{ f(p.fajr) }}</td><td>{{ f(p.sunrise) }}</td><td>{{ f(p.dhuhr) }}</td><td>{{ f(p.asr) }}</td>
                  <td>{{ f(p.maghrib) }}</td><td>{{ f(p.isha) }}</td><td>{{ f(p.jumuah) }}</td>
                  <td class="text-nowrap text-right">
                    <a class="btn btn-sm btn-outline-primary needs-manage" [routerLink]="['/imam/svc/salah-times', p.id]">Edit</a>
                    <button class="btn btn-sm btn-secondary ml-1 needs-manage" (click)="remove(p)">Delete</button>
                  </td>
                </tr>
              } @empty {
                <tr><td colspan="9" class="text-center text-muted py-4">Nothing here yet.</td></tr>
              }
            </tbody>
          </table>
        </div>
      </div></div>
    } @else if (!error()) {
      <p class="text-muted">Loading…</p>
    }
  `,
})
export class SalahTimes {
  private http = inject(HttpClient);
  private auth = inject(AuthService);

  o = signal<Overview | null>(null);
  s: Settings = { changeUnit: 'MONTHS', changeEvery: 1, publicEnabled: true, publicDays: 1, showSunrise: true };
  busy = signal(false);
  copied = signal(false);
  message = signal('');
  error = signal('');
  f = fmt12;

  link = computed(() => `${location.origin}/salah/${this.o()?.slug}`);
  private text = computed(() => `Prayer times at ${this.auth.me()?.masjidName ?? 'our masjid'}: ${this.link()}`);
  whatsapp = computed(() => `https://wa.me/?text=${encodeURIComponent(this.text())}`);
  sms = computed(() => `sms:?&body=${encodeURIComponent(this.text())}`);
  mail = computed(() => `mailto:?subject=${encodeURIComponent('Prayer times')}&body=${encodeURIComponent(this.text())}`);

  constructor() {
    this.load();
  }

  load() {
    this.http.get<Overview>(API).subscribe({
      next: (r) => {
        this.o.set(r);
        this.s = { ...r.settings };
      },
      error: (e) => this.error.set(errorMessage(e)),
    });
  }

  saveSettings() {
    this.busy.set(true);
    this.error.set('');
    this.message.set('');
    this.http.put<Settings>(`${API}/settings`, this.s).subscribe({
      next: () => {
        this.busy.set(false);
        this.message.set('Saved.');
        this.load();
      },
      error: (e) => {
        this.busy.set(false);
        this.error.set(errorMessage(e));
      },
    });
  }

  copy() {
    navigator.clipboard?.writeText(this.link()).then(() => {
      this.copied.set(true);
      setTimeout(() => this.copied.set(false), 2000);
    });
  }

  remove(p: Period) {
    if (!confirm(`Delete the times for ${p.startDate} to ${p.endDate}?`)) return;
    this.http.delete(`${API}/periods/${p.id}`).subscribe({
      next: () => this.load(),
      error: (e) => this.error.set(errorMessage(e)),
    });
  }
}
