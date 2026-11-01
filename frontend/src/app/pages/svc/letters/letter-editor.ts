import { HttpClient } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { errorMessage } from '../../../core/auth.service';
import { LETTERS_API, Letter, LetterType, todayIso } from './letter.model';

/** Create, edit, void, download and print one letter. The type pre-fills the wording; the imam edits the final text. */
@Component({
  selector: 'app-letter-editor',
  imports: [FormsModule, RouterLink],
  template: `
    <div class="page-title-box d-flex flex-wrap align-items-center justify-content-between">
      <h4 class="page-title">{{ !id ? 'New letter' : letter()?.letterNumber || 'Letter' }}
        @if (letter()?.status === 'VOID') { <span class="badge badge-secondary ml-2">Void</span> }
      </h4>
      <a class="btn btn-secondary btn-responsive" routerLink="/imam/svc/letters">Back to letters</a>
    </div>
    @if (error()) { <div class="alert alert-danger">{{ error() }}</div> }
    @if (notice()) { <div class="alert alert-success">{{ notice() }}</div> }

    @if (letter()?.status === 'VOID') {
      <div class="alert alert-warning">This letter was voided{{ letter()!.voidReason ? ': ' + letter()!.voidReason : '' }}. It can no longer be edited.</div>
    }

    <form class="card" (ngSubmit)="save()"><div class="card-body">
      <fieldset [disabled]="readonly()" class="border-0 p-0 m-0" style="min-width:0">
        <div class="row">
          <div class="form-group col-md-6">
            <label>Letter type</label>
            <select class="form-control" name="type" [ngModel]="f.letterType" (ngModelChange)="typeChanged($event)" required>
              @for (t of types(); track t.key) { <option [value]="t.key">{{ t.label }}</option> }
            </select>
          </div>
          <div class="form-group col-md-6">
            <label>Letter date</label>
            <input class="form-control" type="date" name="date" [(ngModel)]="f.letterDate" required />
          </div>
          <div class="form-group col-md-6">
            <label>Person the letter is about</label>
            <input class="form-control" name="rn" [(ngModel)]="f.recipientName" maxlength="200" required />
          </div>
          <div class="form-group col-md-6">
            <label>Addressed to</label>
            <input class="form-control" name="ad" [(ngModel)]="f.addressee" maxlength="300" placeholder="e.g. Whom it may concern" />
          </div>
          <div class="form-group col-12">
            <label>Purpose</label>
            <input class="form-control" name="pu" [(ngModel)]="f.purpose" maxlength="500" placeholder="e.g. a job application" />
          </div>
        </div>
        @if (!readonly()) {
          <button type="button" class="btn btn-secondary btn-responsive mb-3" (click)="fill()" [disabled]="busy() || !f.letterType">
            <i class="mdi mdi-text-box-check-outline"></i> {{ f.body.trim() ? 'Replace wording from template' : 'Fill wording from template' }}
          </button>
        }
        <div class="form-group">
          <label>Subject</label>
          <input class="form-control" name="subj" [(ngModel)]="f.subject" maxlength="200" required />
        </div>
        <div class="form-group">
          <label>Letter text</label>
          <textarea class="form-control" name="body" rows="14" [(ngModel)]="f.body" maxlength="10000" required></textarea>
          <small class="text-muted">The masjid letterhead (logo, name, address) is added automatically on the PDF.</small>
        </div>
      </fieldset>
      <div class="d-flex flex-wrap" style="gap:.4rem">
        @if (!readonly()) {
          <button class="btn btn-primary btn-responsive" [disabled]="busy()">{{ id ? 'Save changes' : 'Create letter' }}</button>
        }
        @if (id) {
          <button type="button" class="btn btn-secondary btn-responsive" (click)="pdf(false)" [disabled]="busy()"><i class="mdi mdi-file-pdf"></i> Download PDF</button>
          <button type="button" class="btn btn-secondary btn-responsive" (click)="pdf(true)" [disabled]="busy()"><i class="mdi mdi-printer"></i> Print</button>
          @if (!readonly()) {
            <button type="button" class="btn btn-outline-danger btn-responsive" (click)="voiding.set(true)">Void letter</button>
          }
        }
      </div>
    </div></form>

    @if (voiding()) {
      <form class="card" (ngSubmit)="voidIt()"><div class="card-body">
        <strong>Void this letter</strong>
        <div class="form-group mt-2">
          <label>Reason (kept in the record)</label>
          <input class="form-control" name="reason" [(ngModel)]="reason" maxlength="500" required />
        </div>
        <div class="d-flex flex-wrap" style="gap:.4rem">
          <button class="btn btn-primary btn-responsive" [disabled]="busy() || !reason.trim()">Confirm void</button>
          <button type="button" class="btn btn-secondary btn-responsive" (click)="voiding.set(false)">Cancel</button>
        </div>
      </div></form>
    }
  `,
})
export class LetterEditor {
  private http = inject(HttpClient);
  private route = inject(ActivatedRoute);
  private router = inject(Router);

