import { Component, ViewEncapsulation, computed, input } from '@angular/core';
import { CertificateContent } from './certificate-render';

/**
 * A rendered nikah certificate. All text is shown with interpolation only (never innerHTML).
 * Reusable: pass the resolved content, an optional watermark ("DRAFT", "VOID"...) and a logo object URL.
 */
@Component({
  selector: 'app-certificate-preview',
  encapsulation: ViewEncapsulation.None,
  template: `
    <div class="cert-sheet" [class.cert-a4]="paper() === 'A4'" [style.--cert-accent]="accent()">
      <div class="cert-frame">
        @if (watermark()) { <div class="cert-watermark" aria-hidden="true">{{ watermark() }}</div> }
        @if (logoUrl()) { <img class="cert-logo" [src]="logoUrl()" alt="Masjid logo" /> }
        @if (content().bismillah) { <p class="cert-bismillah" dir="rtl" lang="ar">بسم الله الرحمن الرحيم</p> }
        <h1 class="cert-title">{{ content().title }}</h1>
        @if (content().masjidName || content().addressText || content().phoneText) {
          <p class="cert-masjid">
            @if (content().masjidName) { <strong>{{ content().masjidName }}</strong> }
            @if (content().addressText) { <br /><small>{{ content().addressText }}</small> }
            @if (content().phoneText) { <br /><small>{{ content().phoneText }}</small> }
          </p>
        }
        @if (content().intro) { <p class="cert-intro">{{ content().intro }}</p> }
        @if (content().body) { <p class="cert-body">{{ content().body }}</p> }
        @if (content().footer) { <p class="cert-footer">{{ content().footer }}</p> }
        <div class="cert-signatures">
          <div><span class="cert-line"></span>{{ content().leftLabel }}</div>
          <div><span class="cert-line"></span>{{ content().rightLabel }}</div>
        </div>
      </div>
    </div>
  `,
  styles: `
    app-certificate-preview { display: block; }
    .cert-sheet { background: #fff; color: #1f2933; padding: 10px; border: 1px solid #dfe3e8; border-radius: 6px;
      box-shadow: 0 1px 6px rgba(0,0,0,.08); aspect-ratio: 8.5 / 11; width: 100%; max-width: 640px; margin: 0 auto; container-type: inline-size; }
    .cert-sheet.cert-a4 { aspect-ratio: 210 / 297; }
    .cert-frame { position: relative; height: 100%; border: 3px double var(--cert-accent, #4f7a00); padding: 5cqw 6cqw;
      text-align: center; line-height: 1.6; display: flex; flex-direction: column; overflow: hidden; font-size: 3cqw; }
    .cert-frame > * { position: relative; z-index: 1; }
    .cert-logo { display: block; max-height: 11cqw; max-width: 30cqw; object-fit: contain; margin: 0 auto 2cqw; }
    .cert-bismillah { font-size: 4.6cqw; margin: 0 0 1cqw; }
    .cert-title { font-family: Georgia, 'Times New Roman', serif; font-size: 6.4cqw; color: var(--cert-accent, #4f7a00); margin: 0 0 2cqw; line-height: 1.2; }
    .cert-masjid { margin: 0 0 4cqw; font-size: 3.4cqw; }
    .cert-masjid small { color: #5f6b7a; font-size: 2.8cqw; }
    .cert-intro { margin: 0 0 2cqw; font-style: italic; }
    .cert-body { font-size: 3.7cqw; line-height: 1.8; margin: 0 0 3cqw; white-space: pre-line; overflow-wrap: anywhere; }
    .cert-footer { font-size: 2.2cqw; color: #5f6b7a; white-space: pre-line; margin: 0; overflow-wrap: anywhere; }
    .cert-signatures { margin-top: auto; padding-top: 6cqw; display: flex; justify-content: space-around; gap: 3cqw; font-size: 2.8cqw; }
    .cert-signatures > div { flex: 1; min-width: 0; overflow-wrap: anywhere; }
    .cert-line { display: block; width: 90%; max-width: 38cqw; margin: 0 auto 1cqw; border-top: 1px solid #1f2933; }
    .cert-watermark { position: absolute !important; inset: 0; z-index: 0 !important; display: flex; align-items: center; justify-content: center;
      font-size: 16cqw; font-weight: 800; letter-spacing: .1em; color: rgba(180, 30, 30, .13); transform: rotate(-30deg);
      pointer-events: none; user-select: none; white-space: nowrap; }
    @media print {
      app-certificate-preview .cert-sheet { border: 0; box-shadow: none; border-radius: 0; padding: 0; max-width: none; width: 100%;
        height: 100vh; aspect-ratio: auto; page-break-inside: avoid; break-inside: avoid; -webkit-print-color-adjust: exact; print-color-adjust: exact; }
      app-certificate-preview .cert-frame { font-size: 11pt; padding: 18mm 16mm; }
      app-certificate-preview .cert-title { font-size: 28pt; }
      app-certificate-preview .cert-bismillah { font-size: 20pt; }
      app-certificate-preview .cert-masjid { font-size: 13pt; }
      app-certificate-preview .cert-masjid small { font-size: 10.5pt; }
      app-certificate-preview .cert-body { font-size: 14pt; }
      app-certificate-preview .cert-footer { font-size: 10pt; }
      app-certificate-preview .cert-signatures { font-size: 10.5pt; }
      app-certificate-preview .cert-logo { max-height: 28mm; max-width: 70mm; }
      app-certificate-preview .cert-watermark { font-size: 110pt; }
    }
  `,
})
export class CertificatePreview {
  readonly content = input.required<CertificateContent>();
  readonly watermark = input<string | null>(null);
  readonly logoUrl = input<string | null>(null);
  readonly paper = input<string>('LETTER');

  /** Only a #rrggbb value may reach the style binding. */
  protected readonly accent = computed(() => (/^#[0-9a-fA-F]{6}$/.test(this.content().accentColor) ? this.content().accentColor : '#4f7a00'));
}
