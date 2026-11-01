import { Component, HostListener, effect, inject, signal, untracked } from '@angular/core';
import { NavigationEnd, Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { filter } from 'rxjs';
import { AuthService } from '../core/auth.service';
import { CatalogService } from '../core/catalog.service';

interface NavLink { path: string; label: string; icon: string }

/** App layout (same as Rahbar): dark brand sidebar + sticky top bar + content area. Styles are in src/styles.scss. */
@Component({
  selector: 'app-shell',
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  template: `
    <div class="app" [class.app--collapsed]="collapsed()" [class.app--mobile-open]="mobileOpen()">
      <aside class="app-sidebar no-print">
        <a class="app-brand" routerLink="/" (click)="mobileOpen.set(false)">
          <img class="brand-logo" src="assets/icons/favicon.svg" alt="" />
          <span class="brand-text"><strong>Masjidly</strong><small>{{ auth.me()?.masjidName || 'Platform admin' }}</small></span>
        </a>
        <nav class="app-nav">
          <div class="nav-section">{{ auth.me()?.role === 'PLATFORM_ADMIN' ? 'Administration' : auth.me()?.role === 'STAFF' ? 'Staff' : 'Imam' }}</div>
          @for (l of links(); track l.path) {
            <a class="nav-item" [routerLink]="l.path" routerLinkActive="active" [title]="collapsed() ? l.label : ''"
               (click)="mobileOpen.set(false)">
              <i class="mdi" [class]="l.icon"></i><span>{{ l.label }}</span>
            </a>
          }
        </nav>
        <div class="sidebar-foot">Services for your masjid</div>
      </aside>
      <div class="app-backdrop no-print" (click)="mobileOpen.set(false)"></div>

      <div class="app-main">
        <header class="app-topbar no-print">
          <button type="button" class="icon-btn" (click)="toggleSidebar()" aria-label="Toggle menu"><i class="mdi mdi-menu"></i></button>
          <div class="topbar-crumbs"><small>{{ auth.me()?.masjidName || 'Masjidly' }}</small><strong>{{ label() }}</strong></div>
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
                  <a routerLink="/profile" (click)="menuOpen.set(false)"><i class="mdi mdi-account-circle-outline"></i><span>My profile</span></a>
                  <a routerLink="/change-password" (click)="menuOpen.set(false)"><i class="mdi mdi-lock-reset"></i><span>Change password</span></a>
                  <a href="#" class="danger" (click)="$event.preventDefault(); auth.logout()"><i class="mdi mdi-logout-variant"></i><span>Sign out</span></a>
                </div>
              }
            </div>
          </div>
        </header>

        <main class="app-content" [class.view-only]="!!viewOnlyLabel()">
          @if (viewOnlyLabel(); as vo) {
            <div class="alert alert-info no-print"><i class="mdi mdi-eye-outline"></i> You have view-only access to {{ vo }}.</div>
          }
          <router-outlet />
        </main>
        <footer class="app-footer no-print"><span>© {{ year }} Masjidly</span></footer>
      </div>
    </div>
  `,
})
export class Shell {
  protected auth = inject(AuthService);
  private catalog = inject(CatalogService);
  private router = inject(Router);

  collapsed = signal(false);
  mobileOpen = signal(false);
  menuOpen = signal(false);
  label = signal('');
  url = signal('');
  readonly year = new Date().getFullYear();

  constructor() {
    this.router.events.pipe(filter((e) => e instanceof NavigationEnd)).subscribe(() => {
      this.mobileOpen.set(false);
      this.label.set(this.labelFor(this.router.url));
      this.url.set(this.router.url);
    });
    this.url.set(this.router.url);
    this.label.set(this.labelFor(this.router.url));
    // The imam's menu comes from the catalog. It loads once the profile is known and the password is no longer
    // a temporary one (the API refuses everything else), and again after the catalog is cleared.
    effect(() => {
      const me = this.auth.me();
      if ((me?.role === 'IMAM' || me?.role === 'STAFF') && !me.mustChangePassword && this.catalog.services() === null) {
        untracked(() => this.catalog.load(true).subscribe({ error: () => undefined }));
      }
    });
  }

  @HostListener('document:click')
  closeMenu() {
    this.menuOpen.set(false);
  }

  links(): NavLink[] {
    if (this.auth.me()?.role === 'PLATFORM_ADMIN') {
      return [{ path: '/admin/masjids', label: 'Masjids', icon: 'mdi-home-city-outline' },
        { path: '/admin/activity', label: 'Activity log', icon: 'mdi-history' }];
    }
    // Only what the platform admin has switched on for this imam (null while loading).
    const on = this.catalog.services() ?? [];
    const links: NavLink[] = [];
    for (const s of on) {
      if (s.key === 'NIKAH') {
        links.push({ path: '/imam/nikahs', label: 'Nikahs', icon: 'mdi-book-open-page-variant' });
        // the masjid's own settings and certificate design are the imam's to change
        if (this.auth.me()?.role === 'IMAM') {
          links.push(
            { path: '/imam/settings', label: 'Masjid settings', icon: 'mdi-settings' },
            { path: '/imam/certificate-design', label: 'Certificate design', icon: 'mdi-certificate-outline' },
          );
        }
      } else {
        links.push({ path: '/imam/svc/' + s.slug, label: s.label, icon: s.icon });
      }
    }
    if (this.auth.me()?.role === 'IMAM') {
      links.push({ path: '/imam/staff', label: 'Team', icon: 'mdi-account-multiple-outline' },
        { path: '/imam/activity', label: 'Activity log', icon: 'mdi-history' });
    }
    return links;
  }

  /** The name of the service on screen when the signed-in user may only look at it. */
  viewOnlyLabel(): string {
    const u = this.url().split(/[?#]/)[0];
    const svc = /^\/imam\/svc\/([^/]+)/.exec(u);
    const nikahArea = /^\/imam\/(nikahs|settings|certificate-design)/.test(u);
    const s = this.catalog.services()?.find((x) => (svc ? x.slug === svc[1] : nikahArea && x.key === 'NIKAH'));
    return s?.level === 'VIEW' ? s.label : '';
  }

  roleLabel(): string {
    const me = this.auth.me();
    if (me?.role === 'PLATFORM_ADMIN') return 'Platform admin';
    return me?.jobTitle || (me?.role === 'STAFF' ? 'Staff' : 'Imam');
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
    if (u.startsWith('/admin/masjids/') && u.endsWith('/services')) return 'Services';
    if (u.startsWith('/admin/masjids')) return 'Masjids';
    if (u.startsWith('/imam/not-available')) return 'Not available';
    if (u.startsWith('/imam/staff')) return 'Team';
    if (u.startsWith('/imam/activity') || u.startsWith('/admin/activity')) return 'Activity log';
    if (u.startsWith('/profile')) return 'My profile';
    const svc = /^\/imam\/svc\/([^/]+)/.exec(u);
    if (svc) return this.catalog.services()?.find((x) => x.slug === svc[1])?.label ?? '';
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
