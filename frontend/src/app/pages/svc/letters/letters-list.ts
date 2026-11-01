import { HttpClient } from '@angular/common/http';
import { DatePipe } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { errorMessage } from '../../../core/auth.service';
import { CardTableDirective } from '../../../shared/card-table.directive';
import { LETTERS_API, Letter } from './letter.model';

@Component({
  selector: 'app-letters-list',
  imports: [CardTableDirective, RouterLink, DatePipe, FormsModule],
  template: `
    <div class="page-title-box d-flex flex-wrap align-items-center justify-content-between">
      <h4 class="page-title">Letters</h4>
      <a class="btn btn-primary btn-responsive" routerLink="/imam/svc/letters/new"><i class="mdi mdi-plus"></i> New letter</a>
    </div>
    @if (error()) { <div class="alert alert-danger">{{ error() }}</div> }

    <div class="card"><div class="card-body">
      <div class="input-icon mb-3" style="max-width:340px">
        <i class="mdi mdi-magnify"></i>
        <input class="form-control" placeholder="Search by name, number or type…" [ngModel]="q()" (ngModelChange)="q.set($event)" />
      </div>
      <div class="table-responsive">
        <table class="table table-hover mb-0">
          <thead><tr><th>Date</th><th>Number</th><th>Type</th><th>Regarding</th><th>Status</th><th></th></tr></thead>
          <tbody>
            @for (l of shown(); track l.id) {
              <tr [class.text-muted]="l.status === 'VOID'">
                <td class="text-nowrap">{{ l.letterDate | date: 'MMM d, y' }}</td>
                <td class="text-nowrap">{{ l.letterNumber }}</td>
                <td>{{ l.letterTypeLabel }}</td>
                <td>{{ l.recipientName }}</td>
                <td><span class="badge" [class.badge-success]="l.status === 'ISSUED'" [class.badge-secondary]="l.status === 'VOID'">{{ l.status === 'VOID' ? 'Void' : 'Issued' }}</span></td>
                <td class="text-right"><a class="btn btn-sm btn-outline-primary" [routerLink]="['/imam/svc/letters', l.id]">Open</a></td>
              </tr>
            } @empty {
              <tr><td colspan="6" class="text-center text-muted py-4">{{ loaded() ? 'No letters yet.' : 'Loading…' }}</td></tr>
            }
          </tbody>
        </table>
      </div>
    </div></div>
  `,
})
export class LettersList {
  private http = inject(HttpClient);

  letters = signal<Letter[]>([]);
  q = signal('');
  loaded = signal(false);
  error = signal('');
  shown = computed(() => {
    const q = this.q().trim().toLowerCase();
    return q
      ? this.letters().filter((l) => (l.recipientName + ' ' + l.letterNumber + ' ' + l.letterTypeLabel).toLowerCase().includes(q))
      : this.letters();
  });

  constructor() {
    this.http.get<Letter[]>(LETTERS_API).subscribe({
      next: (r) => {
        this.letters.set(r);
        this.loaded.set(true);
      },
      error: (e) => this.error.set(errorMessage(e)),
    });
  }
}
