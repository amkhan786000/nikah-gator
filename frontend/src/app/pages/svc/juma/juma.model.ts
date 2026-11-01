export interface JumaStudent {
  name: string;
  grade: string | null;
}

export interface JumaLetter {
  id?: number;
  schoolName: string;
  schoolAddress: string | null;
  schoolContact: string | null;
  addressee: string;
  letterDate: string | null;
  standing: boolean;
  startDate: string;
  endDate: string | null;
  windowStart: string | null;
  windowEnd: string | null;
  note: string | null;
  status?: 'ACTIVE' | 'VOID';
  voidReason?: string | null;
  students: JumaStudent[];
}

export const JUMA_API = '/api/imam/svc/juma';

/** "12:30:00" -> "12:30" for time inputs. */
export function hhmm(t: string | null): string {
  return t ? t.slice(0, 5) : '';
}

/** "12:30" -> "12:30 PM". */
export function clock(t: string | null): string {
  if (!t) return '';
  const [h, m] = t.split(':').map(Number);
  return `${h % 12 === 0 ? 12 : h % 12}:${String(m).padStart(2, '0')} ${h >= 12 ? 'PM' : 'AM'}`;
}
