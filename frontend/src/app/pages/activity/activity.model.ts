export interface ActivityRow {
  id: number;
  masjidId: number | null;
  masjidName: string | null;
  userId: number | null;
  userName: string | null;
  userRole: string | null;
  action: string;
  entityType: string | null;
  entityId: number | null;
  summary: string;
  changedFields: string | null; // JSON {"field":{"old":..,"new":..}}
  ip: string | null;
  userAgent: string | null;
  createdAt: string;
}

export interface ActivityPage {
  items: ActivityRow[];
  total: number;
  page: number;
  size: number;
}

export interface FilterChoices {
  users: { id: number; name: string }[];
  entityTypes: string[];
  actions: string[];
}

const ACTIONS: Record<string, string> = {
  CREATE: 'Created',
  UPDATE: 'Changed',
  DELETE: 'Deleted',
  VOID: 'Voided',
  LOGIN: 'Signed in',
  LOGIN_FAILED: 'Failed sign-in',
  LOGOUT: 'Signed out',
  VIEW: 'Opened',
};

export const actionLabel = (a: string): string => ACTIONS[a] ?? a;

export const actionBadge = (a: string): string =>
  'badge ' +
  ({ CREATE: 'badge-success', UPDATE: 'badge-info', DELETE: 'badge-danger', VOID: 'badge-danger', LOGIN: 'badge-secondary',
     LOGIN_FAILED: 'badge-warning', LOGOUT: 'badge-secondary', VIEW: 'badge-warning' }[a] ?? 'badge-secondary');

const RECORDS: Record<string, string> = {
  NikahRequest: 'Nikah',
  Certificate: 'Nikah certificate',
  ServiceCert: 'Community service certificate',
  JumaLetter: 'Juma letter',
  LifeEvent: 'Life event record',
  Divorce: 'Divorce record',
  CounselingSession: 'Counseling session',
  HallBooking: 'Hall booking',
  AppUser: 'User account',
  MasjidReminderSettings: 'Reminder settings',
  MasjidService: 'Masjid service',
  ImamService: 'Imam service setting',
  StaffPermission: 'Staff permission',
  NikahFee: 'Nikah fee',
};

export function recordLabel(t: string): string {
  if (!t) return '';
  if (RECORDS[t]) return RECORDS[t];
  const spaced = t.replace(/([a-z])([A-Z])/g, '$1 $2').toLowerCase();
  return spaced.charAt(0).toUpperCase() + spaced.slice(1);
}

/** The before/after rows of an entry, with readable field names ("groomName" -> "groom name"). */
export function changesOf(r: ActivityRow): { field: string; old: string | null; new: string | null }[] {
  if (!r.changedFields) return [];
  try {
    const o = JSON.parse(r.changedFields) as Record<string, { old: string | null; new: string | null }>;
    return Object.entries(o).map(([k, v]) => ({
      field: k === 'passwordHash' ? 'password' : k.replace(/([a-z])([A-Z])/g, '$1 $2').toLowerCase(),
      old: v.old,
      new: v.new,
    }));
  } catch {
    return [];
  }
}
