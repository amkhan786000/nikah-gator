import { HttpClient } from '@angular/common/http';
import { DatePipe } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { Title } from '@angular/platform-browser';
import { ActivatedRoute } from '@angular/router';
import { fmt12 } from '../shared/time';

interface Day {
  date: string;
  weekday: string;
  today: boolean;
  published: boolean;
  fajr: string | null;
  sunrise: string | null;
  dhuhr: string | null;
  asr: string | null;
  maghrib: string | null;
  isha: string | null;
  jumuah: string | null;
  notes: string | null;
}

interface Times {
  masjidName: string;
  address: string;
  phone: string | null;
  timezone: string;
  today: string;
  nowTime: string;
  showSunrise: boolean;
  days: Day[];
}

/** The page behind the link the imam shares: prayer times for today (or the next few days). No sign-in. */
@Component({
  selector: 'app-public-salah',
  imports: [DatePipe],
  styles: [`
    :host { display: block; min-height: 100vh; background: #f4f6f1; }
    .wrap { max-width: 560px; margin: 0 auto; padding: 0 16px 40px; }
    header { background: linear-gradient(160deg, #1d3a26, #0f1d16); color: #fff; padding: 28px 16px 44px; text-align: center; }
    header img { width: 44px; height: 44px; border-radius: 12px; margin-bottom: 10px; }
    header h1 { font-size: 22px; font-weight: 800; margin: 0 0 4px; color: #fff !important; }
    header p { margin: 0; font-size: 13px; opacity: .8; }
    .card-day { background: #fff; border-radius: 16px; box-shadow: 0 8px 24px -14px rgba(15, 29, 22, .35); margin-top: -26px; margin-bottom: 18px; overflow: hidden; }
    .card-day + .card-day { margin-top: 0; }
    .day-head { display: flex; justify-content: space-between; align-items: baseline; padding: 14px 18px; border-bottom: 1px solid #e8ece4; }
    .day-head strong { font-size: 16px; }
    .day-head small { color: #6b7a6f; }
    .today-tag { background: #9ccc3d; color: #0f1d16; font-weight: 700; font-size: 11px; border-radius: 99px; padding: 2px 9px; margin-left: 8px; letter-spacing: .03em; }
    .row { display: flex; justify-content: space-between; padding: 12px 18px; font-size: 17px; border-bottom: 1px solid #f0f2ed; }
    .row:last-child { border-bottom: 0; }
    .row span:first-child { color: #55655a; }
    .row b { font-variant-numeric: tabular-nums; }
    .row.next { background: #f1f8e2; }
    .row.next span:first-child { color: #0f1d16; font-weight: 700; }
    .row.dim span, .row.dim b { color: #8a978d; font-size: 15px; }
    .note { padding: 10px 18px 14px; color: #55655a; font-size: 13px; }
    .none { padding: 18px; color: #6b7a6f; }
    .foot { text-align: center; color: #6b7a6f; font-size: 12px; margin-top: 8px; }
    .err { text-align: center; padding: 60px 16px; color: #55655a; }
    @media print { header { background: none; color: #000; } :host { background: #fff; } }
  `],
  template: `
    @if (error()) {
      <div class="err"><h2>Prayer times are not available</h2><p>This link is not active. Please ask the masjid for the current link.</p></div>
    } @else if (data(); as t) {
      <header>
        <img src="assets/icons/favicon.svg" alt="" />
        <h1>{{ t.masjidName }}</h1>
        @if (t.address) { <p>{{ t.address }}</p> }
        @if (t.phone) { <p>{{ t.phone }}</p> }
      </header>
      <div class="wrap">
        @for (d of t.days; track d.date) {
          <section class="card-day">
            <div class="day-head">
              <span><strong>{{ d.weekday }}</strong>@if (d.today) { <span class="today-tag">TODAY</span> }</span>
              <small>{{ d.date | date: 'MMM d, y' }}</small>
            </div>
            @if (d.published) {
              @for (p of rows(t, d); track p.name) {
                <div class="row" [class.dim]="p.dim" [class.next]="d.today && !p.dim && p.name === nextName()"><span>{{ p.name }}</span><b>{{ f(p.time) }}</b></div>
              }
              @if (d.jumuah) { <div class="row"><span>Jumu'ah</span><b>{{ f(d.jumuah) }}</b></div> }
              @if (d.notes) { <div class="note">{{ d.notes }}</div> }
            } @else {
              <div class="none">Times for this day have not been published yet.</div>
            }
          </section>
        }
        <p class="foot">Times are for {{ t.timezone.replace('_', ' ') }}. Please confirm with the masjid if unsure.</p>
      </div>
    } @else {
      <div class="err">Loading…</div>
    }
  `,
})
export class PublicSalah {
  private http = inject(HttpClient);

  data = signal<Times | null>(null);
  error = signal(false);
  f = fmt12;

  /** The next prayer today (the first one later than the masjid's current time), highlighted. */
  nextName = computed(() => {
    const t = this.data();
    const today = t?.days.find((d) => d.today);
    if (!t || !today?.published) return '';
    return this.prayers(today).find((p) => (p.time ?? '') > t.nowTime)?.name ?? '';
  });

  constructor() {
    const slug = inject(ActivatedRoute).snapshot.paramMap.get('slug');
    const title = inject(Title);
    this.http.get<Times>(`/api/public/salah/${slug}`).subscribe({
      next: (r) => {
        this.data.set(r);
        title.setTitle(`Prayer times · ${r.masjidName}`);
      },
      error: () => this.error.set(true),
    });
  }

  /** The five prayers with sunrise (dimmed) between Fajr and Dhuhr, when the masjid shows it. */
  rows(t: Times, d: Day): { name: string; time: string | null; dim: boolean }[] {
    const out = this.prayers(d).map((p) => ({ ...p, dim: false }));
    if (t.showSunrise && d.sunrise) out.splice(1, 0, { name: 'Sunrise', time: d.sunrise, dim: true });
    return out;
  }

  prayers(d: Day): { name: string; time: string | null }[] {
    return [
      { name: 'Fajr', time: d.fajr },
      { name: 'Dhuhr', time: d.dhuhr },
      { name: 'Asr', time: d.asr },
      { name: 'Maghrib', time: d.maghrib },
      { name: 'Isha', time: d.isha },
    ];
  }
}
