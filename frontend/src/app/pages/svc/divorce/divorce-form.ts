import { HttpClient } from '@angular/common/http';
import { DatePipe } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { errorMessage } from '../../../core/auth.service';
import { Nikah } from '../../nikah.model';
import { DIVORCE_API, DIVORCE_TYPES, Divorce } from './divorce.model';

function blank(): Divorce {
  return {
    nikahId: null, divorceType: 'TALAQ', status: 'RECORDED', groomName: '', brideName: '', groomFatherName: '', brideFatherName: '',
    marriageDate: '', marriagePlace: '', divorceDate: '', talaqNumber: 1, iddahEndDate: '', revokedDate: '', settlementNotes: '',
    reconciliationNotes: '', witness1Name: '', witness2Name: '', privateNotes: '',
  };
}

@Component({
  selector: 'app-divorce-form',
  imports: [FormsModule, RouterLink, DatePipe],
  template: `
    <div class="page-title-box"><h4 class="page-title">{{ id ? 'Edit divorce record' : 'New divorce record' }}</h4></div>
    @if (error()) { <div class="alert alert-danger">{{ error() }}</div> }

    <form class="card" (ngSubmit)="save()"><div class="card-body">
      <span class="card-header-title">Couple</span>
      @if (!id) {
        <div class="btn-group mb-3" role="group">
          <button type="button" class="btn" [class.btn-primary]="mode() === 'nikah'" [class.btn-outline-primary]="mode() !== 'nikah'" (click)="setMode('nikah')">Nikah recorded here</button>
          <button type="button" class="btn" [class.btn-primary]="mode() === 'new'" [class.btn-outline-primary]="mode() !== 'new'" (click)="setMode('new')">Other couple</button>
        </div>
      }
      @if (mode() === 'nikah' && !id) {
        @if (picked(); as p) {
          <div class="alert alert-light border d-flex flex-wrap align-items-center justify-content-between">
            <span>Linked nikah: <strong>{{ p.groomName }} &amp; {{ p.brideName }}</strong>, {{ p.nikahAt | date: 'mediumDate' }}</span>
            <button type="button" class="btn btn-sm btn-outline-secondary" (click)="unpick()">Change</button>
          </div>
        } @else {
          <div class="form-group">
            <label>Find the nikah by name</label>
            <div class="input-icon"><i class="mdi mdi-magnify"></i>
              <input class="form-control" name="nq" placeholder="Groom or bride name…" [ngModel]="nq()" (ngModelChange)="nq.set($event)" autocomplete="off" />
            </div>
            <div class="list-group mt-2">
              @for (n of matches(); track n.id) {
                <button type="button" class="list-group-item list-group-item-action" (click)="pick(n)">
                  {{ n.groomName }} &amp; {{ n.brideName }} <small class="text-muted">· {{ n.nikahAt | date: 'mediumDate' }}</small>
                </button>
              } @empty {
                <div class="text-muted small p-2">{{ nikahsLoaded() ? 'No matching nikah of your masjid.' : 'Loading…' }}</div>
              }
            </div>
          </div>
        }
      }
      @if (mode() === 'new' || id || picked()) {
        <div class="row">
          <div class="form-group col-md-6"><label>Groom (husband)</label><input class="form-control" name="gn" maxlength="200" [(ngModel)]="d.groomName" required /></div>
          <div class="form-group col-md-6"><label>Bride (wife)</label><input class="form-control" name="bn" maxlength="200" [(ngModel)]="d.brideName" required /></div>
          <div class="form-group col-md-6"><label>Groom's father (optional)</label><input class="form-control" name="gf" maxlength="200" [(ngModel)]="d.groomFatherName" /></div>
          <div class="form-group col-md-6"><label>Bride's father (optional)</label><input class="form-control" name="bf" maxlength="200" [(ngModel)]="d.brideFatherName" /></div>
          <div class="form-group col-md-4"><label>Date of marriage (optional)</label><input class="form-control" type="date" name="md" [(ngModel)]="d.marriageDate" /></div>
          <div class="form-group col-md-8"><label>Place of marriage (optional)</label><input class="form-control" name="mp" maxlength="300" [(ngModel)]="d.marriagePlace" /></div>
        </div>

        <span class="card-header-title mt-2">Divorce</span>
        <div class="row">
          <div class="form-group col-md-4">
            <label>Type</label>
            <select class="form-control" name="ty" [(ngModel)]="d.divorceType">
              @for (t of types; track t.value) { <option [value]="t.value">{{ t.label }}</option> }
            </select>
          </div>
          <div class="form-group col-6 col-md-4"><label>Date of divorce</label><input class="form-control" type="date" name="dd" [max]="today" [(ngModel)]="d.divorceDate" required /></div>
          @if (d.divorceType === 'TALAQ') {
            <div class="form-group col-6 col-md-4">
              <label>Which talaq</label>
              <select class="form-control" name="tn" [(ngModel)]="d.talaqNumber">
                <option [ngValue]="1">1st talaq</option>
                <option [ngValue]="2">2nd talaq</option>
                <option [ngValue]="3">3rd talaq (final)</option>
              </select>
            </div>
          }
        </div>
        @if (d.divorceType === 'TALAQ' && d.talaqNumber === 3) {
          <div class="alert alert-warning">The 3rd talaq is final: the couple cannot reconcile and remarry without the conditions set by the shariah. It cannot be marked as revoked.</div>
        }
        <div class="row">
          <div class="form-group col-md-4">
            <label>Iddah ends (optional)</label>
            <input class="form-control" type="date" name="ie" [(ngModel)]="d.iddahEndDate" />
            <button type="button" class="btn btn-link btn-sm p-0" [disabled]="!d.divorceDate" (click)="suggestIddah()">Suggest about 3 months after the divorce</button>
          </div>
        </div>
        <div class="form-group"><label>Mahr and settlement notes (optional, printed)</label><textarea class="form-control" name="sn" rows="2" maxlength="2000" [(ngModel)]="d.settlementNotes"></textarea></div>
        <div class="form-group"><label>Reconciliation attempts (optional, printed)</label><textarea class="form-control" name="rn" rows="2" maxlength="2000" [(ngModel)]="d.reconciliationNotes"></textarea></div>
        <div class="row">
          <div class="form-group col-md-6"><label>Witness 1 (optional)</label><input class="form-control" name="w1" maxlength="200" [(ngModel)]="d.witness1Name" /></div>
          <div class="form-group col-md-6"><label>Witness 2 (optional)</label><input class="form-control" name="w2" maxlength="200" [(ngModel)]="d.witness2Name" /></div>
        </div>

        @if (canRevoke()) {
          <span class="card-header-title mt-2">Status</span>
          <div class="row">
            <div class="form-group col-md-4">
              <label>Status</label>
              <select class="form-control" name="st" [(ngModel)]="d.status">
                <option value="RECORDED">Recorded</option>
                <option value="REVOKED">Revoked (reconciled during the iddah)</option>
              </select>
            </div>
            @if (d.status === 'REVOKED') {
              <div class="form-group col-md-4"><label>Date reconciled</label><input class="form-control" type="date" name="rd" [max]="today" [(ngModel)]="d.revokedDate" required /></div>
            }
          </div>
        }
        <div class="form-group"><label>Private notes (never printed)</label><textarea class="form-control" name="pn" rows="2" maxlength="2000" [(ngModel)]="d.privateNotes"></textarea></div>

        <div class="d-flex flex-wrap" style="gap:.5rem">
          <button class="btn btn-primary btn-responsive" [disabled]="busy()">{{ busy() ? 'Saving…' : 'Save' }}</button>
          <a class="btn btn-secondary btn-responsive" [routerLink]="id ? ['/imam/svc/divorce', id] : ['/imam/svc/divorce']">Cancel</a>
        </div>
      }
    </div></form>
  `,
})
export class DivorceForm {
  private http = inject(HttpClient);
  private router = inject(Router);

