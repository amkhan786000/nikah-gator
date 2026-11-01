export const SESSION_TYPES = ['MARRIAGE', 'FAMILY', 'YOUTH', 'PERSONAL', 'OTHER'];
export const SESSION_STATUSES = ['SCHEDULED', 'COMPLETED', 'CANCELLED'];

/** A list row: the server never sends notes in lists. */
export interface CounselingRow {
  id: number;
  clientName: string;
  sessionType: string;
  sessionAt: string;
  durationMin: number | null;
  followUpOn: string | null;
  status: string;
}

export interface CounselingDetail extends CounselingRow {
  notes: string | null;
}

export function statusBadge(s: string): string {
  return s === 'COMPLETED' ? 'badge badge-success' : s === 'CANCELLED' ? 'badge badge-secondary' : 'badge badge-info';
}
