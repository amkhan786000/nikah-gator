import { Component, inject } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../core/auth.service';

@Component({ selector: 'app-home-redirect', template: '' })
export class HomeRedirect {
  constructor() {
    const auth = inject(AuthService);
    inject(Router).navigateByUrl(auth.homeFor(auth.me()?.role), { replaceUrl: true });
  }
}
