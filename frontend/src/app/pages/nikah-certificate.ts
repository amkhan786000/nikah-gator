import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { DatePipe } from '@angular/common';
import { Component, effect, inject, input, signal, untracked } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { errorMessage } from '../core/auth.service';

interface Certificate {
  id: number;
  number: string;
  status: 'VALID' | 'VOID';
  nikahDate: string;
  issuedAt: string;
  voidReason: string | null;
  voidedAt: string | null;
  verifyUrl: string;
}

interface Content {
  title: string;
  bismillah: boolean;
  masjidName: string;
  address: string;
  phone: string | null;
  intro: string;
  body: string;
  footer: string | null;
  leftLabel: string;
  rightLabel: string;
  accentColor: string;
  logoUrl: string | null;
  number: string | null;
  watermark: boolean;
}

/**
 * The certificate of one nikah. Until the nikah is COMPLETED it is only a watermarked preview (no download, no
 * print); once completed it can be issued, then downloaded as PDF or printed.
 */
@Component({
  selector: 'app-nikah-certificate',
  imports: [FormsModule, DatePipe],
  template: `
    <div class="card no-print"><div class="card-body">
      <span class="card-header-title">Certificate</span>
      @if (error()) { <div class="alert alert-danger">{{ error() }}</div> }
      @if (loading()) {
        <p class="text-muted mb-0">Loading…</p>
      } @else if (status() !== 'COMPLETED') {
        <div class="alert alert-info mb-0">
          <i class="mdi mdi-information-outline"></i> Mark the nikah as completed to issue the certificate.
          The certificate below is a preview only and is not valid.
          @if (cert()?.status === 'VALID') { <br />Certificate {{ cert()!.number }} was issued earlier but is not valid while the nikah is not completed. }
        </div>
      } @else if (!cert() || cert()!.status === 'VOID') {
        @if (cert(); as c) {
          <div class="alert alert-warning">
            Certificate <strong>{{ c.number }}</strong> was voided{{ c.voidedAt ? ' on ' : '' }}{{ c.voidedAt | date: 'mediumDate' }}.
            Reason: {{ c.voidReason }}
          </div>
        }
        <p class="text-muted">{{ cert() ? 'You can issue a replacement certificate with a new number.' : 'The nikah is completed. Issue the certificate to give it a permanent number.' }}</p>
        <button class="btn btn-primary btn-responsive" (click)="issue()" [disabled]="busy()">
          <i class="mdi mdi-certificate"></i> {{ cert() ? 'Issue a new certificate' : 'Issue certificate' }}
        </button>
        @if (cert()) {
          <div class="mt-3"><button class="btn btn-secondary btn-sm" (click)="download()" [disabled]="busy()"><i class="mdi mdi-download"></i> Download voided copy</button></div>
        }
      } @else {
        <div class="detail-grid">
          <div><span>Certificate number</span><strong>{{ cert()!.number }}</strong></div>
          <div><span>Issued</span><strong>{{ cert()!.issuedAt | date: 'medium' }}</strong></div>
          <div><span>Verification link</span><strong style="word-break:break-all"><a [href]="cert()!.verifyUrl" target="_blank" rel="noopener">{{ cert()!.verifyUrl }}</a></strong></div>
        </div>
        <div class="d-flex flex-wrap mt-3" style="gap:.4rem">
          <button class="btn btn-primary btn-responsive" (click)="download()" [disabled]="busy()"><i class="mdi mdi-file-pdf"></i> Download PDF</button>
          <button class="btn btn-secondary btn-responsive" (click)="print()"><i class="mdi mdi-printer"></i> Print</button>
          <button class="btn btn-secondary btn-responsive" (click)="voiding.set(!voiding())"><i class="mdi mdi-cancel"></i> Void certificate</button>
        </div>
        @if (voiding()) {
          <form class="mt-3" (ngSubmit)="voidIt()">
            <div class="form-group">
              <label for="reason">Reason for voiding</label>
              <input class="form-control" id="reason" name="reason" [(ngModel)]="reason" maxlength="500" required placeholder="For example: misspelled name" />
              <small class="form-text text-muted">The number stays on record and shows as void when verified. You can then issue a new one.</small>
            </div>
            <button class="btn btn-secondary btn-responsive" [disabled]="busy() || reason.trim().length < 3">Void it</button>
          </form>
        }
      }
      @if (!loading() && content()) {
        <div class="mt-3">
          <button class="btn btn-outline-primary btn-responsive" (click)="showing.set(!showing())">
            <i class="mdi" [class]="showing() ? 'mdi-eye-off-outline' : 'mdi-eye-outline'"></i>
            {{ showing() ? 'Hide ' + noun() : 'View ' + noun() }}
          </button>
        </div>
      }
    </div></div>

    @if (showing() && content(); as c) {
      <div class="certificate cert-doc" [class.is-preview]="c.watermark" [style.border-color]="c.accentColor"
           (contextmenu)="c.watermark && $event.preventDefault()">
        @if (c.watermark) { <div class="cert-watermark" aria-hidden="true">PREVIEW - NOT VALID</div> }
        @if (c.logoUrl) { <img [src]="c.logoUrl" alt="" style="max-height:70px;max-width:120px;margin-bottom:6px" /> }
        @if (c.bismillah) { <p class="bismillah" [style.color]="c.accentColor" dir="rtl">بسم الله الرحمن الرحيم</p> }
        <h1 [style.color]="c.accentColor">{{ c.title }}</h1>
        <p class="center"><strong>{{ c.masjidName }}</strong><br /><small>{{ c.address }}</small>@if (c.phone) { <br /><small>{{ c.phone }}</small> }</p>
        @if (c.intro) { <p>{{ c.intro }}</p> }
        @for (line of lines(c.body); track $index) { <p class="names">{{ line }}</p> }
        @if (c.footer) { <p><small>{{ c.footer }}</small></p> }
        <div class="signatures">
          <div><span class="line"></span>{{ c.leftLabel }}</div>
          <div><span class="line"></span>{{ c.rightLabel }}</div>
        </div>
        <p><small>@if (c.number) { Certificate no. {{ c.number }} } @else { Certificate number is assigned when issued }</small></p>
      </div>
    }
  `,
})
export class NikahCertificate {
  private http = inject(HttpClient);