  id = this.route.snapshot.paramMap.get('id');
  types = signal<LetterType[]>([]);
  letter = signal<Letter | null>(null);
  busy = signal(false);
  error = signal('');
  notice = signal('');
  voiding = signal(false);
  reason = '';
  f = { letterType: 'CHARACTER_REFERENCE', recipientName: '', addressee: '', purpose: '', letterDate: todayIso(), subject: '', body: '' };

  constructor() {
    this.http.get<{ types: LetterType[] }>(`${LETTERS_API}/types`).subscribe({
      next: (r) => this.types.set(r.types),
      error: (e) => this.error.set(errorMessage(e)),
    });
    if (this.id) {
      this.http.get<Letter>(`${LETTERS_API}/${this.id}`).subscribe({
        next: (l) => this.load(l),
        error: (e) => this.error.set(errorMessage(e)),
      });
    }
  }

  readonly = () => this.letter()?.status === 'VOID';

  private load(l: Letter) {
    this.letter.set(l);
    this.f = {
      letterType: l.letterType, recipientName: l.recipientName, addressee: l.addressee ?? '', purpose: l.purpose ?? '',
      letterDate: l.letterDate, subject: l.subject, body: l.body,
    };
  }

  typeChanged(key: string) {
    this.f.letterType = key;
    // a fresh letter takes the new type's wording straight away; an edited one is only replaced on request
    if (!this.id && !this.f.body.trim()) this.fill();
  }

  fill() {
    this.busy.set(true);
    this.error.set('');
    this.http.post<{ subject: string; body: string }>(`${LETTERS_API}/preview`, {
      letterType: this.f.letterType, recipientName: this.f.recipientName, addressee: this.f.addressee,
      purpose: this.f.purpose, letterDate: this.f.letterDate || null,
    }).subscribe({
      next: (p) => {
        this.f.subject = p.subject;
        this.f.body = p.body;
        this.busy.set(false);
      },
      error: (e) => {
        this.busy.set(false);
        this.error.set(errorMessage(e));
      },
    });
  }

  save() {
    this.busy.set(true);
    this.error.set('');
    this.notice.set('');
    const req = this.id ? this.http.put<Letter>(`${LETTERS_API}/${this.id}`, this.f) : this.http.post<Letter>(LETTERS_API, this.f);
    req.subscribe({
      next: (l) => {
        this.busy.set(false);
        if (this.id) {
          this.load(l);
          this.notice.set('Saved.');
        } else {
          this.router.navigate(['/imam/svc/letters', l.id]);
        }
      },
      error: (e) => {
        this.busy.set(false);
        this.error.set(errorMessage(e));
      },
    });
  }

  voidIt() {
    this.busy.set(true);
    this.http.post<Letter>(`${LETTERS_API}/${this.id}/void`, { reason: this.reason }).subscribe({
      next: (l) => {
        this.busy.set(false);
        this.voiding.set(false);
        this.load(l);
      },
      error: (e) => {
        this.busy.set(false);
        this.error.set(errorMessage(e));
      },
    });
  }

  pdf(print: boolean) {
    const l = this.letter();
    if (!l) return;
    this.busy.set(true);
    this.error.set('');
    this.http.get(`${LETTERS_API}/${l.id}/pdf`, { responseType: 'blob' }).subscribe({
      next: (blob) => {
        const href = URL.createObjectURL(blob);
        if (print) {
          // the browser's PDF viewer has the print dialog
          window.open(href, '_blank');
        } else {
          const a = document.createElement('a');
          a.href = href;
          a.download = `${l.letterNumber}.pdf`;
          document.body.appendChild(a);
          a.click();
          a.remove();
        }
        setTimeout(() => URL.revokeObjectURL(href), 60000);
        this.busy.set(false);
      },
      error: (e) => {
        this.busy.set(false);
        this.error.set(errorMessage(e));
      },
    });
  }
}
