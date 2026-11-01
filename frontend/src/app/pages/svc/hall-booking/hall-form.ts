import { HttpClient } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { errorMessage } from '../../../core/auth.service';
import { HALL_STATUSES, HallBooking } from './hall.model';

@Component({
  selector: 'app-hall-form',
  imports: [FormsModule, RouterLink],
  template: `
    <div class="page-title-box"><h4 class="page-title">{{ id ? 'Edit booking' : 'New booking' }}</h4></div>
    @if (error()) { <div class="alert alert-danger">{{ error() }}</div> }

    <form class="card" (ngSubmit)="save()"><div class="card-body">
      <div class="row">
        <div class="form-group col-md-6"><label>Title</label><input class="form-control" name="ti" [(ngModel)]="b.title" required maxlength="200" placeholder="e.g. Walima, Eid dinner" /></div>
        <div class="form-group col-md-6"><label>Space</label><input class="form-control" name="sp" [(ngModel)]="b.space" maxlength="100" placeholder="Main hall" /></div>
        <div class="form-group col-md-6"><label>Starts</label><input class="form-control" type="datetime-local" name="s" [(ngModel)]="b.startsAt" required /></div>
        <div class="form-group col-md-6"><label>Ends</label><input class="form-control" type="datetime-local" name="e" [(ngModel)]="b.endsAt" required /></div>
        <div class="form-group col-md-4"><label>Contact name</label><input class="form-control" name="cn" [(ngModel)]="b.contactName" required maxlength="200" /></div>
        <div class="form-group col-md-4"><label>Phone</label><input class="form-control" type="tel" name="cp" [(ngModel)]="b.contactPhone" maxlength="50" /></div>
        <div class="form-group col-md-4"><label>Email</label><input class="form-control" type="email" name="ce" [(ngModel)]="b.contactEmail" maxlength="200" /></div>
        <div class="form-group col-md-4"><label>Expected guests</label><input class="form-control" type="number" min="0" name="gu" [(ngModel)]="b.expectedGuests" /></div>
        <div class="form-group col-md-4"><label>Fee (text)</label><input class="form-control" name="fe" [(ngModel)]="b.feeText" maxlength="200" placeholder="e.g. $250 deposit paid" /></div>
        <div class="form-group col-md-4"><label>Status</label>
          <select class="form-control" name="st" [(ngModel)]="b.status">
            @for (t of statuses; track t) { <option [value]="t">{{ t }}</option> }
          </select></div>
      </div>
      <div class="form-group"><label>Notes</label><textarea class="form-control" name="no" rows="3" [(ngModel)]="b.notes"></textarea></div>
      <div class="d-flex flex-wrap" style="gap:.5rem">
        <button class="btn btn-primary btn-responsive" [disabled]="busy()">{{ busy() ? 'Saving…' : 'Save' }}</button>
        <a class="btn btn-secondary btn-responsive" routerLink="/imam/svc/hall-booking">Cancel</a>
        @if (id) { <button type="button" class="btn btn-outline-danger btn-responsive" (click)="remove()">Delete</button> }
      </div>
    </div></form>
  `,
})
export class HallForm {
  private http = inject(HttpClient);
  private router = inject(Router);

  id = inject(ActivatedRoute).snapshot.paramMap.get('id');
  statuses = HALL_STATUSES;
  b: HallBooking = {
    title: '', contactName: '', contactPhone: '', contactEmail: '', startsAt: '', endsAt: '',
    space: 'Main hall', expectedGuests: null, status: 'REQUESTED', feeText: '', notes: '',
  };
  busy = signal(false);
  error = signal('');

  constructor() {
    if (this.id) {
      this.http.get<HallBooking>(`/api/imam/svc/hall-booking/${this.id}`).subscribe({
        next: (r) => (this.b = { ...r, startsAt: r.startsAt.slice(0, 16), endsAt: r.endsAt.slice(0, 16) }),
        error: (e) => this.error.set(errorMessage(e)),
      });
    }
  }

  save() {
    this.busy.set(true);
    this.error.set('');
    const body = { ...this.b, expectedGuests: this.b.expectedGuests ?? null };
    const call = this.id
      ? this.http.put<HallBooking>(`/api/imam/svc/hall-booking/${this.id}`, body)
      : this.http.post<HallBooking>('/api/imam/svc/hall-booking', body);
    call.subscribe({
      next: () => this.router.navigate(['/imam/svc/hall-booking']),
      error: (e) => {
        this.busy.set(false);
        this.error.set(errorMessage(e));
      },
    });
  }

  remove() {
    if (!confirm('Delete this booking?')) return;
    this.http.delete(`/api/imam/svc/hall-booking/${this.id}`).subscribe({
      next: () => this.router.navigate(['/imam/svc/hall-booking']),
      error: (e) => this.error.set(errorMessage(e)),
    });
  }
}
