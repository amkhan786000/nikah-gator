import { HttpClient } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { errorMessage } from '../core/auth.service';
import { Nikah } from './nikah.model';

function blank(): Nikah {
  return {
    groomName: '', groomFatherName: '', groomEmail: '', groomPhone: '',
    brideName: '', brideFatherName: '', brideEmail: '', bridePhone: '',
    waliName: '', waliRelation: '', witness1Name: '', witness2Name: '',
    mahrDescription: '', nikahAt: '', notes: '',
  };
}

@Component({
  selector: 'app-nikah-form',
  imports: [FormsModule, RouterLink],
  template: `
    <div class="page-title-box"><h4 class="page-title">{{ id ? 'Edit nikah' : 'Register nikah' }}</h4></div>
    @if (error()) { <div class="alert alert-danger">{{ error() }}</div> }

    <form class="card" (ngSubmit)="save()"><div class="card-body">
      <span class="card-header-title">Nikah</span>
      <div class="row">
          <div class="form-group col-md-6"><label>Date and time</label><input class="form-control" type="datetime-local" name="at" [(ngModel)]="n.nikahAt"  required /></div>
          <div class="form-group col-md-6"><label>Mahr (dower)</label><input class="form-control" type="text" name="mahr" [(ngModel)]="n.mahrDescription" placeholder="e.g. $1,000 / gold set"  /></div>
      </div>

      <span class="card-header-title mt-2">Groom</span>
      <div class="row">
          <div class="form-group col-md-6"><label>Full name</label><input class="form-control" type="text" name="gn" [(ngModel)]="n.groomName"  required /></div>
          <div class="form-group col-md-6"><label>Father's name</label><input class="form-control" type="text" name="gf" [(ngModel)]="n.groomFatherName"   /></div>
          <div class="form-group col-md-6"><label>Email</label><input class="form-control" type="email" name="ge" [(ngModel)]="n.groomEmail"   /></div>
          <div class="form-group col-md-6"><label>Phone</label><input class="form-control" type="tel" name="gp" [(ngModel)]="n.groomPhone"   /></div>
      </div>

      <span class="card-header-title mt-2">Bride</span>
      <div class="row">
          <div class="form-group col-md-6"><label>Full name</label><input class="form-control" type="text" name="bn" [(ngModel)]="n.brideName"  required /></div>
          <div class="form-group col-md-6"><label>Father's name</label><input class="form-control" type="text" name="bf" [(ngModel)]="n.brideFatherName"   /></div>
          <div class="form-group col-md-6"><label>Email</label><input class="form-control" type="email" name="be" [(ngModel)]="n.brideEmail"   /></div>
          <div class="form-group col-md-6"><label>Phone</label><input class="form-control" type="tel" name="bp" [(ngModel)]="n.bridePhone"   /></div>
      </div>

      <span class="card-header-title mt-2">Wali and witnesses</span>
      <div class="row">
          <div class="form-group col-md-6"><label>Wali name</label><input class="form-control" type="text" name="wn" [(ngModel)]="n.waliName"   /></div>
          <div class="form-group col-md-6"><label>Wali relation</label><input class="form-control" type="text" name="wr" [(ngModel)]="n.waliRelation" placeholder="Father, brother…"  /></div>
          <div class="form-group col-md-6"><label>Witness 1</label><input class="form-control" type="text" name="w1" [(ngModel)]="n.witness1Name"   /></div>
          <div class="form-group col-md-6"><label>Witness 2</label><input class="form-control" type="text" name="w2" [(ngModel)]="n.witness2Name"   /></div>
      </div>

      <div class="form-group"><label>Notes</label><textarea class="form-control" name="notes" rows="3" [(ngModel)]="n.notes"></textarea></div>

      <div class="d-flex flex-wrap" style="gap:.5rem">
        <button class="btn btn-primary btn-responsive" [disabled]="busy()">{{ busy() ? 'Saving…' : 'Save' }}</button>
        <a class="btn btn-secondary btn-responsive" [routerLink]="id ? ['/imam/nikahs', id] : ['/imam/nikahs']">Cancel</a>
      </div>
    </div></form>
  `,
})
export class NikahForm {
  private http = inject(HttpClient);
  private router = inject(Router);

  id = inject(ActivatedRoute).snapshot.paramMap.get('id');
  n: Nikah = blank();
  busy = signal(false);
  error = signal('');

  constructor() {
    if (this.id) {
      this.http.get<Nikah>(`/api/imam/nikahs/${this.id}`).subscribe({
        next: (r) => (this.n = { ...r, nikahAt: r.nikahAt.slice(0, 16) }),
        error: (e) => this.error.set(errorMessage(e)),
      });
    }
  }

  save() {
    this.busy.set(true);
    this.error.set('');
    const call = this.id
      ? this.http.put<Nikah>(`/api/imam/nikahs/${this.id}`, this.n)
      : this.http.post<Nikah>('/api/imam/nikahs', this.n);
    call.subscribe({
      next: (r) => this.router.navigate(['/imam/nikahs', r.id]),
      error: (e) => {
        this.busy.set(false);
        this.error.set(errorMessage(e));
      },
    });
  }
}
