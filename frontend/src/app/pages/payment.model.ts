/** Shared types and helpers for fees and payments. Amounts travel as cents, the UI shows and edits dollars. */
export interface PaymentView {
  id: number;
  kind: 'FEE' | 'DONATION';
  amountCents: number;
  status: 'PAID' | 'VOID' | 'REFUNDED';
  method: string;
  paidAt: string;
  payerName: string | null;
  note: string | null;
  receiptNumber: string | null;
  statusReason: string | null;
  statusChangedAt: string | null;
  receivedByName: string | null;
}

export interface PaymentSummary {
  feeCents: number;
  feeWaived: boolean;
  waivedReason: string | null;
  paidCents: number;
  balanceCents: number;
  donationCents: number;
  status: PayStatus;
  suggestedDonationCents: number;
  payments: PaymentView[];
}

export type PayStatus = 'PAID' | 'PARTIAL' | 'UNPAID' | 'WAIVED' | 'NO_FEE';

export interface StatusRow { nikahId: number; status: PayStatus; balanceCents: number }

export const METHODS = ['CASH', 'CHECK', 'ZELLE', 'CARD', 'OTHER'];

/** Sane upper limit for one amount (matches the server): $100,000. */
export const MAX_DOLLARS = 100000;

export function dollars(cents: number | null | undefined): string {
  return ((cents ?? 0) / 100).toLocaleString('en-US', { style: 'currency', currency: 'USD' });
}

/** Dollars typed by the user to whole cents, or null if not a valid amount in [min, MAX_DOLLARS]. */
export function toCents(value: unknown, allowZero = false): number | null {
  const n = typeof value === 'number' ? value : parseFloat(String(value ?? '').replace(/[$,\s]/g, ''));
  if (!isFinite(n) || n > MAX_DOLLARS || n < 0 || (n === 0 && !allowZero)) return null;
  return Math.round(n * 100);
}

export function statusLabel(s: PayStatus): string {
  return { PAID: 'Paid', PARTIAL: 'Partial', UNPAID: 'Unpaid', WAIVED: 'Waived', NO_FEE: 'No fee' }[s];
}

export function statusBadge(s: PayStatus): string {
  return { PAID: 'badge-success', PARTIAL: 'badge-warning', UNPAID: 'badge-danger', WAIVED: 'badge-info', NO_FEE: 'badge-secondary' }[s];
}

export function today(): string {
  const d = new Date();
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
}
