import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

import { ActionButtonComponent } from '../../shared/ui/action-button/action-button.component';
import { AuthCardComponent } from '../../shared/ui/auth-card/auth-card.component';

@Component({
  selector: 'app-register-page',
  imports: [RouterLink, ActionButtonComponent, AuthCardComponent],
  templateUrl: './register-page.component.html',
  styleUrl: './register-page.component.scss'
})
export class RegisterPageComponent {}
