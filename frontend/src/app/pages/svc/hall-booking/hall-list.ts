import { HttpClient } from '@angular/common/http';
import { DatePipe } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { errorMessage } from '../../../core/auth.service';
import { HallBooking, hallBadge } from './hall.model';

/** Bookings of one month, grouped by the day they start. Printable (the menu and buttons are hidden on paper). */
@Component({
  selector: 'app-hall-list',
  imports: [RouterLink, DatePipe],
  template: `
    <div class="page-title-box no-print d-flex flex-wrap align-items-center justify-content-between">
      <h4 class="page-title">Hall &amp; event bookings</h4>
      <div class="title-actions">
        <button class="btn btn-secondary btn-responsive" (click)="print()"><i class="mdi mdi-printer"></i> Print</button>
        <a class="btn btn-primary btn-responsive" routerLink="/imam/svc/hall-booking/new"><i class="mdi mdi-plus"></i> New booking</a>
      </div>
    </div>
    @if (error()) { <div class="alert alert-danger">{{ error() }}</div> }

    <div class="card"><div class="card-body">
      <div class="d-flex align-items-center justify-content-between mb-3">
        <button class="btn btn-sm btn-outline-secondary no-print" (click)="move(-1)" aria-label="Previous month"><i class="mdi mdi-chevron-left"></i></button>
        <h5 class="mb-0">{{ monthStart() | date: 'MMMM y' }}</h5>
        <button class="btn btn-sm btn-outline-secondary no-print" (click)="move(1)" aria-label="Next month"><i class="mdi mdi-chevron-right"></i></button>
      </div>

      @for (g of groups(); track g.day) {
        <div class="mb-3">
          <div class="font-weight-bold border-bottom pb-1 mb-2">{{ g.day | date: 'EEEE, MMM d' }}</div>
          @for (b of g.items; track b.id) {
            <div class="d-flex flex-wrap align-items-start justify-content-between py-1" [class.text-muted]="b.status === 'CANCELLED'">
              <div>
                <strong [style.text-decoration]="b.status === 'CANCELLED' ? 'line-through' : 'none'">{{ b.title }}</strong>
                <span [class]="badge(b.status)" class="ml-1">{{ b.status }}</span><br />
                <small>{{ b.startsAt | date: 'h:mm a' }} – {{ b.endsAt | date: (sameDay(b) ? 'h:mm a' : 'MMM d, h:mm a') }} · {{ b.space }}
                  @if (b.expectedGuests) { · {{ b.expectedGuests }} guests }
                  · {{ b.contactName }}@if (b.contactPhone) { ({{ b.contactPhone }}) }
                  @if (b.feeText) { · Fee: {{ b.feeText }} }</small>
              </div>
              <a class="btn btn-sm btn-outline-primary no-print" [routerLink]="['/imam/svc/hall-booking', b.id, 'edit']">Open</a>
            </div>
          }
        </div>
      } @empty {
        <p class="text-center text-muted py-4 mb-0">{{ loaded() ? 'No bookings this month.' : 'Loading…' }}</p>
      }
    </div></div>
  `,
})
export class HallList {
  private http = inject(HttpClient);

  month = signal(this.fmt(new Date()));
  bookings = signal<HallBooking[]>([]);
  loaded = signal(false);
  error = signal('');
  badge = hallBadge;

  monthStart = computed(() => new Date(this.month() + '-01T00:00:00'));
  groups = computed(() => {
    const byDay = new Map<string, HallBooking[]>();
    for (const b of this.bookings()) {
      const day = b.startsAt.slice(0, 10);
      byDay.set(day, [...(byDay.get(day) ?? []), b]);
    }
    return [...byDay.entries()].sort(([a], [b]) => a.localeCompare(b)).map(([day, items]) => ({ day, items }));
  });

  constructor() {
    this.load();
  }

  private fmt(d: Date): string {
    return d.getFullYear() + '-' + String(d.getMonth() + 1).padStart(2, '0');
  }

  sameDay(b: HallBooking): boolean {
    return b.startsAt.slice(0, 10) === b.endsAt.slice(0, 10);
  }

  move(by: number) {
    const d = this.monthStart();
    d.setMonth(d.getMonth() + by);
    this.month.set(this.fmt(d));
    this.load();
  }

  print() {
    window.print();
  }

  private load() {
    this.loaded.set(false);
    this.http.get<HallBooking[]>('/api/imam/svc/hall-booking', { params: { month: this.month() } }).subscribe({
      next: (r) => {
        this.bookings.set(r);
        this.loaded.set(true);
      },
      error: (e) => this.error.set(errorMessage(e)),
    });
  }
}
