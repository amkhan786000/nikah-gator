import { HttpClient } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { errorMessage } from '../../../core/auth.service';
import { CS_API, ServiceCert } from './cs.model';

function blank(): ServiceCert {
  return {
    personName: '', personType: 'STUDENT', organization: '', activities: '', startDate: '', endDate: '',
    hours: null, supervisor: '', notes: '',
  };
}

@Component({
  selector: 'app-cs-form',
  imports: [FormsModule, RouterLink],
  template: `
    <div class="page-title-box"><h4 class="page-title">{{ id ? 'Edit certificate' : 'New community service certificate' }}</h4></div>
    @if (error()) { <div class="alert alert-danger">{{ error() }}</div> }

    <form class="card" (ngSubmit)="save()"><div class="card-body">
      <span class="card-header-title">Person</span>
      <div class="row">
        <div class="form-group col-md-6"><label>Full name</label><input class="form-control" name="pn" maxlength="200" [(ngModel)]="c.personName" required /></div>
        <div class="form-group col-md-3">
          <label>Type</label>
          <select class="form-control" name="pt" [(ngModel)]="c.personType">
            <option value="STUDENT">Student</option>
            <option value="VOLUNTEER">Volunteer</option>
          </select>
        </div>
        <div class="form-group col-md-6"><label>School or organization (optional)</label><input class="form-control" name="org" maxlength="200" [(ngModel)]="c.organization" /></div>
      </div>

      <span class="card-header-title mt-2">Service</span>
      <div class="form-group"><label>Activities</label><textarea class="form-control" name="act" rows="3" maxlength="2000" placeholder="e.g. Friday cleanup, serving iftar meals" [(ngModel)]="c.activities" required></textarea></div>
      <div class="row">
        <div class="form-group col-6 col-md-3"><label>First date</label><input class="form-control" type="date" name="sd" [(ngModel)]="c.startDate" required /></div>
        <div class="form-group col-6 col-md-3"><label>Last date</label><input class="form-control" type="date" name="ed" [max]="today" [(ngModel)]="c.endDate" required /></div>
        <div class="form-group col-6 col-md-3"><label>Total hours</label><input class="form-control" type="number" inputmode="decimal" name="hrs" min="0.25" max="2000" step="0.25" [(ngModel)]="c.hours" required /></div>
        <div class="form-group col-6 col-md-3"><label>Supervisor</label><input class="form-control" name="sup" maxlength="200" [(ngModel)]="c.supervisor" required /></div>
      </div>
      <div class="form-group"><label>Notes (optional, printed on the certificate)</label><textarea class="form-control" name="notes" rows="2" maxlength="1000" [(ngModel)]="c.notes"></textarea></div>

      <div class="d-flex flex-wrap" style="gap:.5rem">
        <button class="btn btn-primary btn-responsive" [disabled]="busy()">{{ busy() ? 'Saving…' : 'Save' }}</button>
        <a class="btn btn-secondary btn-responsive" [routerLink]="id ? ['/imam/svc/community-service', id] : ['/imam/svc/community-service']">Cancel</a>
      </div>
    </div></form>
  `,
})
export class CsForm {
  private http = inject(HttpClient);
  private router = inject(Router);

  id = inject(ActivatedRoute).snapshot.paramMap.get('id');
  c: ServiceCert = blank();
  busy = signal(false);
  error = signal('');
  today = new Date().toISOString().slice(0, 10);

  constructor() {
    if (this.id) {
      this.http.get<ServiceCert>(`${CS_API}/${this.id}`).subscribe({
        next: (r) => {
          if (r.status === 'VOID') {
            this.router.navigate(['/imam/svc/community-service', this.id]);
            return;
          }
          this.c = r;
        },
        error: (e) => this.error.set(errorMessage(e)),
      });
    }
  }

  save() {
    this.busy.set(true);
    this.error.set('');
    const call = this.id ? this.http.put<ServiceCert>(`${CS_API}/${this.id}`, this.c) : this.http.post<ServiceCert>(CS_API, this.c);
    call.subscribe({
      next: (r) => this.router.navigate(['/imam/svc/community-service', r.id]),
      error: (e) => {
        this.busy.set(false);
        this.error.set(errorMessage(e));
      },
    });
  }
}
