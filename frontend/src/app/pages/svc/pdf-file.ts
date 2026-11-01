import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

/** Fetches a PDF from the API (the auth header is added by the interceptor) and saves it. */
export function downloadPdf(http: HttpClient, url: string, filename: string): Observable<void> {
  return new Observable<void>((sub) => {
    http.get(url, { responseType: 'blob' }).subscribe({
      next: (blob) => {
        const href = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = href;
        a.download = filename;
        document.body.appendChild(a);
        a.click();
        a.remove();
        setTimeout(() => URL.revokeObjectURL(href), 10000);
        sub.next();
        sub.complete();
      },
      error: (e) => sub.error(e),
    });
  });
}

/** Fetches a PDF and opens the browser's print dialog for it (falls back to opening it in a new tab). */
export function printPdf(http: HttpClient, url: string): Observable<void> {
  return new Observable<void>((sub) => {
    http.get(url, { responseType: 'blob' }).subscribe({
      next: (blob) => {
        const href = URL.createObjectURL(blob);
        const frame = document.createElement('iframe');
        frame.style.cssText = 'position:fixed;right:0;bottom:0;width:0;height:0;border:0';
        frame.src = href;
        frame.onload = () => {
          try {
            frame.contentWindow?.focus();
            frame.contentWindow?.print();
          } catch {
            window.open(href, '_blank');
          }
          setTimeout(() => {
            frame.remove();
            URL.revokeObjectURL(href);
          }, 60000);
        };
        document.body.appendChild(frame);
        sub.next();
        sub.complete();
      },
      error: (e) => sub.error(e),
    });
  });
}
