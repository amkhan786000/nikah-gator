import { HttpClient } from '@angular/common/http';
import { Component, DestroyRef, ElementRef, computed, effect, inject, signal, viewChild } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { errorMessage } from '../core/auth.service';
import { CertificatePreview } from '../shared/certificate-preview';
import { CertificateContent, renderTemplate } from '../shared/certificate-render';
import { Nikah } from './nikah.model';

interface Placeholder { key: string; description: string; example: string; }

interface Design {
  title: string;
  bismillahEnabled: boolean;
  masjidDisplayName: string;
  addressText: string;
  phoneText: string;
  introText: string;
  bodyText: string;
  footerText: string;
  leftSignatureLabel: string;
  rightSignatureLabel: string;
  accentColor: string;
  paperSize: string;
}

interface TemplateView extends Omit<Design, 'masjidDisplayName' | 'addressText' | 'phoneText'> {
  designed: boolean;
  masjidDisplayName: string | null;
  addressText: string | null;
  phoneText: string | null;
  hasLogo: boolean;
  masjidName: string;
  masjidAddress: string;
  masjidPhone: string;
  placeholders: Placeholder[];
}

const PRESETS = ['#4f7a00', '#0b6b4f', '#1d4e89', '#7a2e8e', '#9c2b2b', '#b8860b', '#222222'];

