export const HALL_STATUSES = ['REQUESTED', 'CONFIRMED', 'CANCELLED'];

export interface HallBooking {
  id?: number;
  title: string;
  contactName: string;
  contactPhone: string | null;
  contactEmail: string | null;
  startsAt: string;
  endsAt: string;
  space: string;
  expectedGuests: number | null;
  status: string;
  feeText: string | null;
  notes: string | null;
}

export function hallBadge(s: string): string {
  return s === 'CONFIRMED' ? 'badge badge-success' : s === 'CANCELLED' ? 'badge badge-secondary' : 'badge badge-warning';
}
