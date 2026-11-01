import { Component } from '@angular/core';
import { LifeEventPage } from '../life-event/life-event';
import { LifeEventConfig } from '../life-event/life-event.model';

const CONFIG: LifeEventConfig = {
  slug: 'baby-naming',
  title: 'Baby naming & Aqeeqah',
  singular: 'naming certificate',
  icon: 'mdi-baby-face-outline',
  dateKey: 'secondaryDate',
  dateLabel: 'Naming',
  nameLabel: 'Child',
  fields: [
    { key: 'personName', label: "Child's name", type: 'text', required: true },
    { key: 'eventDate', label: 'Birth date', type: 'date', required: true },
    { key: 'parents', label: "Parents' names", type: 'text', required: true, placeholder: 'e.g. Ali Khan and Sara Khan', col: 12 },
    { key: 'secondaryDate', label: 'Date of naming / aqeeqah', type: 'date', required: true },
    { key: 'place', label: 'Place', type: 'text' },
    { key: 'imamName', label: 'Imam (printed on the certificate)', type: 'text', placeholder: 'Defaults to you' },
    { key: 'notes', label: 'Notes (shown on the certificate)', type: 'textarea', col: 12 },
  ],
};

@Component({
  selector: 'app-baby-naming',
  imports: [LifeEventPage],
  template: `<app-life-event [cfg]="cfg" />`,
})
export class BabyNaming {
  cfg = CONFIG;
}
