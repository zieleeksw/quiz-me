import { AbstractControl, ValidationErrors, ValidatorFn, Validators } from '@angular/forms';
import { finalize } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Component, DestroyRef, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';

import { AuthService } from '../../core/auth/auth.service';
import { extractApiMessage, extractFieldErrors } from '../../shared/api/api-error.utils';
import { ActionButtonComponent } from '../../shared/ui/action-button/action-button.component';
import { AuthCardComponent } from '../../shared/ui/auth-card/auth-card.component';

const passwordMatchValidator: ValidatorFn = (control: AbstractControl): ValidationErrors | null => {
  const password = control.get('password')?.value;
  const confirmPassword = control.get('confirmPassword')?.value;

  if (!password || !confirmPassword) {
    return null;
  }

  return password === confirmPassword ? null : { passwordMismatch: true };
};

@Component({
  selector: 'app-register-page',
  imports: [ReactiveFormsModule, RouterLink, ActionButtonComponent, AuthCardComponent],
  templateUrl: './register-page.component.html',
  styleUrl: './register-page.component.scss'
})
export class RegisterPageComponent {
  private readonly formBuilder = inject(FormBuilder);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);

  readonly isSubmitting = signal(false);
  readonly hasSubmitted = signal(false);
  readonly formError = signal<string | null>(null);
  readonly serverFieldErrors = signal<Record<string, string>>({});

  readonly form = this.formBuilder.nonNullable.group(
    {
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required, Validators.minLength(12), Validators.maxLength(128)]],
      confirmPassword: ['', [Validators.required]]
    },
    {
      validators: [passwordMatchValidator]
    }
  );

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

    const { email, password } = this.form.getRawValue();

    this.formError.set(null);
    this.serverFieldErrors.set({});
    this.isSubmitting.set(true);

    this.authService
      .register({ email, password })
      .pipe(
        finalize(() => this.isSubmitting.set(false)),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe({
        next: () => void this.router.navigateByUrl('/dashboard'),
        error: (error) => {
          this.serverFieldErrors.set(extractFieldErrors(error));
          this.formError.set(extractApiMessage(error) ?? 'Unable to create your account right now.');
        }
      });
  }

  hasError(controlName: 'email' | 'password' | 'confirmPassword'): boolean {
    const control = this.form.controls[controlName];

    if (controlName !== 'confirmPassword' && this.serverFieldErrors()[controlName]) {
      return true;
    }

    if (controlName === 'confirmPassword' && this.form.hasError('passwordMismatch')) {
      return this.hasSubmitted();
    }

    return this.hasSubmitted() && control.invalid;
  }

  getErrorMessage(controlName: 'email' | 'password' | 'confirmPassword'): string | null {
    if (controlName !== 'confirmPassword') {
      const serverError = this.serverFieldErrors()[controlName];

      if (serverError) {
        return serverError;
      }
    }

    const control = this.form.controls[controlName];

    if (!control.errors && !(controlName === 'confirmPassword' && this.form.hasError('passwordMismatch'))) {
      return null;
    }

    if (!this.hasSubmitted()) {
      return null;
    }

    if (control.errors?.['required']) {
      if (controlName === 'email') {
        return 'Email is required.';
      }

      return controlName === 'password' ? 'Password is required.' : 'Please confirm your password.';
    }

    if (control.errors?.['email']) {
      return 'Enter a valid email address.';
    }

    if (control.errors?.['minlength']) {
      return 'Password must be at least 12 characters long.';
    }

    if (control.errors?.['maxlength']) {
      return 'Password cannot be longer than 128 characters.';
    }

    if (controlName === 'confirmPassword' && this.form.hasError('passwordMismatch')) {
      return 'Passwords must match.';
    }

    return null;
  }
}
