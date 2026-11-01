import { Component, HostListener, inject, signal } from '@angular/core';
import { NavigationEnd, Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { filter } from 'rxjs';
import { AuthService } from '../core/auth.service';

interface NavLink { path: string; label: string; icon: string }

/** App layout (same as Rahbar): dark brand sidebar + sticky top bar + content area. Styles are in src/styles.scss. */
@Component({
  selector: 'app-shell',
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  template: `
    <div class="app" [class.app--collapsed]="collapsed()" [class.app--mobile-open]="mobileOpen()">
      <aside class="app-sidebar no-print">
        <a class="app-brand" routerLink="/" (click)="mobileOpen.set(false)">
          <span class="brand-mark">ng</span>
          <span class="brand-text"><strong>Nikah Gator</strong><small>{{ auth.me()?.masjidName || 'Platform admin' }}</small></span>
        </a>
        <nav class="app-nav">
          <div class="nav-section">{{ auth.me()?.role === 'PLATFORM_ADMIN' ? 'Administration' : 'Imam' }}</div>
          @for (l of links(); track l.path) {
            <a class="nav-item" [routerLink]="l.path" routerLinkActive="active" [title]="collapsed() ? l.label : ''"
               (click)="mobileOpen.set(false)">
              <i class="mdi" [class]="l.icon"></i><span>{{ l.label }}</span>
            </a>
          }
        </nav>
        <div class="sidebar-foot">Nikah records for your masjid</div>
      </aside>
      <div class="app-backdrop no-print" (click)="mobileOpen.set(false)"></div>

      <div class="app-main">
        <header class="app-topbar no-print">
          <button type="button" class="icon-btn" (click)="toggleSidebar()" aria-label="Toggle menu"><i class="mdi mdi-menu"></i></button>
          <div class="topbar-crumbs"><small>{{ auth.me()?.masjidName || 'Nikah Gator' }}</small><strong>{{ label() }}</strong></div>
          <div class="topbar-right">
            <div class="user-wrap" (click)="$event.stopPropagation()">
              <div class="user-chip" role="button" tabindex="0" (click)="menuOpen.set(!menuOpen())" (keydown.enter)="menuOpen.set(!menuOpen())">
                <span class="avatar">{{ initials() }}</span>
                <span class="user-meta"><strong>{{ auth.me()?.name }}</strong><small>{{ roleLabel() }}</small></span>
                <i class="mdi mdi-chevron-down"></i>
              </div>
              @if (menuOpen()) {
                <div class="user-menu">
                  <div class="user-menu-head"><strong>{{ auth.me()?.name }}</strong><small>{{ auth.me()?.email }}</small></div>
                  <a routerLink="/change-password" (click)="menuOpen.set(false)"><i class="mdi mdi-lock-reset"></i><span>Change password</span></a>
                  <a href="#" class="danger" (click)="$event.preventDefault(); auth.logout()"><i class="mdi mdi-logout-variant"></i><span>Sign out</span></a>
                </div>
              }
            </div>
          </div>
        </header>

        <main class="app-content"><router-outlet /></main>
        <footer class="app-footer no-print"><span>© {{ year }} Nikah Gator</span></footer>
      </div>
    </div>
  `,
})
export class Shell {
  protected auth = inject(AuthService);
  private router = inject(Router);

  collapsed = signal(false);
  mobileOpen = signal(false);
  menuOpen = signal(false);
  label = signal('');
  readonly year = new Date().getFullYear();

  constructor() {
    this.router.events.pipe(filter((e) => e instanceof NavigationEnd)).subscribe(() => {
      this.mobileOpen.set(false);
      this.label.set(this.labelFor(this.router.url));
    });
    this.label.set(this.labelFor(this.router.url));
  }

  @HostListener('document:click')
  closeMenu() {
    this.menuOpen.set(false);
  }

  links(): NavLink[] {
    return this.auth.me()?.role === 'PLATFORM_ADMIN'
      ? [{ path: '/admin/masjids', label: 'Masjids', icon: 'mdi-home-city-outline' }]
      : [
          { path: '/imam/nikahs', label: 'Nikahs', icon: 'mdi-book-open-page-variant' },
          { path: '/imam/settings', label: 'Masjid settings', icon: 'mdi-settings' },
          { path: '/imam/certificate-design', label: 'Certificate design', icon: 'mdi-certificate-outline' },
        ];
  }

  roleLabel(): string {
    return this.auth.me()?.role === 'PLATFORM_ADMIN' ? 'Platform admin' : 'Imam';
  }

  initials(): string {
    return (this.auth.me()?.name ?? '?').split(/\s+/).map((w) => w[0]).slice(0, 2).join('').toUpperCase();
  }

  toggleSidebar() {
    if (window.innerWidth < 992) this.mobileOpen.set(!this.mobileOpen());
    else this.collapsed.set(!this.collapsed());
  }

  private labelFor(url: string): string {
    const u = url.split(/[?#]/)[0];
    if (u.startsWith('/admin/masjids')) return 'Masjids';
    if (u.startsWith('/imam/settings')) return 'Masjid settings';
    if (u.endsWith('/new')) return 'Register nikah';
    if (u.endsWith('/edit')) return 'Edit nikah';
    if (u.startsWith('/imam/nikahs/')) return 'Nikah';
    if (u.startsWith('/imam/nikahs')) return 'Nikahs';
    if (u.startsWith('/imam/certificate-design')) return 'Certificate design';
    if (u.startsWith('/change-password')) return 'Change password';
    return '';
  }
}
