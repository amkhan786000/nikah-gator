export interface ServiceCert {
  id?: number;
  certNumber?: string;
  personName: string;
  personType: 'STUDENT' | 'VOLUNTEER';
  organization: string | null;
  activities: string;
  startDate: string;
  endDate: string;
  hours: number | null;
  supervisor: string;
  notes: string | null;
  issueDate?: string;
  status?: 'VALID' | 'VOID';
  voidReason?: string | null;
}

export const CS_API = '/api/imam/svc/community-service';
