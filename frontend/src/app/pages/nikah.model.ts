export interface Nikah {
  id?: number;
  status?: string;
  groomName: string;
  groomFatherName: string | null;
  groomEmail: string | null;
  groomPhone: string | null;
  brideName: string;
  brideFatherName: string | null;
  brideEmail: string | null;
  bridePhone: string | null;
  waliName: string | null;
  waliRelation: string | null;
  witness1Name: string | null;
  witness2Name: string | null;
  mahrDescription: string | null;
  nikahAt: string;
  notes: string | null;
  createdAt?: string;
}
