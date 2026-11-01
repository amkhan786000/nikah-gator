import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService, errorMessage } from '../core/auth.service';

@Component({
  selector: 'app-change-password',
  imports: [FormsModule],
  template: `
    <div class="page-title-box"><h4 class="page-title">Change password</h4></div>
    <div class="card" style="max-width:480px"><div class="card-body">
      @if (auth.me()?.mustChangePassword) {
        <div class="alert alert-info">You are using a temporary password. Please choose your own to continue.</div>
      }
      @if (error()) { <div class="alert alert-danger">{{ error() }}</div> }
      <form (ngSubmit)="save()">
        <div class="form-group">
          <label for="cur">Current password</label>
          <input class="form-control" id="cur" type="password" name="cur" [(ngModel)]="current" autocomplete="current-password" required />
        </div>
        <div class="form-group">
          <label for="new">New password</label>
          <input class="form-control" id="new" type="password" name="new" [(ngModel)]="newPassword" autocomplete="new-password" required />
        </div>
        <div class="form-group">
          <label for="conf">Confirm new password</label>
          <input class="form-control" id="conf" type="password" name="conf" [(ngModel)]="confirm" autocomplete="new-password" required />
          <small class="form-text text-muted">At least 8 characters, with letters and numbers.</small>
        </div>
        <button class="btn btn-primary btn-responsive" [disabled]="busy()">Change password</button>
      </form>
    </div></div>
  `,
})
export class ChangePassword {
  protected auth = inject(AuthService);
  private router = inject(Router);

  current = '';
  newPassword = '';
  confirm = '';
  busy = signal(false);
  error = signal('');

  save() {
    this.busy.set(true);
    this.error.set('');
    this.auth.changePassword(this.current, this.newPassword, this.confirm).subscribe({
      next: () => {
        this.busy.set(false);
        this.current = this.newPassword = this.confirm = '';
        this.router.navigateByUrl(this.auth.homeFor(this.auth.me()?.role));
      },
      error: (e) => {
        this.busy.set(false);
        this.error.set(errorMessage(e));
      },
    });
  }
}
