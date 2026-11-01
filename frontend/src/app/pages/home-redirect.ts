import { Component, inject } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../core/auth.service';
import { CatalogService } from '../core/catalog.service';

/** Sends the signed-in user to the first thing they can use. */
@Component({ selector: 'app-home-redirect', template: '' })
export class HomeRedirect {
  constructor() {
    const auth = inject(AuthService);
    const router = inject(Router);
    if (auth.me()?.role === 'PLATFORM_ADMIN' || !auth.me()) {
      router.navigateByUrl('/admin/masjids', { replaceUrl: true });
      return;
    }
    inject(CatalogService).load().subscribe({
      next: (s) => {
        const first = s[0];
        const target = !first ? '/imam/not-available' : first.key === 'NIKAH' ? '/imam/nikahs' : '/imam/svc/' + first.slug;
        router.navigateByUrl(target, { replaceUrl: true });
      },
      error: () => router.navigateByUrl('/imam/not-available', { replaceUrl: true }),
    });
  }
}
