/** Client-side twin of CertificateTemplateService.render (same {{key}} rules), so typing needs no server call. */

export interface CertificateContent {
  title: string;
  bismillah: boolean;
  masjidName: string;
  addressText: string;
  phoneText: string;
  intro: string;
  body: string;
  footer: string;
  leftLabel: string;
  rightLabel: string;
  accentColor: string;
  hasLogo: boolean;
}

const PLACEHOLDER = /\{\{\s*([A-Za-z][A-Za-z0-9]*)\s*\}\}/g;

function stripControl(s: string): string {
  // eslint-disable-next-line no-control-regex
  return s.replace(/\r/g, '').replace(/[\u0000-\u0009\u000b-\u001f\u007f-\u009f]/g, '');
}

/** Replaces {{key}} with plain-text values (unknown keys become empty), then tidies empty clauses. Values are not re-scanned. */
export function renderTemplate(text: string | null | undefined, values: Record<string, string>): string {
  if (!text) return '';
  const out = text.replace(PLACEHOLDER, (_m, key: string) => stripControl(values[key] ?? '').trim());
  return tidy(out);
}

export function tidy(s: string): string {
  return s
    .replace(/\(\s*\)/g, '')
    .replace(/\s(son|daughter) of\s+(?=with\b|was\b|[,.;:]|$)/gi, ' ')
    .replace(/,?\s*\b(with mahr|mahr|wali|witnesses|witness)\s*:\s*(?=[.,;]|$)/gi, '')
    .replace(/[ \t]{2,}/g, ' ')
    .replace(/[ \t]+([,.;:])/g, '$1')
    .replace(/,{2,}/g, ',')
    .replace(/\.{2,}(?!\.)/g, '.')
    .replace(/,\s*\./g, '.')
    .replace(/[ \t]+\n/g, '\n')
    .trim();
}
