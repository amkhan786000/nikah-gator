import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { Observable, map, of, tap } from 'rxjs';

export interface EnabledService {
  key: string;
  slug: string;
  label: string;
  icon: string;
}

/** The services the platform admin has switched on for the signed-in imam (drives the menu and route guards). */
@Injectable({ providedIn: 'root' })
export class CatalogService {
  private http = inject(HttpClient);

  /** null until loaded. */
  readonly services = signal<EnabledService[] | null>(null);

  load(force = false): Observable<EnabledService[]> {
    const cached = this.services();
    if (cached && !force) return of(cached);
    return this.http.get<EnabledService[]>('/api/imam/catalog').pipe(tap((s) => this.services.set(s)));
  }

  has(key: string): Observable<boolean> {
    return this.load().pipe(map((s) => s.some((x) => x.key === key)));
  }

  clear(): void {
    this.services.set(null);
  }
}
