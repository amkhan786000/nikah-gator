import { HttpClient } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { errorMessage } from '../../../core/auth.service';
import { JUMA_API, JumaLetter, hhmm } from './juma.model';

function blank(): JumaLetter {
  return {
    schoolName: '', schoolAddress: '', schoolContact: '', addressee: '', letterDate: '',
    standing: true, startDate: '', endDate: '', windowStart: '12:30', windowEnd: '14:00', note: '',
    students: [{ name: '', grade: '' }],
  };
}

@Component({
  selector: 'app-juma-form',
  imports: [FormsModule, RouterLink],
  template: `
    <div class="page-title-box"><h4 class="page-title">{{ id ? 'Edit Juma letter' : 'New Juma letter' }}</h4></div>
    @if (error()) { <div class="alert alert-danger">{{ error() }}</div> }

    <form class="card" (ngSubmit)="save()"><div class="card-body">
      <span class="card-header-title">School</span>
      <div class="row">
        <div class="form-group col-md-6"><label>School name</label><input class="form-control" name="sn" maxlength="200" [(ngModel)]="l.schoolName" required /></div>
        <div class="form-group col-md-6"><label>Addressed to (principal)</label><input class="form-control" name="ad" maxlength="200" placeholder="e.g. Ms. Jane Lee, Principal" [(ngModel)]="l.addressee" required /></div>
        <div class="form-group col-md-6"><label>School address</label><input class="form-control" name="sa" maxlength="300" [(ngModel)]="l.schoolAddress" /></div>
        <div class="form-group col-md-6"><label>School contact (email or phone)</label><input class="form-control" name="sc" maxlength="200" [(ngModel)]="l.schoolContact" /></div>
      </div>

      <span class="card-header-title mt-2">Students</span>
      @for (s of l.students; track $index) {
        <div class="row align-items-end">
          <div class="form-group col-7 col-md-6"><label>Name</label><input class="form-control" [name]="'sn' + $index" maxlength="150" [(ngModel)]="s.name" required /></div>
          <div class="form-group col-3 col-md-3"><label>Grade</label><input class="form-control" [name]="'sg' + $index" maxlength="40" [(ngModel)]="s.grade" /></div>
          <div class="form-group col-2 col-md-1">
            <button type="button" class="btn btn-outline-danger" title="Remove" [disabled]="l.students.length === 1" (click)="remove($index)"><i class="mdi mdi-close"></i></button>
          </div>
        </div>
      }
      <button type="button" class="btn btn-secondary btn-responsive mb-3" [disabled]="l.students.length >= 60" (click)="add()"><i class="mdi mdi-plus"></i> Add student</button>

      <span class="card-header-title mt-2">Request</span>
      <div class="form-group">
        <div class="custom-control custom-radio"><input class="custom-control-input" type="radio" id="st1" name="standing" [value]="true" [(ngModel)]="l.standing" /><label class="custom-control-label" for="st1">Standing request (every Friday until further notice)</label></div>
        <div class="custom-control custom-radio"><input class="custom-control-input" type="radio" id="st2" name="standing" [value]="false" [(ngModel)]="l.standing" /><label class="custom-control-label" for="st2">Date range</label></div>
      </div>
      <div class="row">
        <div class="form-group col-6 col-md-3"><label>{{ l.standing ? 'Starting' : 'First date' }}</label><input class="form-control" type="date" name="sd" [(ngModel)]="l.startDate" required /></div>
        @if (!l.standing) {
          <div class="form-group col-6 col-md-3"><label>Last date</label><input class="form-control" type="date" name="ed" [(ngModel)]="l.endDate" required /></div>
        }
        <div class="form-group col-6 col-md-3"><label>Friday from</label><input class="form-control" type="time" name="ws" [(ngModel)]="l.windowStart" required /></div>
        <div class="form-group col-6 col-md-3"><label>Friday until</label><input class="form-control" type="time" name="we" [(ngModel)]="l.windowEnd" required /></div>
        <div class="form-group col-6 col-md-3"><label>Letter date (optional)</label><input class="form-control" type="date" name="ld" [(ngModel)]="l.letterDate" /></div>
      </div>
      <div class="form-group"><label>Note (optional, printed in the letter)</label><textarea class="form-control" name="note" rows="3" maxlength="1000" [(ngModel)]="l.note"></textarea></div>

      <div class="d-flex flex-wrap" style="gap:.5rem">
        <button class="btn btn-primary btn-responsive" [disabled]="busy()">{{ busy() ? 'Saving…' : 'Save' }}</button>
        <a class="btn btn-secondary btn-responsive" [routerLink]="id ? ['/imam/svc/juma', id] : ['/imam/svc/juma']">Cancel</a>
      </div>
    </div></form>
  `,
})
export class JumaForm {
  private http = inject(HttpClient);
  private router = inject(Router);

  id = inject(ActivatedRoute).snapshot.paramMap.get('id');
  l: JumaLetter = blank();
  busy = signal(false);
  error = signal('');

  constructor() {
    if (this.id) {
      this.http.get<JumaLetter>(`${JUMA_API}/${this.id}`).subscribe({
        next: (r) => {
          if (r.status === 'VOID') {
            this.router.navigate(['/imam/svc/juma', this.id]);
            return;
          }
          this.l = { ...r, windowStart: hhmm(r.windowStart), windowEnd: hhmm(r.windowEnd) };
        },
        error: (e) => this.error.set(errorMessage(e)),
      });
    }
  }

  add() {
    this.l.students = [...this.l.students, { name: '', grade: '' }];
  }

  remove(i: number) {
    this.l.students = this.l.students.filter((_, idx) => idx !== i);
  }

  save() {
    this.busy.set(true);
    this.error.set('');
    const body = {
      ...this.l,
      letterDate: this.l.letterDate || null,
      endDate: this.l.standing ? null : this.l.endDate || null,
    };
    const call = this.id ? this.http.put<JumaLetter>(`${JUMA_API}/${this.id}`, body) : this.http.post<JumaLetter>(JUMA_API, body);
    call.subscribe({
      next: (r) => this.router.navigate(['/imam/svc/juma', r.id]),
      error: (e) => {
        this.busy.set(false);
        this.error.set(errorMessage(e));
      },
    });
  }
}