@Component({
  selector: 'app-certificate-designer',
  imports: [FormsModule, CertificatePreview],
  styles: `
    .cd-grid { display: grid; grid-template-columns: minmax(0, 1fr) minmax(0, 1fr); gap: 20px; align-items: start; }
    .cd-sticky { position: sticky; top: 76px; }
    .cd-chips { display: flex; flex-wrap: wrap; gap: 6px; margin-bottom: 8px; }
    .cd-chip { border: 1px solid var(--line, #dfe3e8); background: #f6f8fa; border-radius: 14px; padding: 4px 10px; font-size: 12.5px; cursor: pointer; }
    .cd-chip:hover { background: #eaf2dc; }
    .cd-swatches { display: flex; flex-wrap: wrap; gap: 8px; align-items: center; }
    .cd-swatch { width: 34px; height: 34px; border-radius: 50%; border: 2px solid #fff; box-shadow: 0 0 0 1px #c5ccd3; cursor: pointer; padding: 0; }
    .cd-swatch.on { box-shadow: 0 0 0 3px #1f2933; }
    .cd-color { width: 44px; height: 38px; padding: 2px; }
    .cd-logo { max-height: 56px; max-width: 160px; border: 1px solid #dfe3e8; border-radius: 4px; padding: 3px; background: #fff; }
    .cd-toggle { display: none; }
    .cd-bar { position: sticky; bottom: 0; background: #fff; border-top: 1px solid #e5e9ee; padding: 10px 0; margin-top: 12px; display: flex; flex-wrap: wrap; gap: 8px; align-items: center; }
    @media (max-width: 991.98px) {
      .cd-grid { grid-template-columns: minmax(0, 1fr); }
      .cd-sticky { position: static; }
      .cd-toggle { display: flex; margin-bottom: 12px; }
      .cd-toggle .btn { flex: 1; }
      .cd-hide-sm { display: none; }
    }
  `,
  template: `
    <div class="page-title-box d-flex flex-wrap align-items-center justify-content-between">
      <h4 class="page-title">Certificate design</h4>
      @if (dirty()) { <span class="badge badge-warning">Unsaved changes</span> }
      @else if (view()) { <span class="badge" [class.badge-success]="view()!.designed" [class.badge-secondary]="!view()!.designed">{{ view()!.designed ? 'Saved' : 'Using default design' }}</span> }
    </div>
    @if (error()) { <div class="alert alert-danger">{{ error() }}</div> }
    @if (success()) { <div class="alert alert-success">{{ success() }}</div> }

    @if (view()) {
      <div class="cd-toggle btn-group" role="group" aria-label="Edit or preview">
        <button type="button" class="btn" [class.btn-primary]="tab() === 'edit'" [class.btn-secondary]="tab() !== 'edit'" (click)="tab.set('edit')"><i class="mdi mdi-pencil"></i> Edit</button>
        <button type="button" class="btn" [class.btn-primary]="tab() === 'preview'" [class.btn-secondary]="tab() !== 'preview'" (click)="tab.set('preview')"><i class="mdi mdi-eye"></i> Preview</button>
      </div>

      <div class="cd-grid">
        <div [class.cd-hide-sm]="tab() !== 'edit'">
          <div class="card"><div class="card-body">
            <span class="card-header-title">Wording</span>
            <div class="form-group">
              <label for="cd-title">Title</label>
              <input id="cd-title" class="form-control" maxlength="120" [ngModel]="f().title" (ngModelChange)="set('title', $event)" />
            </div>
            <div class="custom-control custom-checkbox mb-3">
              <input type="checkbox" class="custom-control-input" id="cd-bism" [ngModel]="f().bismillahEnabled" (ngModelChange)="set('bismillahEnabled', $event)" />
              <label class="custom-control-label" for="cd-bism">Show Bismillah at the top</label>
            </div>
            <div class="form-group">
              <label for="cd-intro">Introduction</label>
              <textarea id="cd-intro" class="form-control" rows="2" maxlength="500" [ngModel]="f().introText" (ngModelChange)="set('introText', $event)"></textarea>
            </div>
            <div class="form-group">
              <label for="cd-body">Certificate text</label>
              <div class="cd-chips" aria-label="Insert a field">
                @for (p of view()!.placeholders; track p.key) {
                  <button type="button" class="cd-chip" [title]="p.description" (click)="insert(p.key)">{{ '{{' + p.key + '}}' }}</button>
                }
              </div>
              <textarea #body id="cd-body" class="form-control" rows="7" maxlength="2000" [ngModel]="f().bodyText" (ngModelChange)="set('bodyText', $event)"></textarea>
              <small class="form-text text-muted">Tap a field to insert it where your cursor is. Empty fields are left out (for example "son of" when no father is entered).</small>
            </div>
            <div class="form-group">
              <label for="cd-footer">Footer note (optional)</label>
              <textarea id="cd-footer" class="form-control" rows="2" maxlength="500" [ngModel]="f().footerText" (ngModelChange)="set('footerText', $event)"></textarea>
            </div>
            <div class="form-row">
              <div class="form-group col-sm-6">
                <label for="cd-left">Left signature label</label>
                <input id="cd-left" class="form-control" maxlength="80" [ngModel]="f().leftSignatureLabel" (ngModelChange)="set('leftSignatureLabel', $event)" />
              </div>
              <div class="form-group col-sm-6">
                <label for="cd-right">Right signature label</label>
                <input id="cd-right" class="form-control" maxlength="80" [ngModel]="f().rightSignatureLabel" (ngModelChange)="set('rightSignatureLabel', $event)" />
              </div>
            </div>
          </div></div>

          <div class="card"><div class="card-body">
            <span class="card-header-title">Masjid details on the certificate</span>
            <div class="form-group">
              <label for="cd-name">Masjid name</label>
              <input id="cd-name" class="form-control" maxlength="160" [placeholder]="view()!.masjidName" [ngModel]="f().masjidDisplayName" (ngModelChange)="set('masjidDisplayName', $event)" />
            </div>
            <div class="form-group">
              <label for="cd-addr">Address</label>
              <input id="cd-addr" class="form-control" maxlength="300" [placeholder]="view()!.masjidAddress" [ngModel]="f().addressText" (ngModelChange)="set('addressText', $event)" />
            </div>
            <div class="form-group">
              <label for="cd-phone">Phone</label>
              <input id="cd-phone" class="form-control" maxlength="60" [placeholder]="view()!.masjidPhone" [ngModel]="f().phoneText" (ngModelChange)="set('phoneText', $event)" />
              <small class="form-text text-muted">Leave blank to use the details saved for your masjid.</small>
            </div>
          </div></div>

          <div class="card"><div class="card-body">
            <span class="card-header-title">Look</span>
            <div class="form-group">
              <label>Accent color</label>
              <div class="cd-swatches">
                @for (c of presets; track c) {
                  <button type="button" class="cd-swatch" [class.on]="f().accentColor.toLowerCase() === c" [style.background]="c" [attr.aria-label]="'Use color ' + c" (click)="set('accentColor', c)"></button>
                }
                <input type="color" class="form-control cd-color" aria-label="Custom color" [ngModel]="f().accentColor" (ngModelChange)="set('accentColor', $event)" />
              </div>
            </div>
            <div class="form-group">
              <label for="cd-paper">Paper size</label>
              <select id="cd-paper" class="form-control" [ngModel]="f().paperSize" (ngModelChange)="set('paperSize', $event)">
                <option value="LETTER">US Letter (8.5 x 11 in)</option>
                <option value="A4">A4</option>
              </select>
            </div>
            <div class="form-group mb-0">
              <label>Logo</label>
              <div class="d-flex flex-wrap align-items-center" style="gap:12px">
                @if (logoUrl()) { <img class="cd-logo" [src]="logoUrl()" alt="Current logo" /> }
                <label class="btn btn-secondary btn-sm mb-0" for="cd-logo-file"><i class="mdi mdi-upload"></i> {{ logoUrl() ? 'Replace' : 'Upload' }} logo</label>
                <input id="cd-logo-file" type="file" accept="image/png,image/jpeg" class="d-none" (change)="uploadLogo($event)" />
                @if (logoUrl()) { <button type="button" class="btn btn-secondary btn-sm" (click)="removeLogo()" [disabled]="busy()"><i class="mdi mdi-delete"></i> Remove</button> }
              </div>
              <small class="form-text text-muted">PNG or JPG, up to 2 MB. A logo is saved right away.</small>
            </div>
          </div></div>

          <div class="cd-bar">
            <button type="button" class="btn btn-primary btn-responsive" (click)="save()" [disabled]="busy() || !dirty()"><i class="mdi mdi-content-save"></i> Save design</button>
            <button type="button" class="btn btn-secondary btn-responsive" (click)="reset()" [disabled]="busy()"><i class="mdi mdi-restore"></i> Reset to default</button>
            @if (dirty()) { <span class="text-muted small">Unsaved changes</span> }
          </div>
        </div>

        <div [class.cd-hide-sm]="tab() !== 'preview'">
          <div class="cd-sticky">
            <div class="card"><div class="card-body">
              <span class="card-header-title">Live preview</span>
              <div class="form-group">
                <label for="cd-nikah">Preview with</label>
                <select id="cd-nikah" class="form-control" [ngModel]="nikahId()" (ngModelChange)="nikahId.set($event)">
                  <option [ngValue]="null">Sample names</option>
                  @for (n of nikahs(); track n.id) {
                    <option [ngValue]="n.id">{{ n.groomName }} &amp; {{ n.brideName }}</option>
                  }
                </select>
              </div>
              <app-certificate-preview [content]="content()" [logoUrl]="logoUrl()" [paper]="f().paperSize" />
            </div></div>
          </div>
        </div>
      </div>
    }
  `,
})
export class CertificateDesigner {
  private http = inject(HttpClient);
  private destroyRef = inject(DestroyRef);
  private bodyBox = viewChild<ElementRef<HTMLTextAreaElement>>('body');

