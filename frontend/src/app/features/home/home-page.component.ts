import { Component } from '@angular/core';

import { ActionButtonComponent } from '../../shared/ui/action-button/action-button.component';
import { AuthCardComponent } from '../../shared/ui/auth-card/auth-card.component';

@Component({
  selector: 'app-home-page',
  imports: [ActionButtonComponent, AuthCardComponent],
  templateUrl: './home-page.component.html',
  styleUrl: './home-page.component.scss'
})
export class HomePageComponent {}
