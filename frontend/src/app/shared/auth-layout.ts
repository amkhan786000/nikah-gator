import { Component, input } from '@angular/core';

/** Split-screen frame for the sign-in pages: brand story on the left, the form card on the right. */
@Component({
  selector: 'app-auth-layout',
  template: `
    <div class="auth">
      <section class="auth-hero">
        <div class="app-brand" style="padding:0;border:0">
          <span class="brand-mark">ng</span>
          <span class="brand-text"><strong>Nikah Gator</strong><small>Nikah records for masjids</small></span>
        </div>
        <div>
          <h1>Every nikah, <span>recorded with care.</span></h1>
          <p>Nikah Gator helps your imam register nikahs, keep the records of your masjid in one place, and print the
             certificate for the couple.</p>
          <div class="auth-points">
            <div><i class="mdi mdi-book-open-page-variant"></i>Nikah records</div>
            <div><i class="mdi mdi-certificate-outline"></i>Certificates</div>
            <div><i class="mdi mdi-home-city-outline"></i>Your masjid</div>
          </div>
        </div>
        <small>© {{ year }} Nikah Gator</small>
      </section>
      <section class="auth-panel">
        <div class="auth-card">
          <div class="auth-mobile-brand"><span class="brand-mark">ng</span>Nikah Gator</div>
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