  readonly presets = PRESETS;
  view = signal<TemplateView | null>(null);
  f = signal<Design>(this.empty());
  saved = signal('');
  error = signal('');
  success = signal('');
  busy = signal(false);
  tab = signal<'edit' | 'preview'>('edit');
  logoUrl = signal<string | null>(null);
  nikahs = signal<Nikah[]>([]);
  nikahId = signal<number | null>(null);
  serverContent = signal<CertificateContent | null>(null);

  dirty = computed(() => !!this.view() && JSON.stringify(this.f()) !== this.saved());

  private localContent = computed<CertificateContent>(() => {
    const v = this.view();
    const f = this.f();
    const values: Record<string, string> = {};
    for (const p of v?.placeholders ?? []) values[p.key] = p.example;
    values['masjidName'] = v?.masjidName ?? '';
    values['masjidAddress'] = v?.masjidAddress ?? '';
    const r = (t: string) => renderTemplate(t, values);
    return {
      title: r(f.title) || 'Nikah Certificate',
      bismillah: f.bismillahEnabled,
      masjidName: f.masjidDisplayName.trim() || v?.masjidName || '',
      addressText: r(f.addressText.trim() || v?.masjidAddress || ''),
      phoneText: r(f.phoneText.trim() || v?.masjidPhone || ''),
      intro: r(f.introText),
      body: r(f.bodyText),
      footer: r(f.footerText),
      leftLabel: r(f.leftSignatureLabel),
      rightLabel: r(f.rightSignatureLabel),
      accentColor: f.accentColor,
      hasLogo: !!this.logoUrl(),
    };
  });

  content = computed(() => (this.nikahId() !== null && this.serverContent()) || this.localContent());

  private logoObjectUrl: string | null = null;

  constructor() {
    this.destroyRef.onDestroy(() => this.revokeLogo());
    this.http.get<TemplateView>('/api/imam/certificate-template').subscribe({
      next: (v) => this.apply(v),
      error: (e) => this.error.set(errorMessage(e)),
    });
    this.http.get<Nikah[]>('/api/imam/nikahs').subscribe({ next: (r) => this.nikahs.set(r), error: () => {} });

    // With a real nikah selected, ask the server to render the draft (debounced); otherwise rendering is local.
    effect((onCleanup) => {
      const id = this.nikahId();
      const draft = this.f();
      if (id === null) {
        this.serverContent.set(null);
        return;
      }
      const t = setTimeout(() => {
        this.http.post<CertificateContent>('/api/imam/certificate-template/preview', { draft, nikahId: id }).subscribe({
          next: (c) => this.serverContent.set(c),
          error: (e) => this.error.set(errorMessage(e)),
        });
      }, 300);
      onCleanup(() => clearTimeout(t));
    });
  }

