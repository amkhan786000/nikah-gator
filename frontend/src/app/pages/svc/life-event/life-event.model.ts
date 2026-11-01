export interface LifeEvent {
  id?: number;
  number?: string;
  status?: 'VALID' | 'VOID';
  personName: string;
  altName: string | null;
  eventDate: string;
  secondaryDate: string | null;
  secondaryTime: string | null;
  place: string | null;
  burialDate: string | null;
  burialTime: string | null;
  burialPlace: string | null;
  cemetery: string | null;
  contactName: string | null;
  contactPhone: string | null;
  parents: string | null;
  witnesses: string | null;
  imamName: string | null;
  notes: string | null;
  voidReason?: string | null;
}

export type FieldKey = Exclude<keyof LifeEvent, 'id' | 'number' | 'status' | 'voidReason'>;

export interface FieldDef {
  key: FieldKey;
  label: string;
  type: 'text' | 'date' | 'time' | 'tel' | 'textarea';
  required?: boolean;
  placeholder?: string;
  /** Bootstrap column width, default 6. */
  col?: 4 | 6 | 12;
}

export interface LifeEventConfig {
  slug: string;
  title: string;
  singular: string;
  icon: string;
  /** Which date the list shows, and its column heading. */
  dateKey: FieldKey;
  dateLabel: string;
  nameLabel: string;
  sensitive?: boolean;
  fields: FieldDef[];
}
