import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { Observable, tap } from 'rxjs';
import { CatalogService } from './catalog.service';

export type Role = 'PLATFORM_ADMIN' | 'IMAM' | 'STAFF' | 'COUPLE';

export interface AuthResponse {
  otpRequired: boolean;
  id: number;
  message: string | null;
  token: string | null;
  name: string | null;
  role: Role | null;
  masjidId: number | null;
  mustChangePassword: boolean;
  rememberDays: number;
}

export interface Me {
  id: number;
  email: string;
  name: string;
  role: Role;
  mustChangePassword: boolean;
  phone?: string | null;
  jobTitle?: string | null;
  masjidName?: string;
  masjidAddress?: string;
}

const TOKEN_KEY = 'nikahgator_token';

function readToken(): string | null {
  try {
    return localStorage.getItem(TOKEN_KEY);
  } catch {
    return null;
  }
}

/** The readable message of a failed API call. */
export function errorMessage(e: any): string {
  return e?.error?.error ?? (e?.status === 0 ? 'Cannot reach the server.' : 'Something went wrong. Please try again.');
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private http = inject(HttpClient);
  private router = inject(Router);
  private catalog = inject(CatalogService);

  readonly token = signal<string | null>(readToken());
  readonly me = signal<Me | null>(null);
  readonly signedIn = computed(() => !!this.token());

  login(email: string, password: string): Observable<AuthResponse> {
    return this.http.post<AuthResponse>('/api/auth/login', { email, password }).pipe(tap((r) => this.accept(r)));
  }

  verifyOtp(id: number, otp: string, rememberDevice: boolean): Observable<AuthResponse> {
    return this.http.post<AuthResponse>('/api/auth/verify-otp', { id, otp, rememberDevice }).pipe(tap((r) => this.accept(r)));
  }

  forgotPassword(email: string) {
    return this.http.post<{ message: string }>('/api/auth/forgot-password', { email });
  }

  resetPassword(email: string, code: string, newPassword: string, confirmPassword: string) {
    return this.http.post<{ message: string }>('/api/auth/reset-password', { email, code, newPassword, confirmPassword });
  }

  changePassword(currentPassword: string, newPassword: string, confirmPassword: string) {
    return this.http
      .post<{ message: string }>('/api/auth/change-password', { currentPassword, newPassword, confirmPassword })
      .pipe(tap(() => {
        this.catalog.clear();
        this.me.update((m) => (m ? { ...m, mustChangePassword: false } : m));
      }));
  }

  loadMe(): Observable<Me> {
    return this.http.get<Me>('/api/auth/me').pipe(tap((m) => this.me.set(m)));
  }

  logout(): void {
    this.setToken(null);
    this.me.set(null);
    this.catalog.clear();
    this.router.navigateByUrl('/login');
  }

  /** Where a signed-in user belongs. */
  homeFor(role: Role | undefined): string {
    // imam and staff: the home page picks the first service they may use
    return role === 'PLATFORM_ADMIN' ? '/admin/masjids' : '/';
  }

  private accept(r: AuthResponse): void {
    if (r.token) this.setToken(r.token);
  }

  private setToken(token: string | null): void {
    this.token.set(token);
    try {
      if (token) localStorage.setItem(TOKEN_KEY, token);
      else localStorage.removeItem(TOKEN_KEY);
    } catch {
      /* private mode: the token lives in memory only */
    }
  }
}
