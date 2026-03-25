import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

import { ActionButtonComponent } from '../../shared/ui/action-button/action-button.component';
import { AuthCardComponent } from '../../shared/ui/auth-card/auth-card.component';

@Component({
  selector: 'app-login-page',
  imports: [RouterLink, ActionButtonComponent, AuthCardComponent],
  templateUrl: './login-page.component.html',
  styleUrl: './login-page.component.scss'
})
export class LoginPageComponent {}
