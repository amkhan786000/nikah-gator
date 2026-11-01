import { Routes } from '@angular/router';
import { authGuard, guestGuard, roleGuard, serviceGuard } from './core/guards';

export const routes: Routes = [
  { path: 'login', canActivate: [guestGuard], loadComponent: () => import('./pages/login').then((m) => m.Login) },
  { path: 'forgot-password', canActivate: [guestGuard], loadComponent: () => import('./pages/forgot-password').then((m) => m.ForgotPassword) },
  { path: 'verify/:number', loadComponent: () => import('./pages/verify-certificate').then((m) => m.VerifyCertificate) },
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
        path: 'admin/masjids/:id/services',
        canActivate: [roleGuard('PLATFORM_ADMIN')],
        loadComponent: () => import('./pages/admin-services').then((m) => m.AdminServices),
      },
      {
        path: 'imam/not-available',
        canActivate: [roleGuard('IMAM')],
        loadComponent: () => import('./pages/service-disabled').then((m) => m.ServiceDisabled),
      },
      {
        path: 'imam/nikahs',
        canActivate: [roleGuard('IMAM'), serviceGuard('NIKAH')],
        loadComponent: () => import('./pages/nikah-list').then((m) => m.NikahList),
      },
      {
        path: 'imam/nikahs/new',
        canActivate: [roleGuard('IMAM'), serviceGuard('NIKAH')],
        loadComponent: () => import('./pages/nikah-form').then((m) => m.NikahForm),
      },
      {
        path: 'imam/nikahs/:id/edit',
        canActivate: [roleGuard('IMAM'), serviceGuard('NIKAH')],
        loadComponent: () => import('./pages/nikah-form').then((m) => m.NikahForm),
      },
      {
        path: 'imam/nikahs/:id',
        canActivate: [roleGuard('IMAM'), serviceGuard('NIKAH')],
        loadComponent: () => import('./pages/nikah-view').then((m) => m.NikahView),
      },
      {
        path: 'imam/nikahs/:id/payments/:pid/receipt',
        canActivate: [roleGuard('IMAM'), serviceGuard('NIKAH')],
        loadComponent: () => import('./pages/payment-receipt').then((m) => m.PaymentReceipt),
      },
      {
        path: 'imam/settings',
        canActivate: [roleGuard('IMAM'), serviceGuard('NIKAH')],
        loadComponent: () => import('./pages/masjid-settings').then((m) => m.MasjidSettingsPage),
      },
      {
        path: 'imam/certificate-design',
        canActivate: [roleGuard('IMAM'), serviceGuard('NIKAH')],
        loadComponent: () => import('./pages/certificate-designer').then((m) => m.CertificateDesigner),
      },
      // Each service adds its own routes by replacing its anchor line below (path 'imam/svc/<slug>', serviceGuard).
      // svc-route:juma
      //
      // svc-route:community-service
      //
      // svc-route:janazah
      //
      // svc-route:shahada
      //
      // svc-route:baby-naming
      //
      // svc-route:counseling
      //
      // svc-route:hall-booking
      //
      // svc-route:letters
      //
      { path: '', pathMatch: 'full', loadComponent: () => import('./pages/home-redirect').then((m) => m.HomeRedirect) },
    ],
  },
  { path: '**', redirectTo: '' },
];
