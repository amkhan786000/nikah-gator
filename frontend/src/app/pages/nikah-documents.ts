import { HttpClient } from '@angular/common/http';
import { DatePipe } from '@angular/common';
import { Component, computed, inject, input, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { errorMessage } from '../core/auth.service';

interface Item {
  id: number;
  code: string;
  label: string;
  required: boolean;
  done: boolean;
  doneAt: string | null;
  custom: boolean;
}

interface Doc {
  id: number;
  fileName: string;
  contentType: string;
  sizeBytes: number;
  checklistId: number | null;
  createdAt: string;
}

const MAX_BYTES = 25 * 1024 * 1024;

/** "Documents and checklist" card of a nikah: the imam's checklist plus uploaded PDF/JPG/PNG documents. */
@Component({
  selector: 'app-nikah-documents',
  imports: [FormsModule, DatePipe],
  template: `
    <div class="card no-print"><div class="card-body">
      <div class="d-flex flex-wrap align-items-center justify-content-between mb-2">
        <span class="card-header-title mb-0">Documents and checklist</span>
        <span class="badge" [class.badge-success]="progress().done === progress().total && progress().total > 0" [class.badge-info]="progress().done < progress().total">
          {{ progress().done }}/{{ progress().total }} done
        </span>
      </div>
      @if (error()) { <div class="alert alert-danger">{{ error() }}</div> }
      @if (failedUploads().length) {
        <div class="alert alert-warning">
          These files could not be uploaded. Please attach them again below:
          <ul class="mb-0 pl-3">@for (f of failedUploads(); track f) { <li style="word-break:break-word">{{ f }}</li> }</ul>
        </div>
      }

      @for (i of items(); track i.id) {
        <div class="chk-item py-2" [class.text-muted]="!i.required">
          <div class="d-flex align-items-start" style="gap:.6rem">
            <input type="checkbox" class="mt-1" style="width:1.3rem;height:1.3rem;flex:none" [checked]="i.done"
                   [attr.aria-label]="'Done: ' + i.label" (change)="patch(i, { done: !i.done })" />
            <div class="flex-fill" style="min-width:0">
              <div [class.text-success]="i.done" style="word-break:break-word">
                <strong [style.text-decoration]="i.required ? null : 'line-through'">{{ i.label }}</strong>
                @if (!i.required) { <span class="badge badge-secondary ml-1">Not required</span> }
              </div>
              @if (i.done && i.doneAt) { <small class="text-muted">Done {{ i.doneAt | date: 'MMM d, y, h:mm a' }}</small> }
              @for (d of docsOf(i.id); track d.id) {
                <div><small><i class="mdi mdi-paperclip"></i>
                  <a href="#" (click)="download(d); $event.preventDefault()">{{ d.fileName }}</a></small></div>
              }
            </div>
          </div>
          <div class="d-flex flex-wrap mt-1" style="gap:.3rem;padding-left:1.9rem">
            <label class="btn btn-sm btn-outline-secondary mb-0" [class.disabled]="busy()">
              <i class="mdi mdi-upload"></i> Attach
              <input type="file" accept=".pdf,.jpg,.jpeg,.png,application/pdf,image/jpeg,image/png" hidden [disabled]="busy()"
                     (change)="pick($event, i.id)" />
            </label>
            <button class="btn btn-sm btn-outline-secondary" (click)="patch(i, { required: !i.required })">
              {{ i.required ? 'Not required' : 'Make required' }}
            </button>
            @if (i.custom) {
              <button class="btn btn-sm btn-outline-danger" (click)="removeItem(i)"><i class="mdi mdi-delete"></i> Delete</button>
            }
          </div>
        </div>
      }

      <form class="form-inline mt-2" style="gap:.4rem" (ngSubmit)="addItem()">
        <input class="form-control flex-fill" name="newLabel" maxlength="200" placeholder="Add your own item…"
               [ngModel]="newLabel()" (ngModelChange)="newLabel.set($event)" />
        <button class="btn btn-secondary" type="submit" [disabled]="!newLabel().trim()"><i class="mdi mdi-plus"></i> Add</button>
      </form>

      <hr />
      <div class="d-flex flex-wrap align-items-center justify-content-between mb-2">
        <strong>All documents</strong>
        <label class="btn btn-primary btn-sm mb-0" [class.disabled]="busy()">
          <i class="mdi mdi-upload"></i> {{ busy() ? 'Uploading…' : 'Upload document' }}
          <input type="file" accept=".pdf,.jpg,.jpeg,.png,application/pdf,image/jpeg,image/png" hidden [disabled]="busy()"
                 (change)="pick($event, null)" />
        </label>
      </div>
      <small class="text-muted d-block mb-2">PDF, JPG or PNG, up to 25 MB. Attaching a file to a checklist item marks it done.</small>
      @for (d of docs(); track d.id) {
        <div class="d-flex align-items-center py-1" style="gap:.5rem">
          <i class="mdi" [class.mdi-file-pdf]="d.contentType === 'application/pdf'" [class.mdi-file-image]="d.contentType !== 'application/pdf'"></i>
          <div class="flex-fill" style="min-width:0;word-break:break-word">
            <a href="#" (click)="download(d); $event.preventDefault()">{{ d.fileName }}</a><br />
            <small class="text-muted">{{ size(d.sizeBytes) }} · {{ d.createdAt | date: 'MMM d, y' }}@if (labelOf(d.checklistId); as l) { · {{ l }} }</small>
          </div>
          <button class="btn btn-sm btn-outline-secondary" title="Download" (click)="download(d)"><i class="mdi mdi-download"></i></button>
          <button class="btn btn-sm btn-outline-danger" title="Delete" (click)="removeDoc(d)"><i class="mdi mdi-delete"></i></button>
        </div>
      } @empty {
        <p class="text-muted mb-0">No documents uploaded yet.</p>
      }
    </div></div>
  `,
  styles: `.chk-item + .chk-item { border-top: 1px solid rgba(128,128,128,.2); }`,
})
export class NikahDocuments implements OnInit {
  private http = inject(HttpClient);

  nikahId = input.required<number>();

  items = signal<Item[]>([]);
  docs = signal<Doc[]>([]);
  newLabel = signal('');
  error = signal('');
  busy = signal(false);
  /** Files that failed to upload right after the nikah was registered (passed along in the router state). */
  failedUploads = signal<string[]>(history.state?.failedUploads ?? []);

  progress = computed(() => {
    const req = this.items().filter((i) => i.required);
    return { done: req.filter((i) => i.done).length, total: req.length };
  });

  private base = computed(() => `/api/imam/nikahs/${this.nikahId()}`);

  ngOnInit() {
    this.http.get<Item[]>(`${this.base()}/checklist`).subscribe({
      next: (r) => this.items.set(r),
      error: (e) => this.error.set(errorMessage(e)),
    });
    this.loadDocs();
  }

  private loadDocs() {
    this.http.get<Doc[]>(`${this.base()}/documents`).subscribe({
      next: (r) => this.docs.set(r),
      error: (e) => this.error.set(errorMessage(e)),
    });
  }

  docsOf(itemId: number) {
    return this.docs().filter((d) => d.checklistId === itemId);
  }

  labelOf(itemId: number | null) {
    return itemId == null ? '' : (this.items().find((i) => i.id === itemId)?.label ?? '');
  }

  size(b: number) {
    return b >= 1024 * 1024 ? (b / 1024 / 1024).toFixed(1) + ' MB' : Math.max(1, Math.round(b / 1024)) + ' KB';
  }

  patch(i: Item, body: { done?: boolean; required?: boolean }) {
    this.error.set('');
    this.http.put<Item>(`${this.base()}/checklist/${i.id}`, body).subscribe({
      next: (r) => this.items.update((l) => l.map((x) => (x.id === r.id ? r : x))),
      error: (e) => this.error.set(errorMessage(e)),
    });
  }

  addItem() {
    const label = this.newLabel().trim();
    if (!label) return;
    this.error.set('');
    this.http.post<Item>(`${this.base()}/checklist`, { label }).subscribe({
      next: (r) => {
        this.items.update((l) => [...l, r]);
        this.newLabel.set('');
      },
      error: (e) => this.error.set(errorMessage(e)),
    });
  }

  removeItem(i: Item) {
    if (!confirm(`Delete "${i.label}"? Its documents are kept.`)) return;
    this.http.delete(`${this.base()}/checklist/${i.id}`).subscribe({
      next: () => {
        this.items.update((l) => l.filter((x) => x.id !== i.id));
        this.loadDocs();
      },
      error: (e) => this.error.set(errorMessage(e)),
    });
  }

  pick(ev: Event, itemId: number | null) {
    const input = ev.target as HTMLInputElement;
    const file = input.files?.[0];
    input.value = '';
    if (!file) return;
    this.error.set('');
    if (file.size > MAX_BYTES) return this.error.set('The file is larger than 25 MB.');
    if (!/\.(pdf|jpe?g|png)$/i.test(file.name)) return this.error.set('Only PDF, JPG and PNG files are allowed.');
    const fd = new FormData();
    fd.append('file', file);
    if (itemId != null) fd.append('checklistId', String(itemId));
    this.busy.set(true);
    this.http.post<Doc>(`${this.base()}/documents`, fd).subscribe({
      next: (d) => {
        this.busy.set(false);
        this.docs.update((l) => [d, ...l]);
        if (itemId != null) {
          this.http.get<Item[]>(`${this.base()}/checklist`).subscribe((r) => this.items.set(r));
        }
      },
      error: (e) => {
        this.busy.set(false);
        this.error.set(errorMessage(e));
      },
    });
  }

  download(d: Doc) {
    this.http.get(`${this.base()}/documents/${d.id}/download`, { responseType: 'blob' }).subscribe({
      next: (blob) => {
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = d.fileName;
        a.click();
        setTimeout(() => URL.revokeObjectURL(url), 10000);
      },
      error: (e) => this.error.set(errorMessage(e)),
    });
  }

  removeDoc(d: Doc) {
    if (!confirm(`Delete "${d.fileName}"?`)) return;
    this.http.delete(`${this.base()}/documents/${d.id}`).subscribe({
      next: () => this.docs.update((l) => l.filter((x) => x.id !== d.id)),
      error: (e) => this.error.set(errorMessage(e)),
    });
  }
}
