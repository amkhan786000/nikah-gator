import { Routes } from '@angular/router';
import { authGuard, guestGuard, roleGuard } from './core/guards';

export const routes: Routes = [
  { path: 'login', canActivate: [guestGuard], loadComponent: () => import('./pages/login').then((m) => m.Login) },
  { path: 'forgot-password', canActivate: [guestGuard], loadComponent: () => import('./pages/forgot-password').then((m) => m.ForgotPassword) },
  {
    path: '',
    canActivate: [authGuard],
    loadComponent: () => import('./pages/shell').then((m) => m.Shell),
    children: [
      { path: 'change-password', loadComponent: () => import('./pages/change-password').then((m) => m.ChangePassword) },
      {
        path: 'admin/masjids',
        canActivate: [roleGuard('PLATFORM_ADMIN')],
        loadComponent: () => import('./pages/admin-masjids').then((m) => m.AdminMasjids),
      },
      {
        path: 'imam/nikahs',
        canActivate: [roleGuard('IMAM')],
        loadComponent: () => import('./pages/nikah-list').then((m) => m.NikahList),
      },
      {
        path: 'imam/nikahs/new',
        canActivate: [roleGuard('IMAM')],
        loadComponent: () => import('./pages/nikah-form').then((m) => m.NikahForm),
      },
      {
        path: 'imam/nikahs/:id/edit',
        canActivate: [roleGuard('IMAM')],
        loadComponent: () => import('./pages/nikah-form').then((m) => m.NikahForm),
      },
      {
        path: 'imam/nikahs/:id',
        canActivate: [roleGuard('IMAM')],
        loadComponent: () => import('./pages/nikah-view').then((m) => m.NikahView),
      },
      { path: '', pathMatch: 'full', loadComponent: () => import('./pages/home-redirect').then((m) => m.HomeRedirect) },
    ],
  },
  { path: '**', redirectTo: '' },
];
