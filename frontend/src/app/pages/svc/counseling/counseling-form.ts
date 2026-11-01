import { HttpClient } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { errorMessage } from '../../../core/auth.service';
import { CounselingDetail, SESSION_STATUSES, SESSION_TYPES } from './counseling.model';

@Component({
  selector: 'app-counseling-form',
  imports: [FormsModule, RouterLink],
  template: `
    <div class="page-title-box"><h4 class="page-title">{{ id ? 'Edit session' : 'Log session' }}</h4></div>
    @if (error()) { <div class="alert alert-danger">{{ error() }}</div> }

    <form class="card" (ngSubmit)="save()"><div class="card-body">
      <div class="row">
        <div class="form-group col-md-6"><label>Client name</label><input class="form-control" name="cn" [(ngModel)]="s.clientName" required maxlength="200" /></div>
        <div class="form-group col-md-6"><label>Type</label>
          <select class="form-control" name="ty" [(ngModel)]="s.sessionType">
            @for (t of types; track t) { <option [value]="t">{{ t }}</option> }
          </select></div>
        <div class="form-group col-md-6"><label>Date and time</label><input class="form-control" type="datetime-local" name="at" [(ngModel)]="s.sessionAt" required /></div>
        <div class="form-group col-md-6"><label>Duration (minutes)</label><input class="form-control" type="number" min="1" max="1440" name="du" [(ngModel)]="s.durationMin" /></div>
        <div class="form-group col-md-6"><label>Follow-up date</label><input class="form-control" type="date" name="fu" [(ngModel)]="s.followUpOn" /></div>
        <div class="form-group col-md-6"><label>Status</label>
          <select class="form-control" name="st" [(ngModel)]="s.status">
            @for (t of statuses; track t) { <option [value]="t">{{ t }}</option> }
          </select></div>
      </div>
      <div class="form-group"><label>Private notes</label><textarea class="form-control" name="notes" rows="6" [(ngModel)]="s.notes"></textarea></div>
      <div class="d-flex flex-wrap" style="gap:.5rem">
        <button class="btn btn-primary btn-responsive" [disabled]="busy()">{{ busy() ? 'Saving…' : 'Save' }}</button>
        <a class="btn btn-secondary btn-responsive" [routerLink]="id ? ['/imam/svc/counseling', id] : ['/imam/svc/counseling']">Cancel</a>
      </div>
    </div></form>
  `,
})
export class CounselingForm {
  private http = inject(HttpClient);
  private router = inject(Router);

  id = inject(ActivatedRoute).snapshot.paramMap.get('id');
  types = SESSION_TYPES;
  statuses = SESSION_STATUSES;
  s: any = { clientName: '', sessionType: 'MARRIAGE', sessionAt: '', durationMin: null, notes: '', followUpOn: null, status: 'SCHEDULED' };
  busy = signal(false);
  error = signal('');

  constructor() {
    if (this.id) {
      this.http.get<CounselingDetail>(`/api/imam/svc/counseling/${this.id}`).subscribe({
        next: (r) => (this.s = { ...r, sessionAt: r.sessionAt.slice(0, 16) }),
        error: (e) => this.error.set(errorMessage(e)),
      });
    }
  }

  save() {
    this.busy.set(true);
    this.error.set('');
    const body = { ...this.s, followUpOn: this.s.followUpOn || null, durationMin: this.s.durationMin || null };
    const call = this.id
      ? this.http.put<CounselingDetail>(`/api/imam/svc/counseling/${this.id}`, body)
      : this.http.post<CounselingDetail>('/api/imam/svc/counseling', body);
    call.subscribe({
      next: (r) => this.router.navigate(['/imam/svc/counseling', r.id]),
      error: (e) => {
        this.busy.set(false);
        this.error.set(errorMessage(e));
      },
    });
  }
}
