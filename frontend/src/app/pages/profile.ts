import { HttpClient } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { AuthService, errorMessage } from '../core/auth.service';

interface Profile {
  fullName: string;
  email: string;
  phone: string | null;
  jobTitle: string | null;
  role: string;
}

/** Everyone edits their own details here. The e-mail is the sign-in, so it is shown but not editable. */
@Component({
  selector: 'app-profile',
  imports: [FormsModule, RouterLink],
  template: `
    <div class="page-title-box"><h4 class="page-title">My profile</h4></div>
    @if (message()) { <div class="alert alert-success">{{ message() }}</div> }
    @if (error()) { <div class="alert alert-danger">{{ error() }}</div> }

    <form class="card" style="max-width:640px" (ngSubmit)="save()"><div class="card-body">
      <div class="row">
        <div class="form-group col-12"><label for="fn">Full name</label>
          <input class="form-control" id="fn" name="fn" [ngModel]="p().fullName" (ngModelChange)="set({ fullName: $event })" required maxlength="120" /></div>
        <div class="form-group col-md-6"><label for="jt">Title</label>
          <input class="form-control" id="jt" name="jt" [ngModel]="p().jobTitle" (ngModelChange)="set({ jobTitle: $event })" maxlength="80" placeholder="e.g. Imam, Secretary" /></div>
        <div class="form-group col-md-6"><label for="ph">Phone</label>
          <input class="form-control" id="ph" name="ph" type="tel" [ngModel]="p().phone" (ngModelChange)="set({ phone: $event })" maxlength="30" /></div>
        <div class="form-group col-12"><label for="em">E-mail (your sign-in)</label>
          <input class="form-control" id="em" [value]="p().email" disabled />
          <small class="form-text text-muted">To use a different e-mail, ask the administrator.</small></div>
      </div>
      <div class="d-flex flex-wrap" style="gap:.4rem">
        <button class="btn btn-primary btn-responsive" [disabled]="busy() || !p().fullName.trim()">Save</button>
        <a class="btn btn-secondary btn-responsive" routerLink="/change-password"><i class="mdi mdi-lock-reset"></i> Change password</a>
      </div>
    </div></form>
  `,
})
export class ProfilePage {
  private http = inject(HttpClient);
  private auth = inject(AuthService);

  p = signal<Profile>({ fullName: '', email: '', phone: '', jobTitle: '', role: '' });
  busy = signal(false);
  message = signal('');
  error = signal('');

  constructor() {
    this.http.get<Profile>('/api/auth/profile').subscribe({ next: (r) => this.p.set(r), error: (e) => this.error.set(errorMessage(e)) });
  }

  set(change: Partial<Profile>) {
    this.p.update((x) => ({ ...x, ...change }));
  }

  save() {
    this.busy.set(true);
    this.message.set('');
    this.error.set('');
    this.http.put<Profile>('/api/auth/profile', { fullName: this.p().fullName, phone: this.p().phone, jobTitle: this.p().jobTitle }).subscribe({
      next: (r) => {
        this.busy.set(false);
        this.p.set(r);
        this.message.set('Your profile was saved.');
        this.auth.loadMe().subscribe(); // the name in the top bar
      },
      error: (e) => {
        this.busy.set(false);
        this.error.set(errorMessage(e));
      },
    });
  }
}
