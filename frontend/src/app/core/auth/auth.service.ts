import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { Observable, catchError, finalize, map, of, switchMap } from 'rxjs';

import { AuthenticationDto, AuthSession, LoginRequest, RegisterRequest } from './auth.models';

const AUTH_STORAGE_KEY = 'quiz-me.auth-session';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);
  private readonly apiBaseUrl = `${window.location.protocol}//${window.location.hostname}:8080`;
  private readonly sessionState = signal<AuthSession | null>(this.restoreSession());

  readonly session = this.sessionState.asReadonly();
  readonly user = computed(() => this.sessionState()?.user ?? null);
  readonly isAuthenticated = computed(() => {
    const session = this.sessionState();
    return Boolean(session?.accessToken && session?.refreshToken);
  });

  login(payload: LoginRequest): Observable<AuthSession> {
    return this.http
      .post<AuthenticationDto>(`${this.apiBaseUrl}/auth/login`, payload)
      .pipe(map((response) => this.storeSessionFromResponse(response)));
  }

  register(payload: RegisterRequest): Observable<AuthSession> {
    return this.http
      .post(`${this.apiBaseUrl}/auth/register`, payload)
      .pipe(switchMap(() => this.login(payload)));
  }

  logout(redirect = true): void {
    this.http
      .post(`${this.apiBaseUrl}/logout`, {})
      .pipe(
        catchError(() => of(null)),
        finalize(() => {
          this.clearSession();

          if (redirect) {
            void this.router.navigateByUrl('/');
          }
        })
      )
      .subscribe();
  }

  getAccessToken(): string | null {
    return this.sessionState()?.accessToken ?? null;
  }

  handleUnauthorized(): void {
    this.clearSession();
    void this.router.navigateByUrl('/');
  }

  private storeSessionFromResponse(response: AuthenticationDto): AuthSession {
    const session: AuthSession = {
      user: response.user,
      accessToken: response.accessToken.value,
      refreshToken: response.refreshToken.value
    };

    this.sessionState.set(session);
    localStorage.setItem(AUTH_STORAGE_KEY, JSON.stringify(session));
    return session;
  }

  private restoreSession(): AuthSession | null {
    const rawSession = localStorage.getItem(AUTH_STORAGE_KEY);

    if (!rawSession) {
      return null;
    }

    try {
      const parsed = JSON.parse(rawSession) as Partial<AuthSession>;

      if (
        !parsed.accessToken ||
        !parsed.refreshToken ||
        !parsed.user?.email ||
        typeof parsed.user.id !== 'number' ||
        typeof parsed.user.roleId !== 'number'
      ) {
        localStorage.removeItem(AUTH_STORAGE_KEY);
        return null;
      }

      return parsed as AuthSession;
    } catch {
      localStorage.removeItem(AUTH_STORAGE_KEY);
      return null;
    }
  }

  private clearSession(): void {
    this.sessionState.set(null);
    localStorage.removeItem(AUTH_STORAGE_KEY);
  }
}
