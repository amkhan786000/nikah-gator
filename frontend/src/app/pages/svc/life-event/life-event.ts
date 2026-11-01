import { DatePipe } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { Component, computed, inject, input, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { errorMessage } from '../../../core/auth.service';
import { CardTableDirective } from '../../../shared/card-table.directive';
import { FieldDef, LifeEvent, LifeEventConfig } from './life-event.model';
import { SendEmail } from '../../../shared/send-email';

type Bag = Record<string, string | null>;

/** List, create/edit, void and PDF (download/print) for one imam life-event service; the page config says which fields. */
@Component({
  selector: 'app-life-event',
  imports: [CardTableDirective, DatePipe, FormsModule, SendEmail],
  template: `
    <div class="page-title-box d-flex flex-wrap align-items-center justify-content-between">
      <h4 class="page-title"><i class="mdi {{ cfg().icon }}"></i> {{ cfg().title }}</h4>
      @if (!editing()) {
        <button class="btn btn-primary btn-responsive needs-manage" (click)="add()"><i class="mdi mdi-plus"></i> New {{ cfg().singular }}</button>
      }
    </div>
    @if (error()) { <div class="alert alert-danger">{{ error() }}</div> }

    @if (editing(); as e) {
      <form class="card" (ngSubmit)="save()"><div class="card-body">
        <span class="card-header-title">{{ id() ? 'Edit ' + cfg().singular : 'New ' + cfg().singular }}</span>
        @if (number()) { <p class="text-muted mb-2">Reference {{ number() }}</p> }
        <div class="row">
          @for (f of cfg().fields; track f.key) {
            <div class="form-group" [class]="'col-md-' + (f.col ?? 6)">
              <label [attr.for]="'f-' + f.key">{{ f.label }}</label>
              @if (f.type === 'textarea') {
                <textarea class="form-control" rows="3" [id]="'f-' + f.key" [name]="f.key" [ngModel]="val(f)" (ngModelChange)="set(f, $event)"></textarea>
              } @else {
                <input class="form-control" [type]="f.type" [id]="'f-' + f.key" [name]="f.key" [ngModel]="val(f)" (ngModelChange)="set(f, $event)"
                       [required]="!!f.required" [attr.placeholder]="f.placeholder ?? null" />
              }
            </div>
          }
        </div>
        <div class="d-flex flex-wrap" style="gap:.5rem">
          <button class="btn btn-primary btn-responsive" [disabled]="busy()">{{ busy() ? 'Saving…' : 'Save' }}</button>
          <button type="button" class="btn btn-secondary btn-responsive" (click)="cancel()">Cancel</button>
        </div>
      </div></form>
    } @else {
      @if (cfg().sensitive) {
        <div class="alert alert-info"><i class="mdi mdi-lock-outline"></i> These records are private to your masjid. They are never listed publicly and cannot be looked up by number.</div>
      }
      <div class="card"><div class="card-body">
        <div class="input-icon mb-3" style="max-width:340px">
          <i class="mdi mdi-magnify"></i>
          <input class="form-control" placeholder="Search by name…" [ngModel]="q()" (ngModelChange)="q.set($event)" name="q" />
        </div>
        <div class="table-responsive">
          <table class="table table-hover mb-0">
            <thead><tr><th>{{ cfg().nameLabel }}</th><th>{{ cfg().dateLabel }}</th><th>Reference</th><th>Status</th><th></th></tr></thead>
            <tbody>
              @for (r of shown(); track r.id) {
                <tr>
                  <td>{{ r.personName }}</td>
                  <td class="text-nowrap">{{ dateOf(r) | date: 'MMM d, y' }}</td>
                  <td class="text-nowrap">{{ r.number }}</td>
                  <td><span class="badge" [class.badge-success]="r.status === 'VALID'" [class.badge-secondary]="r.status === 'VOID'">{{ r.status === 'VOID' ? 'Void' : 'Valid' }}</span></td>
                  <td class="text-right">
                    <div class="d-flex flex-wrap justify-content-end" style="gap:.25rem">
                      <button class="btn btn-sm btn-outline-primary" (click)="download(r)" [disabled]="busy()"><i class="mdi mdi-download"></i> PDF</button>
                      <button class="btn btn-sm btn-outline-secondary" (click)="print(r)" [disabled]="busy()"><i class="mdi mdi-printer"></i> Print</button>
                      @if (r.status === 'VALID') {
                        <button class="btn btn-sm btn-outline-secondary needs-manage" (click)="emailing.set(r)"><i class="mdi mdi-email-send-outline"></i> E-mail</button>
                      }
                      @if (r.status === 'VALID') {
                        <button class="btn btn-sm btn-outline-primary" (click)="edit(r)">Edit</button>
                        <button class="btn btn-sm btn-outline-danger" (click)="voidIt(r)" [disabled]="busy()">Void</button>
                      }
                    </div>
                  </td>
                </tr>
              } @empty {
                <tr><td colspan="5" class="text-center text-muted py-4">{{ loaded() ? 'Nothing recorded yet.' : 'Loading…' }}</td></tr>
              }
            </tbody>
          </table>
        </div>
      </div></div>
      @if (emailing(); as r) {
        <div class="card"><div class="card-body">
          <app-send-email [inline]="true" [sensitive]="!!cfg().sensitive" [url]="base + '/' + r.id + '/email'"
                          [noun]="r.number || 'this record'" (closed)="emailing.set(null)" />
        </div></div>
      }
    }
  `,
})
export class LifeEventPage {
  private http = inject(HttpClient);

  cfg = input.required<LifeEventConfig>();
  rows = signal<LifeEvent[]>([]);
  q = signal('');
  loaded = signal(false);
  busy = signal(false);
  error = signal('');
  /** The form values while adding or editing, else null. */
  editing = signal<Bag | null>(null);
  id = signal<number | null>(null);
  number = signal('');

  shown = computed(() => {
    const q = this.q().trim().toLowerCase();
    return q ? this.rows().filter((r) => r.personName.toLowerCase().includes(q)) : this.rows();
  });

  /** The record being e-mailed, else null. */
  emailing = signal<LifeEvent | null>(null);

  get base() {
    return `/api/imam/svc/${this.cfg().slug}`;
  }

  constructor() {
    queueMicrotask(() => this.reload());
  }

  reload() {
    this.http.get<LifeEvent[]>(this.base).subscribe({
      next: (r) => {
        this.rows.set(r);
        this.loaded.set(true);
      },
      error: (e) => {
        this.loaded.set(true);
        this.error.set(errorMessage(e));
      },
    });
  }

  dateOf(r: LifeEvent) {
    return (r[this.cfg().dateKey] as string | null) || r.eventDate;
  }

  val(f: FieldDef) {
    return this.editing()?.[f.key] ?? '';
  }

  set(f: FieldDef, v: string) {
    const e = this.editing();
    if (e) e[f.key] = v;
  }

  add() {
    this.error.set('');
    this.id.set(null);
    this.number.set('');
    this.editing.set(Object.fromEntries(this.cfg().fields.map((f) => [f.key, ''])));
  }

  edit(r: LifeEvent) {
    this.error.set('');
    this.id.set(r.id ?? null);
    this.number.set(r.number ?? '');
    this.editing.set(Object.fromEntries(this.cfg().fields.map((f) => [f.key, ((r as unknown as Bag)[f.key] ?? '') as string])));
  }

  cancel() {
    this.editing.set(null);
    this.error.set('');
  }

  save() {
    const e = this.editing();
    if (!e) return;
    const body: Bag = {};
    for (const f of this.cfg().fields) {
      const v = (e[f.key] ?? '').toString().trim();
      body[f.key] = v === '' ? null : v;
    }
    this.busy.set(true);
    this.error.set('');
    const id = this.id();
    const call = id ? this.http.put<LifeEvent>(`${this.base}/${id}`, body) : this.http.post<LifeEvent>(this.base, body);
    call.subscribe({
      next: () => {
        this.busy.set(false);
        this.editing.set(null);
        this.reload();
      },
      error: (err) => {
        this.busy.set(false);
        this.error.set(errorMessage(err));
      },
    });
  }

  voidIt(r: LifeEvent) {
    const reason = prompt(`Void ${r.number}? It will be marked void and can no longer be edited. Reason:`);
    if (!reason?.trim()) return;
    this.busy.set(true);
    this.error.set('');
    this.http.post<LifeEvent>(`${this.base}/${r.id}/void`, { reason: reason.trim() }).subscribe({
      next: () => {
        this.busy.set(false);
        this.reload();
      },
      error: (err) => {
        this.busy.set(false);
        this.error.set(errorMessage(err));
      },
    });
  }

  download(r: LifeEvent) {
    this.pdf(r, (blob) => {
      const href = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = href;
      a.download = `${r.number}.pdf`;
      document.body.appendChild(a);
      a.click();
      a.remove();
      setTimeout(() => URL.revokeObjectURL(href), 10000);
    });
  }

  print(r: LifeEvent) {
    // open the window synchronously (popup blockers), then point it at the PDF
    const w = window.open('', '_blank');
    this.pdf(
      r,
      (blob) => {
        const href = URL.createObjectURL(blob);
        if (w) {
          w.location.href = href;
          w.addEventListener('load', () => w.print());
        } else {
          window.open(href, '_blank');
        }
        setTimeout(() => URL.revokeObjectURL(href), 60000);
      },
      () => w?.close(),
    );
  }

  private pdf(r: LifeEvent, ok: (b: Blob) => void, fail?: () => void) {
    this.busy.set(true);
    this.error.set('');
    this.http.get(`${this.base}/${r.id}/pdf`, { responseType: 'blob' }).subscribe({
      next: (blob) => {
        this.busy.set(false);
        ok(blob);
      },
      error: (e) => {
        this.busy.set(false);
        fail?.();
        this.error.set(errorMessage(e));
      },
    });
  }
}
