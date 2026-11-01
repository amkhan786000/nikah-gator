import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { AuthService } from './auth.service';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);
  const router = inject(Router);
  const token = auth.token();
  const authed = token && req.url.startsWith('/api/') ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } }) : req;
  return next(authed).pipe(
    catchError((e: HttpErrorResponse) => {
      const isAuthCall = req.url.startsWith('/api/auth/login') || req.url.startsWith('/api/auth/verify-otp');
      if (e.status === 401 && token && !isAuthCall) {
        auth.logout();
      } else if (e.status === 403 && e.error?.code === 'PASSWORD_CHANGE_REQUIRED') {
        router.navigateByUrl('/change-password');
      }
      return throwError(() => e);
    }),
  );
};
