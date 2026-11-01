import { HttpClient } from '@angular/common/http';
import { DatePipe } from '@angular/common';
import { Component, OnInit, inject, input, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { CatalogService } from '../../../core/catalog.service';
import { DIVORCE_API, Divorce } from './divorce.model';

/** Small note on the nikah page: shown only when the divorce service is on and a divorce is recorded for this nikah. */
@Component({
  selector: 'app-nikah-divorce-note',
  imports: [RouterLink, DatePipe],
  template: `
    @if (divorce(); as d) {
      <div class="alert alert-secondary no-print">
        <i class="mdi mdi-heart-broken"></i> Divorce recorded {{ d.divorceDate | date: 'mediumDate' }}
        <a class="ml-2" [routerLink]="['/imam/svc/divorce', d.id]">View record</a>
      </div>
    }
  `,
})
export class NikahDivorceNote implements OnInit {
  private http = inject(HttpClient);
  private catalog = inject(CatalogService);

  nikahId = input.required<number>();
  divorce = signal<Divorce | null>(null);

  ngOnInit() {
    this.catalog.has('DIVORCE').subscribe((on) => {
      if (!on) return;
      this.http.get<Divorce>(`${DIVORCE_API}/by-nikah/${this.nikahId()}`).subscribe({
        next: (r) => this.divorce.set(r),
        error: () => undefined,
      });
    });
  }
}
