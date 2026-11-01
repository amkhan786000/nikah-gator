export interface Session {
  time: string;
  label: string;
}

export interface SunPreview {
  sunrise: string;
  sunset: string;
  maghrib: string;
}

export interface Settings {
  changeUnit: 'DAYS' | 'WEEKS' | 'MONTHS';
  changeEvery: number;
  publicEnabled: boolean;
  publicDays: number;
  showSunrise: boolean;
  latitude: number | null;
  longitude: number | null;
  maghribOffsetMinutes: number;
}

export interface Period {
  id: number;
  startDate: string;
  endDate: string;
  fajr: string;
  sunrise: string | null;
  dhuhr: string;
  asr: string;
  maghrib: string | null;
  isha: string;
  jumuah: Session[];
  notes: string | null;
  current: boolean;
}

export interface Overview {
  settings: Settings;
  periods: Period[];
  slug: string;
  timezone: string;
  today: string;
  coveredThrough: string | null;
  daysLeft: number | null;
  todayCovered: boolean;
  nextStart: string;
  nextEnd: string;
  lastPeriod: Period | null;
  sunToday: SunPreview | null;
}

export interface ImportRow {
  line: number;
  startDate: string;
  endDate: string;
  fajr: string;
  sunrise: string | null;
  dhuhr: string;
  asr: string;
  maghrib: string | null;
  isha: string;
  jumuah: Session[];
  notes: string | null;
}

export interface ImportResult {
  committed: boolean;
  rowCount: number;
  replacedCount: number;
  rows: ImportRow[];
  errors: string[];
}
