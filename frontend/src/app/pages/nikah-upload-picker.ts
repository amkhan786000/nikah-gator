import { HttpClient } from '@angular/common/http';
import { DatePipe } from '@angular/common';
import { Component, inject, input, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { errorMessage } from '../core/auth.service';

const MAX_BYTES = 25 * 1024 * 1024;

export const STANDARD_ITEMS = [
  { code: 'MARRIAGE_LICENSE', label: 'Marriage license' },
  { code: 'GROOM_ID', label: 'Groom ID' },
  { code: 'BRIDE_ID', label: 'Bride ID' },
  { code: 'WALI_CONSENT', label: 'Wali consent / presence' },
  { code: 'WITNESSES', label: 'Two witnesses' },
  { code: 'PREMARITAL_COUNSELING', label: 'Pre-marital counseling' },
  { code: 'MAHR_AGREED', label: 'Mahr agreed' },
];

interface Picked {
  file: File;
  code: string;
  status: 'pending' | 'uploading' | 'done' | 'failed';
  error: string;
}

interface Existing {
  id: number;
  fileName: string;
  sizeBytes: number;
  createdAt: string;
}

/**
 * Optional "Documents" section of the nikah form. Files are kept in memory; the form calls uploadAll(nikahId)
 * after the nikah has been saved. For an existing nikah (nikahId set) it also lists the documents already uploaded.
 */
@Component({
  selector: 'app-nikah-upload-picker',
  imports: [FormsModule, DatePipe],
  template: `
    <span class="card-header-title mt-2">Documents (optional)</span>
    <small class="text-muted d-block mb-2">PDF, JPG or PNG, up to 25 MB each. You can take a photo with your phone.</small>
    @if (error()) { <div class="alert alert-danger">{{ error() }}</div> }

    @for (e of existing(); track e.id) {
      <div class="py-1" style="word-break:break-word"><i class="mdi mdi-paperclip"></i> {{ e.fileName }}
        <small class="text-muted">· {{ size(e.sizeBytes) }} · {{ e.createdAt | date: 'MMM d, y' }}</small></div>
    }

    @for (p of picked(); track p) {
      <div class="py-2 pick-row">
        <div style="word-break:break-word">
          <i class="mdi mdi-file-outline"></i> <strong>{{ p.file.name }}</strong> <small class="text-muted">{{ size(p.file.size) }}</small>
          @switch (p.status) {
            @case ('uploading') { <span class="badge badge-info ml-1">Uploading…</span> }
            @case ('done') { <span class="badge badge-success ml-1">Uploaded</span> }
            @case ('failed') { <span class="badge badge-danger ml-1">Failed</span> }
          }
        </div>
        @if (p.status === 'pending') {
          <div class="d-flex mt-1" style="gap:.4rem">
            <select class="form-control form-control-sm flex-fill" [attr.aria-label]="'Checklist item for ' + p.file.name"
                    [ngModel]="p.code" (ngModelChange)="p.code = $event" [ngModelOptions]="{ standalone: true }" style="min-width:0">
              <option value="">Not linked to a checklist item</option>
              @for (i of items; track i.code) { <option [value]="i.code">{{ i.label }}</option> }
            </select>
            <button type="button" class="btn btn-sm btn-outline-danger" (click)="remove(p)" aria-label="Remove file"><i class="mdi mdi-close"></i></button>
          </div>
        }
      </div>
    }

    <label class="btn btn-secondary btn-responsive mb-2">
      <i class="mdi mdi-upload"></i> Add documents
      <input type="file" multiple hidden accept=".pdf,.jpg,.jpeg,.png,application/pdf,image/jpeg,image/png,image/*" (change)="pick($event)" />
    </label>
  `,
  styles: `.pick-row + .pick-row, .pick-row { border-top: 1px solid rgba(128,128,128,.2); }`,
})
export class NikahUploadPicker implements OnInit {
  private http = inject(HttpClient);

  /** Set when editing an existing nikah: lists its documents. */
  nikahId = input<string | null>(null);

  items = STANDARD_ITEMS;
  picked = signal<Picked[]>([]);
  existing = signal<Existing[]>([]);
  error = signal('');

  ngOnInit() {
    const id = this.nikahId();
    if (id) {
      this.http.get<Existing[]>(`/api/imam/nikahs/${id}/documents`).subscribe({ next: (r) => this.existing.set(r) });
    }
  }

  size(b: number) {
    return b >= 1024 * 1024 ? (b / 1024 / 1024).toFixed(1) + ' MB' : Math.max(1, Math.round(b / 1024)) + ' KB';
  }

  pick(ev: Event) {
    const input = ev.target as HTMLInputElement;
    const files = Array.from(input.files ?? []);
    input.value = '';
    this.error.set('');
    const bad: string[] = [];
    const ok: Picked[] = [];
    for (const file of files) {
      if (!/\.(pdf|jpe?g|png)$/i.test(file.name)) bad.push(`${file.name}: only PDF, JPG and PNG are allowed`);
      else if (file.size > MAX_BYTES) bad.push(`${file.name}: larger than 25 MB`);
      else if (file.size === 0) bad.push(`${file.name}: empty file`);
      else ok.push({ file, code: '', status: 'pending', error: '' });
    }
    if (bad.length) this.error.set(bad.join('. ') + '.');
    this.picked.update((l) => [...l, ...ok]);
  }

  remove(p: Picked) {
    this.picked.update((l) => l.filter((x) => x !== p));
  }

  hasFiles() {
    return this.picked().some((p) => p.status === 'pending' || p.status === 'failed');
  }

  /** Uploads the picked files one by one; resolves with the names of the files that failed. */
  async uploadAll(nikahId: number): Promise<string[]> {
    const failed: string[] = [];
    for (const p of this.picked()) {
      if (p.status === 'done') continue;
      this.setStatus(p, 'uploading');
      const fd = new FormData();
      fd.append('file', p.file);
      if (p.code) fd.append('checklistCode', p.code);
      try {
        await new Promise<void>((resolve, reject) =>
          this.http.post(`/api/imam/nikahs/${nikahId}/documents`, fd).subscribe({ next: () => resolve(), error: reject }),
        );
        this.setStatus(p, 'done');
      } catch (e) {
        p.error = errorMessage(e as never);
        failed.push(`${p.file.name} (${p.error})`);
        this.setStatus(p, 'failed');
      }
    }
    return failed;
  }

  private setStatus(p: Picked, s: Picked['status']) {
    p.status = s;
    this.picked.update((l) => [...l]);
  }
}
