import { Component } from '@angular/core';
import { LifeEventPage } from '../life-event/life-event';
import { LifeEventConfig } from '../life-event/life-event.model';

const CONFIG: LifeEventConfig = {
  slug: 'janazah',
  title: 'Janazah records',
  singular: 'janazah',
  icon: 'mdi-flower-outline',
  dateKey: 'secondaryDate',
  dateLabel: 'Janazah',
  nameLabel: 'Deceased',
  fields: [
    { key: 'personName', label: 'Name of the deceased', type: 'text', required: true },
    { key: 'eventDate', label: 'Date of death', type: 'date', required: true },
    { key: 'secondaryDate', label: 'Janazah date', type: 'date', required: true, col: 4 },
    { key: 'secondaryTime', label: 'Janazah time', type: 'time', col: 4 },
    { key: 'place', label: 'Janazah place', type: 'text', placeholder: 'e.g. Main prayer hall', col: 4 },
    { key: 'burialDate', label: 'Burial date', type: 'date', col: 4 },
    { key: 'burialTime', label: 'Burial time', type: 'time', col: 4 },
    { key: 'cemetery', label: 'Cemetery', type: 'text', col: 4 },
    { key: 'burialPlace', label: 'Burial place / address', type: 'text' },
    { key: 'contactName', label: 'Next of kin', type: 'text' },
    { key: 'contactPhone', label: 'Contact phone', type: 'tel' },
    { key: 'imamName', label: 'Imam (printed on the letter)', type: 'text', placeholder: 'Defaults to you' },
    { key: 'notes', label: 'Notes (shown on the letter)', type: 'textarea', col: 12 },
  ],
};

@Component({
  selector: 'app-janazah',
  imports: [LifeEventPage],
  template: `<app-life-event [cfg]="cfg" />`,
})
export class Janazah {
  cfg = CONFIG;
}
