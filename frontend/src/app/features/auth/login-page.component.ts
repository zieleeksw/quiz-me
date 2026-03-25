import { finalize } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Component, DestroyRef, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';

import { AuthService } from '../../core/auth/auth.service';
import { extractApiMessage, extractFieldErrors } from '../../shared/api/api-error.utils';
import { ActionButtonComponent } from '../../shared/ui/action-button/action-button.component';
import { AuthCardComponent } from '../../shared/ui/auth-card/auth-card.component';

@Component({
  selector: 'app-login-page',
  imports: [ReactiveFormsModule, RouterLink, ActionButtonComponent, AuthCardComponent],
  templateUrl: './login-page.component.html',
  styleUrl: './login-page.component.scss'
})
export class LoginPageComponent {
  private readonly formBuilder = inject(FormBuilder);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);

  readonly isSubmitting = signal(false);
  readonly hasSubmitted = signal(false);
  readonly formError = signal<string | null>(null);
  readonly serverFieldErrors = signal<Record<string, string>>({});

  readonly form = this.formBuilder.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required]]
  });

  constructor() {
    this.form.valueChanges.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(() => {
      this.hasSubmitted.set(false);
      this.formError.set(null);
      this.serverFieldErrors.set({});
    });
  }

  submit(): void {
    this.hasSubmitted.set(true);

    if (this.form.invalid) {
      return;
    }

    this.formError.set(null);
    this.serverFieldErrors.set({});
    this.isSubmitting.set(true);

    this.authService
      .login(this.form.getRawValue())
      .pipe(
        finalize(() => this.isSubmitting.set(false)),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe({
        next: () => void this.router.navigateByUrl('/dashboard'),
        error: (error) => {
          this.serverFieldErrors.set(extractFieldErrors(error));
          this.formError.set(extractApiMessage(error) ?? 'Invalid email or password.');
        }
      });
  }

  hasError(controlName: 'email' | 'password'): boolean {
    const control = this.form.controls[controlName];
    return Boolean(this.serverFieldErrors()[controlName]) || (this.hasSubmitted() && control.invalid);
  }

  getErrorMessage(controlName: 'email' | 'password'): string | null {
    const serverError = this.serverFieldErrors()[controlName];

    if (serverError) {
      return serverError;
    }

    const control = this.form.controls[controlName];

    if (!this.hasSubmitted() || !control.errors) {
      return null;
    }

    if (control.errors['required']) {
      return controlName === 'email' ? 'Email is required.' : 'Password is required.';
    }

    if (control.errors['email']) {
      return 'Enter a valid email address.';
    }

    return null;
  }
}
