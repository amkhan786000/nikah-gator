/** "17:45" -> "5:45 PM" (empty for a missing time). */
export function fmt12(hhmm: string | null | undefined): string {
  if (!hhmm) return '';
  const [h, m] = hhmm.split(':').map(Number);
  return `${h % 12 === 0 ? 12 : h % 12}:${String(m).padStart(2, '0')} ${h < 12 ? 'AM' : 'PM'}`;
}
