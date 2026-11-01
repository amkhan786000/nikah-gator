export type DivorceType = 'TALAQ' | 'KHULA' | 'MUTUAL' | 'FASKH';

export interface Divorce {
  id?: number;
  nikahId: number | null;
  number?: string;
  divorceType: DivorceType;
  status: 'RECORDED' | 'REVOKED';
  groomName: string;
  brideName: string;
  groomFatherName: string | null;
  brideFatherName: string | null;
  marriageDate: string | null;
  marriagePlace: string | null;
  divorceDate: string;
  talaqNumber: number | null;
  iddahEndDate: string | null;
  revokedDate: string | null;
  settlementNotes: string | null;
  reconciliationNotes: string | null;
  witness1Name: string | null;
  witness2Name: string | null;
  privateNotes: string | null;
  imamName?: string | null;
  voided?: boolean;
  voidReason?: string | null;
  voidedAt?: string | null;
}

export const DIVORCE_API = '/api/imam/svc/divorce';

export const DIVORCE_TYPES: { value: DivorceType; label: string }[] = [
  { value: 'TALAQ', label: 'Talaq' },
  { value: 'KHULA', label: 'Khula' },
  { value: 'MUTUAL', label: 'Mutual agreement' },
  { value: 'FASKH', label: 'Faskh (annulment)' },
];

export function typeLabel(t: string): string {
  return DIVORCE_TYPES.find((x) => x.value === t)?.label ?? t;
}

export function talaqLabel(n: number | null): string {
  return n === 1 ? '1st talaq' : n === 2 ? '2nd talaq' : n === 3 ? '3rd talaq (final)' : '';
}
