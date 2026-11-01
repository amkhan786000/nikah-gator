import { HttpClient } from '@angular/common/http';
import { DatePipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { errorMessage } from '../../../core/auth.service';
import { fmt12 } from '../../../shared/time';
import { ImportResult } from './salah.model';

const API = '/api/imam/svc/salah-times';

@Component({
  selector: 'app-salah-import',
  imports: [RouterLink, DatePipe],
  template: `
    <div class="page-title-box d-flex flex-wrap align-items-center justify-content-between">
      <h4 class="page-title">Import times from a CSV</h4>
      <a class="btn btn-secondary btn-responsive" routerLink="/imam/svc/salah-times"><i class="mdi mdi-arrow-left"></i> Timetable</a>
    </div>
    @if (error()) { <div class="alert alert-danger">{{ error() }}</div> }

    <div class="card"><div class="card-body">
      <p>Upload a file with one row per day, or one row per range of days. Columns can be in any order:</p>
      <ul class="small text-muted">
        <li><strong>date</strong> (one day per row), or <strong>start date</strong> and <strong>end date</strong> (a range per row)</li>
        <li><strong>fajr, sunrise, dhuhr, asr, maghrib, isha</strong>, and optionally <strong>jumuah</strong> and <strong>notes</strong></li>
        <li>Dates like 2026-11-05 or 11/5/2026. Times like 5:23 AM or 17:45 (without AM/PM, afternoon times are understood).</li>
      </ul>
      <button class="btn btn-sm btn-outline-primary mb-3" type="button" (click)="template()"><i class="mdi mdi-download"></i> Download an example file</button>
      <div class="form-group">
        <input class="form-control-file" type="file" accept=".csv,text/csv,text/plain" (change)="pick($event)" aria-label="CSV file" />
      </div>
      <button class="btn btn-primary btn-responsive" (click)="check()" [disabled]="!file || busy()">Check the file</button>
    </div></div>

    @if (r(); as res) {
      @if (res.errors.length) {
        <div class="alert alert-danger">
          <strong>Nothing was imported.</strong> Please fix these and try again:
          <ul class="mb-0 mt-1">@for (e of res.errors; track $index) { <li>{{ e }}</li> }</ul>
        </div>
      } @else {
        <div class="alert alert-success">
          {{ res.rowCount }} row{{ res.rowCount === 1 ? '' : 's' }} look good.
          @if (res.replacedCount) { Importing replaces the times you already have on those days ({{ res.replacedCount }} existing period{{ res.replacedCount === 1 ? '' : 's' }} affected). }
        </div>
        <div class="card"><div class="card-body p-0"><div class="table-responsive">
          <table class="table mb-0">
            <thead><tr><th>Dates</th><th>Fajr</th><th>Sunrise</th><th>Dhuhr</th><th>Asr</th><th>Maghrib</th><th>Isha</th><th>Jumu'ah</th></tr></thead>
            <tbody>
              @for (x of res.rows.slice(0, 40); track x.line) {
                <tr><td class="text-nowrap">{{ x.startDate | date: 'MMM d' }}{{ x.endDate === x.startDate ? '' : ' – ' + (x.endDate | date: 'MMM d') }}</td>
                  <td>{{ f(x.fajr) }}</td><td>{{ f(x.sunrise) }}</td><td>{{ f(x.dhuhr) }}</td><td>{{ f(x.asr) }}</td><td>{{ f(x.maghrib) }}</td><td>{{ f(x.isha) }}</td><td>{{ f(x.jumuah) }}</td></tr>
              }
            </tbody>
          </table>
        </div></div></div>
        @if (res.rows.length > 40) { <p class="text-muted small">Showing the first 40 of {{ res.rows.length }} rows.</p> }
        <button class="btn btn-primary btn-responsive" (click)="commit()" [disabled]="busy()">Import {{ res.rowCount }} row{{ res.rowCount === 1 ? '' : 's' }}</button>
      }
    }
  `,
})
export class SalahImport {
  private http = inject(HttpClient);
  private router = inject(Router);

  file: File | null = null;
  r = signal<ImportResult | null>(null);
  busy = signal(false);
  error = signal('');
  f = fmt12;

  pick(e: Event) {
    this.file = (e.target as HTMLInputElement).files?.[0] ?? null;
    this.r.set(null);
  }

  template() {
    this.http.get(`${API}/template.csv`, { responseType: 'blob' }).subscribe({
      next: (blob) => {
        const href = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = href;
        a.download = 'salah-times-template.csv';
        document.body.appendChild(a);
        a.click();
        a.remove();
        setTimeout(() => URL.revokeObjectURL(href), 10000);
      },
      error: (e) => this.error.set(errorMessage(e)),
    });
  }

  check() {
    this.send(false);
  }

  commit() {
    this.send(true);
  }

  private send(commit: boolean) {
    if (!this.file) return;
    this.busy.set(true);
    this.error.set('');
    const fd = new FormData();
    fd.append('file', this.file);
    this.http.post<ImportResult>(`${API}/import?commit=${commit}`, fd).subscribe({
      next: (res) => {
        this.busy.set(false);
        this.r.set(res);
        if (res.committed) this.router.navigateByUrl('/imam/svc/salah-times');
      },
      error: (e) => {
        this.busy.set(false);
        this.error.set(errorMessage(e));
      },
    });
  }
}
