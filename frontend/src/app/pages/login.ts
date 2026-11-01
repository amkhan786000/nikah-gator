import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService, errorMessage } from '../core/auth.service';
import { AuthLayout } from '../shared/auth-layout';

@Component({
  selector: 'app-login',
  imports: [FormsModule, RouterLink, AuthLayout],
  template: `
    <app-auth-layout [heading]="step() === 'password' ? 'Welcome back' : 'Check your email'"
      [subheading]="step() === 'password' ? 'Sign in to manage your masjid’s nikah records.' : (info() || 'Enter the 6-digit code we emailed you.')">
      @if (error()) { <div class="alert alert-danger">{{ error() }}</div> }

      @if (step() === 'password') {
        <form (ngSubmit)="signIn()">
          <div class="form-group">
            <label for="email">Email address</label>
            <div class="input-icon">
              <i class="mdi mdi-email-outline"></i>
              <input class="form-control" id="email" type="email" name="email" [(ngModel)]="email" placeholder="you@example.com"
                     autocomplete="email" required />
            </div>
          </div>
          <div class="form-group">
            <div class="d-flex justify-content-between align-items-center">
              <label for="password">Password</label>
              <a routerLink="/forgot-password" class="small font-weight-bold mb-1">Forgot password?</a>
            </div>
            <div class="input-icon">
              <i class="mdi mdi-lock-outline"></i>
              <input class="form-control" id="password" type="password" name="password" [(ngModel)]="password"
                     placeholder="Your password" autocomplete="current-password" required />
            </div>
          </div>
          <button class="btn btn-primary btn-block mt-4" type="submit" [disabled]="busy()">
            @if (busy()) { <span class="spinner-border spinner-border-sm mr-2"></span> }{{ busy() ? 'Signing in…' : 'Sign in' }}
          </button>
        </form>
      } @else {
        <form (ngSubmit)="verify()">
          <div class="form-group">
            <input class="form-control otp-input" name="otp" [(ngModel)]="otp" inputmode="numeric" autocomplete="one-time-code"
                   maxlength="6" placeholder="······" required aria-label="Sign-in code" />
          </div>
          @if (rememberDays() > 0) {
            <div class="custom-control custom-checkbox mb-3">
              <input type="checkbox" class="custom-control-input" id="remember" name="remember" [(ngModel)]="remember" />
              <label class="custom-control-label" for="remember">Remember this device for {{ rememberDays() }} days (no code needed on this browser)</label>
            </div>
          }
          <button class="btn btn-primary btn-block" type="submit" [disabled]="busy()">
            @if (busy()) { <span class="spinner-border spinner-border-sm mr-2"></span> }{{ busy() ? 'Checking…' : 'Verify and sign in' }}
          </button>
        </form>
        <div class="auth-links center"><a href="#" (click)="$event.preventDefault(); back()">Use a different account</a></div>
      }
    </app-auth-layout>
  `,
})
export class Login {
  private auth = inject(AuthService);
  private router = inject(Router);

  email = '';
  password = '';
  otp = '';
  remember = false;
  userId = 0;

  step = signal<'password' | 'otp'>('password');
  busy = signal(false);
  error = signal('');
  info = signal('');
  rememberDays = signal(0);

  signIn() {
    this.busy.set(true);
    this.error.set('');
    this.auth.login(this.email.trim(), this.password).subscribe({
      next: (r) => {
        this.busy.set(false);
        if (r.otpRequired) {
          this.userId = r.id;
          this.rememberDays.set(r.rememberDays);
          this.info.set(r.message ?? '');
          this.step.set('otp');
        } else {
          this.done();
        }
      },
      error: (e) => {
        this.busy.set(false);
        this.error.set(errorMessage(e));
      },
    });
  }

  verify() {
    this.busy.set(true);
    this.error.set('');
    this.auth.verifyOtp(this.userId, this.otp.trim(), this.remember).subscribe({
      next: () => this.done(),
      error: (e) => {
        this.busy.set(false);
        this.error.set(errorMessage(e));
      },
    });
  }

  back() {
    this.step.set('password');
    this.otp = '';
    this.password = '';
    this.info.set('');
    this.error.set('');
  }

  private done() {
    this.auth.loadMe().subscribe({
      next: (m) => this.router.navigateByUrl(m.mustChangePassword ? '/change-password' : this.auth.homeFor(m.role)),
      error: (e) => {
        this.busy.set(false);
        this.error.set(errorMessage(e));
      },
    });
  }
}
