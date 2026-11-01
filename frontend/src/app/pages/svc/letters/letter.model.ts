export interface LetterType {
  key: string;
  label: string;
  subject: string;
  body: string;
}

export interface Letter {
  id: number;
  letterNumber: string;
  letterType: string;
  letterTypeLabel: string;
  recipientName: string;
  addressee: string | null;
  purpose: string | null;
  subject: string;
  body: string;
  letterDate: string;
  signerName: string;
  status: 'ISSUED' | 'VOID';
  voidReason: string | null;
  voidedAt: string | null;
  createdAt: string;
}

export const LETTERS_API = '/api/imam/svc/letters';

export function todayIso(): string {
  const d = new Date();
  const p = (n: number) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())}`;
}