  nikahId = input.required<number>();
  status = input('');

  cert = signal<Certificate | null>(null);
  content = signal<Content | null>(null);
  loading = signal(true);
  busy = signal(false);
  /** The certificate itself is shown only when the imam asks for it. */
  showing = signal(false);
  voiding = signal(false);
  error = signal('');
  reason = '';

  private get url() {
    return `/api/imam/nikahs/${this.nikahId()}/certificate`;
  }

  constructor() {
    // reload whenever the nikah status changes (for example after "Mark completed")
    effect(() => {
      this.status();
      untracked(() => this.load());
    });
  }

  /** "Preview" until a valid certificate has been issued for a completed nikah. */
  noun(): string {
    return this.status() === 'COMPLETED' && this.cert()?.status === 'VALID' ? 'certificate' : 'preview';
  }

  lines(text: string): string[] {
    return (text ?? '').split(/\r?\n/);
  }

  print() {
    // only the certificate prints, so it has to be on the page first
    this.showing.set(true);
    setTimeout(() => window.print(), 100);
  }

  private load() {
    this.http.get<Certificate>(this.url).subscribe({
      next: (c) => {
        this.cert.set(c);
        this.loadContent();
      },
      error: (e: HttpErrorResponse) => {
        this.cert.set(null);
        if (e.status !== 404) this.error.set(errorMessage(e));
        this.loadContent();
      },
    });
  }

  private loadContent() {
    this.http.get<Content>(`${this.url}/content`).subscribe({
      next: (c) => {
        this.content.set(c);
        this.loading.set(false);
      },
      error: (e) => {
        this.loading.set(false);
        this.error.set(errorMessage(e));
      },
    });
  }

  private set(c: Certificate | null) {
    this.cert.set(c);
    this.loadContent();
  }

  issue() {
    this.run(this.http.post<Certificate>(this.url, {}), (c) => this.set(c));
  }

  voidIt() {
    this.run(this.http.post<Certificate>(`${this.url}/void`, { reason: this.reason.trim() }), (c) => {
      this.voiding.set(false);
      this.reason = '';
      this.set(c);
    });
  }

  download() {
    const c = this.cert();
    if (!c) return;
    this.busy.set(true);
    this.error.set('');
    this.http.get(`${this.url}/pdf`, { responseType: 'blob' }).subscribe({
      next: (blob) => {
        const href = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = href;
        a.download = `${c.number}.pdf`;
        document.body.appendChild(a);
        a.click();
        a.remove();
        setTimeout(() => URL.revokeObjectURL(href), 10000);
        this.busy.set(false);
      },
      error: (e) => {
        this.busy.set(false);
        this.error.set(errorMessage(e));
      },
    });
  }

  private run<T>(req: import('rxjs').Observable<T>, ok: (r: T) => void) {
    this.busy.set(true);
    this.error.set('');
    req.subscribe({
      next: (r) => {
        this.busy.set(false);
        ok(r);
      },
      error: (e) => {
        this.busy.set(false);
        this.error.set(errorMessage(e));
      },
    });
  }
}
