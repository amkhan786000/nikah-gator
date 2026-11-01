import { HttpClient } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { errorMessage } from '../../../core/auth.service';
import { Overview, Period, Session } from './salah.model';

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
        <div class="form-group col-6 col-md-3"><label>Sunrise <small class="text-muted">(optional)</small></label><input class="form-control" type="time" name="su" [(ngModel)]="f.sunrise" /></div>
        <div class="form-group col-6 col-md-3"><label>Dhuhr</label><input class="form-control" type="time" name="dh" [(ngModel)]="f.dhuhr" required /></div>
        <div class="form-group col-6 col-md-3"><label>Asr</label><input class="form-control" type="time" name="as" [(ngModel)]="f.asr" required /></div>
        <div class="form-group col-6 col-md-3"><label>Maghrib <small class="text-muted">(optional)</small></label><input class="form-control" type="time" name="ma" [(ngModel)]="f.maghrib" /></div>
        <div class="form-group col-6 col-md-3"><label>Isha</label><input class="form-control" type="time" name="is" [(ngModel)]="f.isha" required /></div>
        <div class="form-group col-12 col-md-6"><label>Note (optional)</label><input class="form-control" name="no" [(ngModel)]="f.notes" maxlength="200" placeholder="e.g. Daylight saving time begins" /></div>
      </div>
      <p class="text-muted small">Leave Sunrise and Maghrib empty and they are worked out every day from your masjid's location (Maghrib is sunset plus your minutes).</p>

      <span class="card-header-title mt-2">Jumu'ah</span>
      @for (j of jumuah(); track $index) {
        <div class="form-row mb-2">
          <div class="col-5 col-md-3"><input class="form-control" type="time" [name]="'jt' + $index" [ngModel]="j.time" (ngModelChange)="setJumuah($index, { time: $event })" [attr.aria-label]="'Jumuah time ' + ($index + 1)" /></div>
          <div class="col-5 col-md-4"><input class="form-control" [name]="'jl' + $index" [ngModel]="j.label" (ngModelChange)="setJumuah($index, { label: $event })" maxlength="40" placeholder="Label, e.g. English khutbah" aria-label="Label" /></div>
          <div class="col-2"><button class="btn btn-outline-secondary" type="button" (click)="removeJumuah($index)" aria-label="Remove this Jumu'ah"><i class="mdi mdi-close"></i></button></div>
        </div>
      }
      <button class="btn btn-sm btn-outline-primary mb-3" type="button" (click)="addJumuah()" [disabled]="jumuah().length >= 5"><i class="mdi mdi-plus"></i> Add a Jumu'ah time</button>
      <p class="text-muted small">Some masjids hold more than one Jumu'ah. Add each one, with a label if you like.</p>

      <div><button class="btn btn-primary btn-responsive" [disabled]="busy()">Save period</button></div>
    </div></form>
  `,
})
export class SalahForm {
  private http = inject(HttpClient);
  private router = inject(Router);

  id = inject(ActivatedRoute).snapshot.paramMap.get('id');
  f = { startDate: '', endDate: '', fajr: '', sunrise: '', dhuhr: '', asr: '', maghrib: '', isha: '', notes: '' };
  jumuah = signal<Session[]>([]);
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
    this.f.maghrib = p.maghrib ?? '';
    this.f.isha = p.isha;
    this.jumuah.set(p.jumuah.map((j) => ({ ...j })));
    this.f.notes = dates ? (p.notes ?? '') : '';
  }

  addJumuah() {
    this.jumuah.update((l) => [...l, { time: '', label: '' }]);
  }

  removeJumuah(i: number) {
    this.jumuah.update((l) => l.filter((_, k) => k !== i));
  }

  setJumuah(i: number, change: Partial<Session>) {
    this.jumuah.update((l) => l.map((j, k) => (k === i ? { ...j, ...change } : j)));
  }

  save() {
    this.busy.set(true);
    this.error.set('');
    const body = {
      ...this.f,
      sunrise: this.f.sunrise || null,
      maghrib: this.f.maghrib || null,
      jumuah: this.jumuah().filter((j) => j.time),
      notes: this.f.notes || null,
    };
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
