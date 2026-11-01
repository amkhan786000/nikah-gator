export type Level = 'VIEW' | 'MANAGE';

export interface StaffView {
  id: number;
  fullName: string;
  email: string;
  phone: string | null;
  jobTitle: string | null;
  enabled: boolean;
  mustChangePassword: boolean;
  /** service key -> level; a missing key means no access */
  permissions: Record<string, Level>;
  credentialsEmailed: boolean;
  createdAt: string;
}
