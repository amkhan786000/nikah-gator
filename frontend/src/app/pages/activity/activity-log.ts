import { DatePipe } from '@angular/common';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Component, computed, inject, signal } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { AuthService, errorMessage } from '../../core/auth.service';
import { CardTableDirective } from '../../shared/card-table.directive';
import { ActivityPage, ActivityRow, FilterChoices, actionBadge, actionLabel, changesOf, recordLabel } from './activity.model';

interface MasjidOption {
  id: number;
  name: string;
}

/**
 * The activity log: who did what, newest first. The imam sees their own masjid (/imam/activity); the platform admin
 * sees every masjid and can narrow by masjid (/admin/activity). Read-only: nothing here can change or delete an entry.
 */
@Component({
  selector: 'app-activity-log',
  imports: [DatePipe, CardTableDirective],
  template: `
    <div class="page-title-box d-flex flex-wrap align-items-center justify-content-between">
      <h4 class="page-title">Activity log</h4>
      <div class="title-actions">
        <button class="btn btn-secondary btn-responsive" (click)="exportCsv()" [disabled]="exporting()">
          <i class="mdi mdi-download"></i> {{ exporting() ? 'Preparing…' : 'Export CSV' }}
        </button>
      </div>
    </div>
    <p class="text-muted">Every change, sign-in and opening of a private record, with who did it. Entries are permanent and cannot be edited or removed.</p>
    @if (error()) { <div class="alert alert-danger">{{ error() }}</div> }

    <div class="card mb-3"><div class="card-body">
      <div class="row">
        @if (admin) {
          <div class="col-12 col-md-4 form-group">
            <label>Masjid</label>
            <select class="form-control" [value]="masjidId()" (change)="masjidId.set($any($event.target).value); reloadChoices(); apply()">
              <option value="">All masjids</option>
              @for (m of masjids(); track m.id) { <option [value]="m.id">{{ m.name }}</option> }
            </select>
          </div>
        }
        <div class="col-6 col-md-2 form-group">
          <label>From</label>
          <input type="date" class="form-control" [value]="from()" (change)="from.set($any($event.target).value); apply()" />
        </div>
        <div class="col-6 col-md-2 form-group">
          <label>To</label>
          <input type="date" class="form-control" [value]="to()" (change)="to.set($any($event.target).value); apply()" />
        </div>
        <div class="col-12 col-md-3 form-group">
          <label>Person</label>
          <select class="form-control" [value]="userId()" (change)="userId.set($any($event.target).value); apply()">
            <option value="">Everyone</option>
            @for (u of choices().users; track u.id) { <option [value]="u.id">{{ u.name }}</option> }
          </select>
        </div>
        <div class="col-6 col-md-3 form-group">
          <label>Action</label>
          <select class="form-control" [value]="action()" (change)="action.set($any($event.target).value); apply()">
            <option value="">Any action</option>
            @for (a of choices().actions; track a) { <option [value]="a">{{ actionLabel(a) }}</option> }
          </select>
        </div>
        <div class="col-6 col-md-3 form-group">
          <label>Record type</label>
          <select class="form-control" [value]="entityType()" (change)="entityType.set($any($event.target).value); entityId.set(''); apply()">
            <option value="">Any record</option>
            @for (t of choices().entityTypes; track t) { <option [value]="t">{{ recordLabel(t) }}</option> }
          </select>
        </div>
      </div>
      @if (entityId()) {
        <span class="badge badge-info">History of {{ recordLabel(entityType()).toLowerCase() }} #{{ entityId() }}
          <a href="javascript:void(0)" class="text-white ml-1" (click)="entityId.set(''); apply()" aria-label="Show all records">×</a>
        </span>
      }
      @if (filtered()) { <button class="btn btn-sm btn-outline-secondary ml-2" (click)="clear()">Clear filters</button> }
    </div></div>

    <div class="card"><div class="card-body p-0">
      <div class="table-responsive">
        <table class="table table-hover mb-0">
          <thead><tr><th>When</th><th>Who</th><th>Action</th><th>Record</th><th>What happened</th><th></th></tr></thead>
          <tbody>
            @for (r of rows(); track r.id) {
              <tr>
                <td class="text-nowrap">{{ r.createdAt | date: 'MMM d, y, h:mm:ss a' }}</td>
                <td>
                  <strong>{{ r.userName || 'Not signed in' }}</strong>
                  @if (r.userRole) { <br /><small class="text-muted">{{ roleLabel(r.userRole) }}</small> }
                  @if (admin && r.masjidName) { <br /><small class="text-muted">{{ r.masjidName }}</small> }
                </td>
                <td><span class="badge" [class]="badge(r.action)">{{ actionLabel(r.action) }}</span></td>
                <td>@if (r.entityType) { {{ recordLabel(r.entityType) }}@if (r.entityId) { #{{ r.entityId }} } }</td>
                <td>{{ r.summary }}</td>
                <td class="text-right text-nowrap">
                  <button class="btn btn-sm btn-outline-primary" (click)="toggle(r.id)">{{ open() === r.id ? 'Hide' : 'Details' }}</button>
                </td>
              </tr>
              @if (open() === r.id) {
                <tr class="no-card-label">
                  <td colspan="6" class="bg-light">
                    @if (changes(r).length) {
                      <table class="table table-sm no-cards mb-2">
                        <thead><tr><th>Field</th><th>Before</th><th>After</th></tr></thead>
                        <tbody>
                          @for (c of changes(r); track c.field) {
                            <tr><td>{{ c.field }}</td><td>{{ c.old ?? '—' }}</td><td>{{ c.new ?? '—' }}</td></tr>
                          }
                        </tbody>
                      </table>
                    }
                    <small class="text-muted">
                      @if (r.ip) { From {{ r.ip }}. }
                      @if (r.userAgent) { {{ r.userAgent }} }
                      @if (!changes(r).length && !r.ip && !r.userAgent) { No further details. }
                    </small>
                  </td>
                </tr>
              }
            } @empty {
              <tr><td colspan="6" class="text-center text-muted py-4">{{ loaded() ? 'No activity found.' : 'Loading…' }}</td></tr>
            }
          </tbody>
        </table>
      </div>
    </div>
    <div class="card-footer d-flex flex-wrap align-items-center justify-content-between">
      <small class="text-muted">{{ total() }} {{ total() === 1 ? 'entry' : 'entries' }}</small>
      <div>
        <button class="btn btn-sm btn-outline-secondary" [disabled]="page() === 0" (click)="go(page() - 1)">Newer</button>
        <span class="mx-2">Page {{ page() + 1 }} of {{ pages() }}</span>
        <button class="btn btn-sm btn-outline-secondary" [disabled]="page() + 1 >= pages()" (click)="go(page() + 1)">Older</button>
      </div>
    </div></div>
  `,
})
export class ActivityLog {
  private http = inject(HttpClient);
  private route = inject(ActivatedRoute);
  readonly admin = inject(AuthService).me()?.role === 'PLATFORM_ADMIN';
  private base = this.admin ? '/api/admin/activity' : '/api/imam/activity';

