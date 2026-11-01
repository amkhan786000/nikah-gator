export interface Settings {
  changeUnit: 'DAYS' | 'WEEKS' | 'MONTHS';
  changeEvery: number;
  publicEnabled: boolean;
  publicDays: number;
  showSunrise: boolean;
}

export interface Period {
  id: number;
  startDate: string;
  endDate: string;
  fajr: string;
  sunrise: string | null;
  dhuhr: string;
  asr: string;
  maghrib: string;
  isha: string;
  jumuah: string | null;
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
}

export interface ImportRow {
  line: number;
  startDate: string;
  endDate: string;
  fajr: string;
  sunrise: string | null;
  dhuhr: string;
  asr: string;
  maghrib: string;
  isha: string;
  jumuah: string | null;
  notes: string | null;
}

export interface ImportResult {
  committed: boolean;
  rowCount: number;
  replacedCount: number;
  rows: ImportRow[];
  errors: string[];
}
