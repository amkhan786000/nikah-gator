import { DatePipe } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { Component, effect, inject, input, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AuthService } from '../core/auth.service';

interface AuditInfo {
  createdBy: string | null;
  createdAt: string | null;
  updatedBy: string | null;
  updatedAt: string | null;
}

/**
 * "Created by X on ..., last changed by Y on ..." for one record, plus (for the imam) a link to its history in the
 * activity log. Drop it on any detail page: <app-audit-info type="Letter" [id]="id" />. `type` is the entity name.
 */
@Component({
  selector: 'app-audit-info',
  imports: [DatePipe, RouterLink],
  template: `
    @if (info(); as i) {
      <p class="text-muted small mt-3 mb-0 no-print">
        Created {{ i.createdBy ? 'by ' + i.createdBy : 'by the system' }}
        @if (i.createdAt) { on {{ i.createdAt | date: 'medium' }} }
        @if (i.updatedAt && i.updatedAt !== i.createdAt) {
          · Last changed {{ i.updatedBy ? 'by ' + i.updatedBy : 'by the system' }} on {{ i.updatedAt | date: 'medium' }}
        }
        @if (isImam()) {
          · <a routerLink="/imam/activity" [queryParams]="{ entityType: type(), entityId: id() }">History</a>
        }
      </p>
    }
  `,
})
export class AuditInfoView {
  private http = inject(HttpClient);
  private auth = inject(AuthService);

  type = input.required<string>();
  id = input.required<number | string | null | undefined>();
  info = signal<AuditInfo | null>(null);

  constructor() {
    effect(() => {
      const id = this.id();
      if (id == null || id === '') return;
      this.http.get<AuditInfo>(`/api/imam/audit-info/${this.type()}/${id}`).subscribe({
        next: (r) => this.info.set(r),
        error: () => this.info.set(null), // not worth an error banner
      });
    });
  }

  isImam(): boolean {
    return this.auth.me()?.role === 'IMAM';
  }
}
