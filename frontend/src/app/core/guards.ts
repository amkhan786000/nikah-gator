import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { map, of, catchError } from 'rxjs';
import { AuthService, Role } from './auth.service';
import { CatalogService } from './catalog.service';

/** Signed in (loads the profile once) and, if needed, forced through the password change first. */
export const authGuard: CanActivateFn = (_route, state) => {
  const auth = inject(AuthService);
  const router = inject(Router);
  if (!auth.signedIn()) return router.parseUrl('/login');
  const check = (mustChange: boolean) =>
    mustChange && state.url !== '/change-password' ? router.parseUrl('/change-password') : true;
  const me = auth.me();
  if (me) return check(me.mustChangePassword);
  return auth.loadMe().pipe(
    map((m) => check(m.mustChangePassword)),
    catchError(() => of(router.parseUrl('/login'))),
  );
};

export const roleGuard =
  (role: Role): CanActivateFn =>
  () => {
    const auth = inject(AuthService);
    const router = inject(Router);
    return auth.me()?.role === role ? true : router.parseUrl(auth.homeFor(auth.me()?.role));
  };

export const guestGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  return auth.signedIn() ? inject(Router).parseUrl(auth.homeFor(auth.me()?.role)) : true;
};

/** The imam may open this page only while the admin has the service switched on. */
export const serviceGuard =
  (key: string): CanActivateFn =>
  () => {
    const router = inject(Router);
    return inject(CatalogService).has(key).pipe(
      map((on) => on || router.parseUrl('/imam/not-available')),
      catchError(() => of(true)),
    );
  };
