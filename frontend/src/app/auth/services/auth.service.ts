import { Injectable, inject } from '@angular/core';
import { Observable, catchError, map, of, switchMap, tap } from 'rxjs';

import {
  APP_ROUTES,
  HTTP_STATUS,
  ROLE_HOME_ROUTE,
  type ApiErrorResponse,
  type AuthSession,
  type LoginRequest,
  type RegisterRequest,
  type UserRole,
} from '../contracts/auth.contracts';
import { AuthApiService } from './auth-api.service';
import { AuthStateService } from './auth-state.service';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly api = inject(AuthApiService);
  private readonly authState = inject(AuthStateService);

  register(payload: RegisterRequest): Observable<void> {
    return this.api.register(payload);
  }

  login(payload: LoginRequest): Observable<void> {
    return this.api.login(payload).pipe(
      tap((tokens) => this.authState.setSessionFromTokens(tokens)),
      switchMap(() => this.api.me()),
      tap((user) => this.authState.patchSessionUser(user)),
      map(() => void 0)
    );
  }

  refresh(): Observable<boolean> {
    const session = this.authState.session();
    if (!session?.refreshToken) {
      return of(false);
    }

    return this.api.refresh({ refreshToken: session.refreshToken }).pipe(
      tap((tokens) => this.authState.setSessionFromTokens(tokens)),
      map(() => true),
      catchError(() => {
        this.clearSession();
        return of(false);
      })
    );
  }

  logout(): Observable<void> {
    return this.api.logout().pipe(
      catchError(() => of(void 0)),
      tap(() => this.clearSession())
    );
  }

  clearSession(): void {
    this.authState.clear();
  }

  currentSession(): AuthSession | null {
    return this.authState.session();
  }

  currentAccessToken(): string | null {
    return this.authState.session()?.accessToken ?? null;
  }

  isAccessExpired(): boolean {
    const value = this.authState.session()?.accessExpiresAt;
    if (!value) {
      return true;
    }
    return value <= this.unixNow();
  }

  isRefreshExpired(): boolean {
    const value = this.authState.session()?.refreshExpiresAt;
    if (!value) {
      return true;
    }
    return value <= this.unixNow();
  }

  isAuthenticated(): boolean {
    return this.authState.isAuthenticated();
  }

  role(): UserRole | null {
    return this.authState.role();
  }

  roleHome(role: UserRole | null): string {
    return role ? ROLE_HOME_ROUTE[role] : APP_ROUTES.login;
  }

  shouldForceLogout(status: number): boolean {
    return status === HTTP_STATUS.BAD_REQUEST || status === HTTP_STATUS.UNAUTHORIZED;
  }

  errorMessage(error: unknown, fallback = 'Unexpected error'): string {
    const body = (error as { error?: ApiErrorResponse })?.error;
    return body?.message ?? fallback;
  }

  private unixNow(): number {
    return Math.floor(Date.now() / 1000);
  }
}
