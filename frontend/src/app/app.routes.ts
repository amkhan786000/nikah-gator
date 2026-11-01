import { Routes } from '@angular/router';
import { authGuard, guestGuard, imamOnlyGuard, roleGuard, serviceGuard } from './core/guards';

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
      { path: 'profile', loadComponent: () => import('./pages/profile').then((m) => m.ProfilePage) },
      {
        path: 'imam/staff',
        canActivate: [imamOnlyGuard],
        loadComponent: () => import('./pages/team/staff-list').then((m) => m.StaffList),
      },
      {
        path: 'imam/staff/new',
        canActivate: [imamOnlyGuard],
        loadComponent: () => import('./pages/team/staff-form').then((m) => m.StaffForm),
      },
      {
        path: 'imam/staff/:id',
        canActivate: [imamOnlyGuard],
        loadComponent: () => import('./pages/team/staff-form').then((m) => m.StaffForm),
      },
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
      {
        path: 'imam/svc/juma',
        canActivate: [roleGuard('IMAM'), serviceGuard('JUMA_LETTER')],
        loadComponent: () => import('./pages/svc/juma/juma-list').then((m) => m.JumaList),
      },
      {
        path: 'imam/svc/juma/new',
        canActivate: [roleGuard('IMAM'), serviceGuard('JUMA_LETTER')],
        loadComponent: () => import('./pages/svc/juma/juma-form').then((m) => m.JumaForm),
      },
      {
        path: 'imam/svc/juma/:id/edit',
        canActivate: [roleGuard('IMAM'), serviceGuard('JUMA_LETTER')],
        loadComponent: () => import('./pages/svc/juma/juma-form').then((m) => m.JumaForm),
      },
      {
        path: 'imam/svc/juma/:id',
        canActivate: [roleGuard('IMAM'), serviceGuard('JUMA_LETTER')],
        loadComponent: () => import('./pages/svc/juma/juma-view').then((m) => m.JumaView),
      },
      //
      {
        path: 'imam/svc/community-service',
        canActivate: [roleGuard('IMAM'), serviceGuard('COMMUNITY_SERVICE')],
        loadComponent: () => import('./pages/svc/community-service/cs-list').then((m) => m.CsList),
      },
      {
        path: 'imam/svc/community-service/new',
        canActivate: [roleGuard('IMAM'), serviceGuard('COMMUNITY_SERVICE')],
        loadComponent: () => import('./pages/svc/community-service/cs-form').then((m) => m.CsForm),
      },
      {
        path: 'imam/svc/community-service/:id/edit',
        canActivate: [roleGuard('IMAM'), serviceGuard('COMMUNITY_SERVICE')],
        loadComponent: () => import('./pages/svc/community-service/cs-form').then((m) => m.CsForm),
      },
      {
        path: 'imam/svc/community-service/:id',
        canActivate: [roleGuard('IMAM'), serviceGuard('COMMUNITY_SERVICE')],
        loadComponent: () => import('./pages/svc/community-service/cs-view').then((m) => m.CsView),
      },
      //
      {
        path: 'imam/svc/janazah',
        canActivate: [roleGuard('IMAM'), serviceGuard('JANAZAH')],
        loadComponent: () => import('./pages/svc/janazah/janazah').then((m) => m.Janazah),
      },
      //
      {
        path: 'imam/svc/shahada',
        canActivate: [roleGuard('IMAM'), serviceGuard('SHAHADA')],
        loadComponent: () => import('./pages/svc/shahada/shahada').then((m) => m.Shahada),
      },
      //
      {
        path: 'imam/svc/baby-naming',
        canActivate: [roleGuard('IMAM'), serviceGuard('BABY_NAMING')],
        loadComponent: () => import('./pages/svc/baby-naming/baby-naming').then((m) => m.BabyNaming),
      },
      //
      {
        path: 'imam/svc/counseling',
        canActivate: [roleGuard('IMAM'), serviceGuard('COUNSELING')],
        loadComponent: () => import('./pages/svc/counseling/counseling-list').then((m) => m.CounselingList),
      },
      {
        path: 'imam/svc/counseling/new',
        canActivate: [roleGuard('IMAM'), serviceGuard('COUNSELING')],
        loadComponent: () => import('./pages/svc/counseling/counseling-form').then((m) => m.CounselingForm),
      },
      {
        path: 'imam/svc/counseling/:id/edit',
        canActivate: [roleGuard('IMAM'), serviceGuard('COUNSELING')],
        loadComponent: () => import('./pages/svc/counseling/counseling-form').then((m) => m.CounselingForm),
      },
      {
        path: 'imam/svc/counseling/:id',
        canActivate: [roleGuard('IMAM'), serviceGuard('COUNSELING')],
        loadComponent: () => import('./pages/svc/counseling/counseling-view').then((m) => m.CounselingView),
      },
      //
      {
        path: 'imam/svc/hall-booking',
        canActivate: [roleGuard('IMAM'), serviceGuard('HALL_BOOKING')],
        loadComponent: () => import('./pages/svc/hall-booking/hall-list').then((m) => m.HallList),
      },
      {
        path: 'imam/svc/hall-booking/new',
        canActivate: [roleGuard('IMAM'), serviceGuard('HALL_BOOKING')],
        loadComponent: () => import('./pages/svc/hall-booking/hall-form').then((m) => m.HallForm),
      },
      {
        path: 'imam/svc/hall-booking/:id/edit',
        canActivate: [roleGuard('IMAM'), serviceGuard('HALL_BOOKING')],
        loadComponent: () => import('./pages/svc/hall-booking/hall-form').then((m) => m.HallForm),
      },
      //
      {
        path: 'imam/svc/letters',
        canActivate: [roleGuard('IMAM'), serviceGuard('GENERAL_LETTER')],
        loadComponent: () => import('./pages/svc/letters/letters-list').then((m) => m.LettersList),
      },
      {
        path: 'imam/svc/letters/new',
        canActivate: [roleGuard('IMAM'), serviceGuard('GENERAL_LETTER')],
        loadComponent: () => import('./pages/svc/letters/letter-editor').then((m) => m.LetterEditor),
      },
      {
        path: 'imam/svc/letters/:id',
        canActivate: [roleGuard('IMAM'), serviceGuard('GENERAL_LETTER')],
        loadComponent: () => import('./pages/svc/letters/letter-editor').then((m) => m.LetterEditor),
      },
      //
      {
        path: 'imam/svc/divorce',
        canActivate: [roleGuard('IMAM'), serviceGuard('DIVORCE')],
        loadComponent: () => import('./pages/svc/divorce/divorce-list').then((m) => m.DivorceList),
      },
      {
        path: 'imam/svc/divorce/new',
        canActivate: [roleGuard('IMAM'), serviceGuard('DIVORCE')],
        loadComponent: () => import('./pages/svc/divorce/divorce-form').then((m) => m.DivorceForm),
      },
      {
        path: 'imam/svc/divorce/:id/edit',
        canActivate: [roleGuard('IMAM'), serviceGuard('DIVORCE')],
        loadComponent: () => import('./pages/svc/divorce/divorce-form').then((m) => m.DivorceForm),
      },
      {
        path: 'imam/svc/divorce/:id',
        canActivate: [roleGuard('IMAM'), serviceGuard('DIVORCE')],
        loadComponent: () => import('./pages/svc/divorce/divorce-view').then((m) => m.DivorceView),
      },
      //
      { path: '', pathMatch: 'full', loadComponent: () => import('./pages/home-redirect').then((m) => m.HomeRedirect) },
    ],
  },
  { path: '**', redirectTo: '' },
];
