import { Component } from '@angular/core';

@Component({
  selector: 'app-service-disabled',
  template: `
    <div class="page-title-box"><h4 class="page-title">Not available</h4></div>
    <div class="card"><div class="card-body text-center py-5">
      <i class="mdi mdi-lock-outline" style="font-size:42px"></i>
      <p class="mt-2 mb-1"><strong>This service is not enabled for your masjid.</strong></p>
      <p class="text-muted mb-0">Ask the Nikah Gator administrator to turn it on.</p>
    </div></div>
  `,
})
export class ServiceDisabled {}
