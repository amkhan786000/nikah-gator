import { Component, input } from '@angular/core';

/** Split-screen frame for the sign-in pages: brand story on the left, the form card on the right. */
@Component({
  selector: 'app-auth-layout',
  template: `
    <div class="auth">
      <section class="auth-hero">
        <div class="app-brand" style="padding:0;border:0">
          <img class="brand-logo" src="assets/icons/favicon.svg" alt="" />
          <span class="brand-text"><strong>Masjidly</strong><small>Services for your masjid</small></span>
        </div>
        <div>
          <h1>Your masjid, <span>served with care.</span></h1>
          <p>Masjidly helps your imam register nikahs, issue certificates and letters, and keep the records of your
             masjid in one place.</p>
          <div class="auth-points">
            <div><i class="mdi mdi-book-open-page-variant"></i>Nikah records</div>
            <div><i class="mdi mdi-certificate-outline"></i>Certificates &amp; letters</div>
            <div><i class="mdi mdi-home-city-outline"></i>Your masjid</div>
          </div>
        </div>
        <small>© {{ year }} Masjidly</small>
      </section>
      <section class="auth-panel">
        <div class="auth-card">
          <div class="auth-mobile-brand"><img class="brand-logo" src="assets/icons/favicon.svg" alt="" />Masjidly</div>
          <h2>{{ heading() }}</h2>
          @if (subheading()) { <p class="auth-sub">{{ subheading() }}</p> }
          <ng-content />
        </div>
      </section>
    </div>
  `,
})
export class AuthLayout {
  heading = input('');
  subheading = input('');
  readonly year = new Date().getFullYear();
}
