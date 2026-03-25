import { Component, input } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-action-button',
  imports: [RouterLink],
  templateUrl: './action-button.component.html',
  styleUrl: './action-button.component.scss'
})
export class ActionButtonComponent {
  readonly label = input.required<string>();
  readonly helper = input<string>('');
  readonly routerLink = input<string | null>(null);
  readonly tone = input<'primary' | 'secondary'>('primary');
  readonly nativeType = input<'button' | 'submit'>('button');
  readonly disabled = input(false);
}