  id = inject(ActivatedRoute).snapshot.paramMap.get('id');
  types = DIVORCE_TYPES;
  d: Divorce = blank();
  busy = signal(false);
  error = signal('');
  today = new Date().toISOString().slice(0, 10);

  mode = signal<'nikah' | 'new'>('nikah');
  nikahs = signal<Nikah[]>([]);
  nikahsLoaded = signal(false);
  nq = signal('');
  picked = signal<Nikah | null>(null);
  matches = computed(() => {
    const q = this.nq().trim().toLowerCase();
    const all = this.nikahs();
    return (q ? all.filter((n) => (n.groomName + ' ' + n.brideName).toLowerCase().includes(q)) : all).slice(0, 8);
  });

  constructor() {
    if (this.id) {
      this.http.get<Divorce>(`${DIVORCE_API}/${this.id}`).subscribe({
        next: (r) => {
          if (r.voided) {
            this.router.navigate(['/imam/svc/divorce', this.id]);
            return;
          }
          this.d = r;
        },
        error: (e) => this.error.set(errorMessage(e)),
      });
    } else {
      this.http.get<Nikah[]>('/api/imam/nikahs').subscribe({
        next: (r) => {
          this.nikahs.set(r);
          this.nikahsLoaded.set(true);
        },
        error: () => this.nikahsLoaded.set(true),
      });
    }
  }

  canRevoke() {
    return this.d.divorceType === 'TALAQ' && this.d.talaqNumber !== 3;
  }

  setMode(m: 'nikah' | 'new') {
    this.mode.set(m);
    if (m === 'new') this.unpick();
  }

  pick(n: Nikah) {
    this.picked.set(n);
    this.d.nikahId = n.id ?? null;
    this.d.groomName = n.groomName;
    this.d.brideName = n.brideName;
    this.d.groomFatherName = n.groomFatherName ?? '';
    this.d.brideFatherName = n.brideFatherName ?? '';
    this.d.marriageDate = n.nikahAt ? n.nikahAt.slice(0, 10) : '';
  }

  unpick() {
    this.picked.set(null);
    this.d.nikahId = null;
  }

  suggestIddah() {
    const base = new Date(this.d.divorceDate + 'T00:00:00');
    base.setMonth(base.getMonth() + 3);
    const p = (n: number) => String(n).padStart(2, '0');
    this.d.iddahEndDate = `${base.getFullYear()}-${p(base.getMonth() + 1)}-${p(base.getDate())}`;
  }

  save() {
    if (!this.id && this.mode() === 'nikah' && !this.picked()) {
      this.error.set('Pick the nikah, or switch to "Other couple".');
      return;
    }
    this.busy.set(true);
    this.error.set('');
    const body = { ...this.d, talaqNumber: this.d.divorceType === 'TALAQ' ? this.d.talaqNumber : null };
    if (!this.canRevoke()) body.status = 'RECORDED';
    const call = this.id ? this.http.put<Divorce>(`${DIVORCE_API}/${this.id}`, body) : this.http.post<Divorce>(DIVORCE_API, body);
    call.subscribe({
      next: (r) => this.router.navigate(['/imam/svc/divorce', r.id]),
      error: (e) => {
        this.busy.set(false);
        this.error.set(errorMessage(e));
      },
    });
  }
}
