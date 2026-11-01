import { Component } from '@angular/core';
import { LifeEventPage } from '../life-event/life-event';
import { LifeEventConfig } from '../life-event/life-event.model';

const CONFIG: LifeEventConfig = {
  slug: 'shahada',
  title: 'New Muslim (Shahada) certificates',
  singular: 'shahada certificate',
  icon: 'mdi-star-circle',
  dateKey: 'eventDate',
  dateLabel: 'Shahada',
  nameLabel: 'Name',
  sensitive: true,
  fields: [
    { key: 'personName', label: "Person's name", type: 'text', required: true },
    { key: 'altName', label: 'Chosen Muslim name (optional)', type: 'text' },
    { key: 'eventDate', label: 'Date of shahada', type: 'date', required: true },
    { key: 'place', label: 'Place of shahada', type: 'text' },
    { key: 'witnesses', label: 'Witnesses', type: 'text', required: true, placeholder: 'e.g. Bilal Khan and Idris Ali', col: 12 },
    { key: 'imamName', label: 'Imam (printed on the certificate)', type: 'text', placeholder: 'Defaults to you' },
    { key: 'notes', label: 'Notes (shown on the certificate)', type: 'textarea', col: 12 },
  ],
};

@Component({
  selector: 'app-shahada',
  imports: [LifeEventPage],
  template: `<app-life-event [cfg]="cfg" />`,
})
export class Shahada {
  cfg = CONFIG;
}