  set<K extends keyof Design>(key: K, value: Design[K]) {
    this.f.update((d) => ({ ...d, [key]: value }));
    this.success.set('');
  }

  insert(key: string) {
    const ta = this.bodyBox()?.nativeElement;
    const token = `{{${key}}}`;
    const text = this.f().bodyText;
    const start = ta?.selectionStart ?? text.length;
    const end = ta?.selectionEnd ?? text.length;
    this.set('bodyText', text.slice(0, start) + token + text.slice(end));
    const pos = start + token.length;
    setTimeout(() => {
      ta?.focus();
      ta?.setSelectionRange(pos, pos);
    });
  }

  save() {
    this.busy.set(true);
    this.error.set('');
    this.http.put<TemplateView>('/api/imam/certificate-template', this.f()).subscribe({
      next: (v) => {
        this.apply(v, false);
        this.success.set('Certificate design saved.');
        this.busy.set(false);
      },
      error: (e) => {
        this.error.set(errorMessage(e));
        this.busy.set(false);
      },
    });
  }

  reset() {
    if (!confirm('Reset the certificate to the default design? Your wording, colors and logo will be removed.')) return;
    this.busy.set(true);
    this.error.set('');
    this.http.post<TemplateView>('/api/imam/certificate-template/reset', {}).subscribe({
      next: (v) => {
        this.apply(v);
        this.success.set('Design reset to the default.');
        this.busy.set(false);
      },
      error: (e) => {
        this.error.set(errorMessage(e));
        this.busy.set(false);
      },
    });
  }

  uploadLogo(ev: Event) {
    const input = ev.target as HTMLInputElement;
    const file = input.files?.[0];
    input.value = '';
    if (!file) return;
    if (file.size > 2 * 1024 * 1024) {
      this.error.set('The logo must be 2 MB or smaller.');
      return;
    }
    const fd = new FormData();
    fd.append('file', file);
    this.busy.set(true);
    this.error.set('');
    this.http.post<TemplateView>('/api/imam/certificate-template/logo', fd).subscribe({
      next: (v) => {
        this.view.set(v);
        this.loadLogo(v.hasLogo);
        this.success.set('Logo saved.');
        this.busy.set(false);
      },
      error: (e) => {
        this.error.set(errorMessage(e));
        this.busy.set(false);
      },
    });
  }

  removeLogo() {
    this.busy.set(true);
    this.error.set('');
    this.http.delete<TemplateView>('/api/imam/certificate-template/logo').subscribe({
      next: (v) => {
        this.view.set(v);
        this.loadLogo(false);
        this.success.set('Logo removed.');
        this.busy.set(false);
      },
      error: (e) => {
        this.error.set(errorMessage(e));
        this.busy.set(false);
      },
    });
  }

  private apply(v: TemplateView, loadLogo = true) {
    this.view.set(v);
    const d: Design = {
      title: v.title,
      bismillahEnabled: v.bismillahEnabled,
      masjidDisplayName: v.masjidDisplayName ?? '',
      addressText: v.addressText ?? '',
      phoneText: v.phoneText ?? '',
      introText: v.introText,
      bodyText: v.bodyText,
      footerText: v.footerText,
      leftSignatureLabel: v.leftSignatureLabel,
      rightSignatureLabel: v.rightSignatureLabel,
      accentColor: v.accentColor,
      paperSize: v.paperSize,
    };
    this.f.set(d);
    this.saved.set(JSON.stringify(d));
    if (loadLogo) this.loadLogo(v.hasLogo);
  }

  /** The logo needs the bearer token, so it is fetched as a blob and shown through an object URL. */
  private loadLogo(has: boolean) {
    this.revokeLogo();
    if (!has) return;
    this.http.get('/api/imam/certificate-template/logo', { responseType: 'blob' }).subscribe({
      next: (b) => {
        this.logoObjectUrl = URL.createObjectURL(b);
        this.logoUrl.set(this.logoObjectUrl);
      },
      error: () => {},
    });
  }

  private revokeLogo() {
    if (this.logoObjectUrl) URL.revokeObjectURL(this.logoObjectUrl);
    this.logoObjectUrl = null;
    this.logoUrl.set(null);
  }

  private empty(): Design {
    return {
      title: '', bismillahEnabled: true, masjidDisplayName: '', addressText: '', phoneText: '', introText: '', bodyText: '',
      footerText: '', leftSignatureLabel: '', rightSignatureLabel: '', accentColor: '#4f7a00', paperSize: 'LETTER',
    };
  }
}