  rows = signal<ActivityRow[]>([]);
  total = signal(0);
  page = signal(0);
  size = 25;
  loaded = signal(false);
  error = signal('');
  open = signal<number | null>(null);
  exporting = signal(false);
  choices = signal<FilterChoices>({ users: [], entityTypes: [], actions: [] });
  masjids = signal<MasjidOption[]>([]);

  masjidId = signal('');
  from = signal('');
  to = signal('');
  userId = signal('');
  action = signal('');
  entityType = signal('');
  entityId = signal('');

  pages = computed(() => Math.max(1, Math.ceil(this.total() / this.size)));
  filtered = computed(() => !!(this.masjidId() || this.from() || this.to() || this.userId() || this.action() || this.entityType() || this.entityId()));

  actionLabel = actionLabel;
  recordLabel = recordLabel;
  badge = actionBadge;

  constructor() {
    const q = this.route.snapshot.queryParamMap;
    this.entityType.set(q.get('entityType') ?? '');
    this.entityId.set(q.get('entityId') ?? '');
    if (this.admin) {
      this.http.get<MasjidOption[]>('/api/admin/masjids').subscribe({ next: (m) => this.masjids.set(m) });
    }
    this.reloadChoices();
    this.load();
  }

  reloadChoices() {
    const params = this.admin && this.masjidId() ? new HttpParams().set('masjidId', this.masjidId()) : undefined;
    this.http.get<FilterChoices>(`${this.base}/filters`, { params }).subscribe({ next: (c) => this.choices.set(c) });
  }

  private params(): HttpParams {
    let p = new HttpParams();
    const set = (k: string, v: string) => (p = v ? p.set(k, v) : p);
    if (this.admin) set('masjidId', this.masjidId());
    set('from', this.from());
    set('to', this.to());
    set('userId', this.userId());
    set('action', this.action());
    set('entityType', this.entityType());
    set('entityId', this.entityId());
    return p;
  }

  apply() {
    this.page.set(0);
    this.load();
  }

  clear() {
    for (const s of [this.masjidId, this.from, this.to, this.userId, this.action, this.entityType, this.entityId]) s.set('');
    this.reloadChoices();
    this.apply();
  }

  go(p: number) {
    this.page.set(p);
    this.load();
  }

  load() {
    this.open.set(null);
    const params = this.params().set('page', this.page()).set('size', this.size);
    this.http.get<ActivityPage>(this.base, { params }).subscribe({
      next: (r) => {
        this.rows.set(r.items);
        this.total.set(r.total);
        this.loaded.set(true);
        this.error.set('');
      },
      error: (e) => this.error.set(errorMessage(e)),
    });
  }

  toggle(id: number) {
    this.open.set(this.open() === id ? null : id);
  }

  changes = changesOf;

  roleLabel(role: string): string {
    return role === 'PLATFORM_ADMIN' ? 'Platform admin' : role === 'STAFF' ? 'Staff' : role === 'IMAM' ? 'Imam' : role;
  }

  /** The CSV carries the current filters; fetched with the sign-in header, then saved as a file. */
  exportCsv() {
    this.exporting.set(true);
    this.http.get(`${this.base}/export`, { params: this.params(), responseType: 'blob' }).subscribe({
      next: (blob) => {
        const a = document.createElement('a');
        a.href = URL.createObjectURL(blob);
        a.download = 'activity-log.csv';
        a.click();
        URL.revokeObjectURL(a.href);
        this.exporting.set(false);
      },
      error: (e) => {
        this.error.set(errorMessage(e));
        this.exporting.set(false);
      },
    });
  }
}
