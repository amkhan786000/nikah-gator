import { HttpClient } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { errorMessage } from '../../../core/auth.service';
import { Overview, Period } from './salah.model';

const API = '/api/imam/svc/salah-times';

@Component({
  selector: 'app-salah-form',
  imports: [FormsModule, RouterLink],
  template: `
    <div class="page-title-box d-flex flex-wrap align-items-center justify-content-between">
      <h4 class="page-title">{{ id ? 'Edit period' : 'Add period' }}</h4>
      <a class="btn btn-secondary btn-responsive" routerLink="/imam/svc/salah-times"><i class="mdi mdi-arrow-left"></i> Timetable</a>
    </div>
    @if (copied()) { <div class="alert alert-info">The dates are suggested from how often your times change, and the times are copied from your last period. Change what is different.</div> }
    @if (error()) { <div class="alert alert-danger">{{ error() }}</div> }

    <form class="card" (ngSubmit)="save()"><div class="card-body">
      <span class="card-header-title">Dates these times apply to</span>
      <div class="row">
        <div class="form-group col-6 col-md-3"><label>From</label><input class="form-control" type="date" name="sd" [(ngModel)]="f.startDate" required /></div>
        <div class="form-group col-6 col-md-3"><label>To (including)</label><input class="form-control" type="date" name="ed" [(ngModel)]="f.endDate" required /></div>
      </div>
      <p class="text-muted small">Days that already have times are replaced by this period.</p>

      <span class="card-header-title mt-2">Times</span>
      <div class="row">
        <div class="form-group col-6 col-md-3"><label>Fajr</label><input class="form-control" type="time" name="fa" [(ngModel)]="f.fajr" required /></div>
        <div class="form-group col-6 col-md-3"><label>Sunrise</label><input class="form-control" type="time" name="su" [(ngModel)]="f.sunrise" /></div>
        <div class="form-group col-6 col-md-3"><label>Dhuhr</label><input class="form-control" type="time" name="dh" [(ngModel)]="f.dhuhr" required /></div>
        <div class="form-group col-6 col-md-3"><label>Asr</label><input class="form-control" type="time" name="as" [(ngModel)]="f.asr" required /></div>
        <div class="form-group col-6 col-md-3"><label>Maghrib</label><input class="form-control" type="time" name="ma" [(ngModel)]="f.maghrib" required /></div>
        <div class="form-group col-6 col-md-3"><label>Isha</label><input class="form-control" type="time" name="is" [(ngModel)]="f.isha" required /></div>
        <div class="form-group col-6 col-md-3"><label>Jumu'ah</label><input class="form-control" type="time" name="ju" [(ngModel)]="f.jumuah" /></div>
        <div class="form-group col-12 col-md-6"><label>Note (optional)</label><input class="form-control" name="no" [(ngModel)]="f.notes" maxlength="200" placeholder="e.g. Daylight saving time begins" /></div>
      </div>
      <button class="btn btn-primary btn-responsive" [disabled]="busy()">Save period</button>
    </div></form>
  `,
})
export class SalahForm {
  private http = inject(HttpClient);
  private router = inject(Router);

  id = inject(ActivatedRoute).snapshot.paramMap.get('id');
  f = { startDate: '', endDate: '', fajr: '', sunrise: '', dhuhr: '', asr: '', maghrib: '', isha: '', jumuah: '', notes: '' };
  busy = signal(false);
  copied = signal(false);
  error = signal('');

  constructor() {
    this.http.get<Overview>(API).subscribe({
      next: (o) => {
        const edit = this.id ? o.periods.find((p) => p.id === +this.id!) : null;
        if (this.id) {
          if (edit) this.fill(edit);
          else this.error.set('Period not found.');
        } else {
          // a new period: suggest the dates from "how often my times change" and start from the last times
          this.f.startDate = o.nextStart;
          this.f.endDate = o.nextEnd;
          if (o.lastPeriod) {
            this.fill(o.lastPeriod, false);
            this.copied.set(true);
          }
        }
      },
      error: (e) => this.error.set(errorMessage(e)),
    });
  }

  private fill(p: Period, dates = true) {
    if (dates) {
      this.f.startDate = p.startDate;
      this.f.endDate = p.endDate;
    }
    this.f.fajr = p.fajr;
    this.f.sunrise = p.sunrise ?? '';
    this.f.dhuhr = p.dhuhr;
    this.f.asr = p.asr;
    this.f.maghrib = p.maghrib;
    this.f.isha = p.isha;
    this.f.jumuah = p.jumuah ?? '';
    this.f.notes = dates ? (p.notes ?? '') : '';
  }

  save() {
    this.busy.set(true);
    this.error.set('');
    const body = { ...this.f, sunrise: this.f.sunrise || null, jumuah: this.f.jumuah || null, notes: this.f.notes || null };
    const req = this.id ? this.http.put(`${API}/periods/${this.id}`, body) : this.http.post(`${API}/periods`, body);
    req.subscribe({
      next: () => this.router.navigateByUrl('/imam/svc/salah-times'),
      error: (e) => {
        this.busy.set(false);
        this.error.set(errorMessage(e));
      },
    });
  }
}
