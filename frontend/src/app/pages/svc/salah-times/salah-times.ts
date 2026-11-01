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
        <span class="card-header-title mt-2">Sunrise and Maghrib</span>
        <p class="text-muted small">You do not need to type these. They are worked out for every day from your masjid's location, and Maghrib is sunset plus the minutes you choose.</p>
        <div class="row">
          <div class="form-group col-6 col-md-3"><label>Latitude</label>
            <input class="form-control" type="number" step="any" name="la" [ngModel]="s.latitude" (ngModelChange)="s.latitude = $event === '' ? null : $event" placeholder="29.6516" /></div>
          <div class="form-group col-6 col-md-3"><label>Longitude</label>
            <input class="form-control" type="number" step="any" name="lo" [ngModel]="s.longitude" (ngModelChange)="s.longitude = $event === '' ? null : $event" placeholder="-82.3248" /></div>
          <div class="form-group col-md-6"><label>&nbsp;</label>
            <div class="title-actions">
              <button class="btn btn-sm btn-outline-primary" type="button" (click)="useHere()"><i class="mdi mdi-crosshairs-gps"></i> Use my current location</button>
              <button class="btn btn-sm btn-outline-primary" type="button" (click)="findAddress()"><i class="mdi mdi-map-search-outline"></i> Find from the masjid address</button>
            </div></div>
        </div>
        @if (place()) { <p class="small text-muted">Found: {{ place() }}. Check that this is your masjid, then Save.</p> }
        <div class="form-group">
          <label>Maghrib is</label>
          <div class="d-flex align-items-center" style="gap:.4rem">
            <input class="form-control" style="max-width:90px" type="number" min="0" max="60" name="mo" [(ngModel)]="s.maghribOffsetMinutes" />
            <span>minutes after sunset</span>
          </div>
        </div>
        @if (ov.sunToday) {
          <p class="small">Today: sunrise <strong>{{ f(ov.sunToday.sunrise) }}</strong>, sunset {{ f(ov.sunToday.sunset) }}, Maghrib <strong>{{ f(ov.sunToday.maghrib) }}</strong>.</p>
        } @else {
          <div class="alert alert-warning py-2"><i class="mdi mdi-alert-outline"></i> Set your location (and Save) so sunrise and Maghrib can be worked out.</div>
        }
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
                  <td>{{ f(p.fajr) }}</td><td>{{ p.sunrise ? f(p.sunrise) : 'Auto' }}</td><td>{{ f(p.dhuhr) }}</td><td>{{ f(p.asr) }}</td>
                  <td>{{ p.maghrib ? f(p.maghrib) : 'Auto' }}</td><td>{{ f(p.isha) }}</td>
                  <td>@for (j of p.jumuah; track $index) { <div class="text-nowrap">{{ f(j.time) }}@if (j.label) { <small class="text-muted"> {{ j.label }}</small> }</div> }</td>
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
  s: Settings = { changeUnit: 'MONTHS', changeEvery: 1, publicEnabled: true, publicDays: 1, showSunrise: true, latitude: null, longitude: null, maghribOffsetMinutes: 3 };
  place = signal('');
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

  /** The browser asks the imam for permission; works best on a phone at the masjid. */
  useHere() {
    if (!navigator.geolocation) {
      this.error.set('This browser cannot find your location. Enter the latitude and longitude instead.');
      return;
    }
    navigator.geolocation.getCurrentPosition(
      (pos) => {
        this.s.latitude = Math.round(pos.coords.latitude * 1e6) / 1e6;
        this.s.longitude = Math.round(pos.coords.longitude * 1e6) / 1e6;
        this.place.set('');
        this.error.set('');
        this.message.set('Location filled in. Press Save to use it.');
      },
      () => this.error.set('Could not get your location. You can allow it in the browser, or enter the latitude and longitude.'),
      { timeout: 10000 },
    );
  }

  findAddress() {
    this.error.set('');
    this.http.post<{ latitude: number; longitude: number; place: string }>(`${API}/locate`, {}).subscribe({
      next: (r) => {
        this.s.latitude = Math.round(r.latitude * 1e6) / 1e6;
        this.s.longitude = Math.round(r.longitude * 1e6) / 1e6;
        this.place.set(r.place);
        this.message.set('');
      },
      error: (e) => this.error.set(errorMessage(e)),
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
