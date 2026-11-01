import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService, errorMessage } from '../core/auth.service';
import { AuthLayout } from '../shared/auth-layout';

@Component({
  selector: 'app-forgot-password',
  imports: [FormsModule, RouterLink, AuthLayout],
  template: `
    <app-auth-layout heading="Reset your password"
      [subheading]="step() === 'email' ? 'We will email you a code to set a new password.' : (info() || 'Enter the code and choose a new password.')">
      @if (error()) { <div class="alert alert-danger">{{ error() }}</div> }

      @if (step() === 'email') {
        <form (ngSubmit)="send()">
          <div class="form-group">
            <label for="email">Email address</label>
            <div class="input-icon">
              <i class="mdi mdi-email-outline"></i>
              <input class="form-control" id="email" type="email" name="email" [(ngModel)]="email" required />
            </div>
          </div>
          <button class="btn btn-primary btn-block mt-4" [disabled]="busy()">Send reset code</button>
        </form>
      } @else {
        <form (ngSubmit)="reset()">
          <div class="form-group">
            <label for="code">Code from your email</label>
            <input class="form-control otp-input" id="code" name="code" [(ngModel)]="code" inputmode="numeric" maxlength="6" required />
          </div>
          <div class="form-group">
            <label for="pw">New password</label>
            <input class="form-control" id="pw" type="password" name="pw" [(ngModel)]="newPassword" autocomplete="new-password" required />
          </div>
          <div class="form-group">
            <label for="pw2">Confirm new password</label>
            <input class="form-control" id="pw2" type="password" name="pw2" [(ngModel)]="confirm" autocomplete="new-password" required />
            <small class="form-text text-muted">At least 8 characters, with letters and numbers.</small>
          </div>
          <button class="btn btn-primary btn-block" [disabled]="busy()">Change password</button>
        </form>
      }
      <div class="auth-links center"><a routerLink="/login">Back to sign in</a></div>
    </app-auth-layout>
  `,
})
export class ForgotPassword {
  private auth = inject(AuthService);
  private router = inject(Router);

  email = '';
  code = '';
  newPassword = '';
  confirm = '';

  step = signal<'email' | 'code'>('email');
  busy = signal(false);
  error = signal('');
  info = signal('');

  send() {
    this.busy.set(true);
    this.error.set('');
    this.auth.forgotPassword(this.email.trim()).subscribe({
      next: (r) => {
        this.busy.set(false);
        this.info.set(r.message);
        this.step.set('code');
      },
      error: (e) => {
        this.busy.set(false);
        this.error.set(errorMessage(e));
      },
    });
  }

  reset() {
    this.busy.set(true);
    this.error.set('');
    this.auth.resetPassword(this.email.trim(), this.code.trim(), this.newPassword, this.confirm).subscribe({
      next: () => this.router.navigateByUrl('/login'),
      error: (e) => {
        this.busy.set(false);
        this.error.set(errorMessage(e));
      },
    });
  }
}
