import { computed, Injectable, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { catchError, finalize, map, Observable, of, shareReplay, switchMap, tap, throwError } from 'rxjs';

import {
  AUTH_ROUTE_URLS,
  AUTH_STORAGE_KEY,
  HTTP_STATUS,
  ROLE_HOME_URL,
  UNIX_TIME_MS_FACTOR,
  UserRole
} from './auth.constants';
import { AuthApiService } from './auth-api.service';
import { CurrentUser, LoginRequest, RegisterRequest, StoredSession, TokenResponse } from './auth.models';

@Injectable({ providedIn: 'root' })
export class SessionService {
  private readonly sessionState = signal<StoredSession | null>(null);
  readonly user = signal<CurrentUser | null>(null);

  readonly isAuthenticated = computed(() => this.sessionState() !== null && this.user() !== null);
  readonly role = computed<UserRole | null>(() => this.user()?.role ?? null);

  private refreshInFlight$: Observable<boolean> | null = null;

  constructor(private readonly authApiService: AuthApiService) {}

  bootstrap(): Observable<boolean> {
    const storedSession = this.readStoredSession();
    if (!storedSession) {
      this.clearInMemory();
      return of(true);
    }

    if (this.isExpired(storedSession.refreshExpiresAt)) {
      this.clearSession();
      return of(true);
    }

    this.sessionState.set(storedSession);

    if (this.isExpired(storedSession.accessExpiresAt)) {
      return this.refreshAccessToken().pipe(map(() => true));
    }

    return this.loadCurrentUser().pipe(
      map(() => true),
      catchError((error) => {
        if (error instanceof HttpErrorResponse && error.status === HTTP_STATUS.UNAUTHORIZED) {
          return this.refreshAccessToken().pipe(map(() => true));
        }

        this.clearSession();
        return of(true);
      })
    );
  }

  login(request: LoginRequest): Observable<void> {
    return this.authApiService.login(request).pipe(
      switchMap((tokenResponse) => this.applyTokenAndLoadUser(tokenResponse)),
      map(() => undefined)
    );
  }

  register(request: RegisterRequest): Observable<void> {
    return this.authApiService.register(request);
  }

  logout(): Observable<void> {
    if (!this.getAccessToken()) {
      this.clearSession();
      return of(undefined);
    }

    return this.authApiService.logout().pipe(
      catchError(() => of(undefined)),
      tap(() => this.clearSession())
    );
  }

  refreshAccessToken(): Observable<boolean> {
    if (this.refreshInFlight$) {
      return this.refreshInFlight$;
    }

    const currentSession = this.sessionState();
    if (!currentSession || this.isExpired(currentSession.refreshExpiresAt)) {
      this.clearSession();
      return of(false);
    }

    const request$ = this.authApiService.refresh({ refreshToken: currentSession.refreshToken }).pipe(
      tap((response) => this.storeSession(response)),
      switchMap(() => this.loadCurrentUser()),
      map(() => true),
      catchError(() => {
        this.clearSession();
        return of(false);
      }),
      finalize(() => {
        this.refreshInFlight$ = null;
      }),
      shareReplay(1)
    );

    this.refreshInFlight$ = request$;
    return request$;
  }

  getAccessToken(): string | null {
    return this.sessionState()?.accessToken ?? null;
  }

  hasRole(roles: readonly UserRole[]): boolean {
    const currentRole = this.role();
    if (!currentRole) {
      return false;
    }

    return roles.includes(currentRole);
  }

  homeUrlByRole(): string {
    const role = this.role();
    return role ? ROLE_HOME_URL[role] : AUTH_ROUTE_URLS.APP;
  }

  private applyTokenAndLoadUser(tokenResponse: TokenResponse): Observable<CurrentUser> {
    this.storeSession(tokenResponse);

    return this.loadCurrentUser().pipe(
      catchError((error) => {
        this.clearSession();
        return throwError(() => error);
      })
    );
  }

  private loadCurrentUser(): Observable<CurrentUser> {
    return this.authApiService.me().pipe(tap((user) => this.user.set(user)));
  }

  private storeSession(tokenResponse: TokenResponse): void {
    const session: StoredSession = {
      accessToken: tokenResponse.accessToken,
      refreshToken: tokenResponse.refreshToken,
      accessExpiresAt: tokenResponse.accessExpiresAt,
      refreshExpiresAt: tokenResponse.refreshExpiresAt
    };

    this.sessionState.set(session);
    localStorage.setItem(AUTH_STORAGE_KEY, JSON.stringify(session));
  }

  private readStoredSession(): StoredSession | null {
    const raw = localStorage.getItem(AUTH_STORAGE_KEY);
    if (!raw) {
      return null;
    }

    try {
      const parsed = JSON.parse(raw) as Partial<StoredSession>;
      if (
        typeof parsed.accessToken !== 'string' ||
        typeof parsed.refreshToken !== 'string' ||
        typeof parsed.accessExpiresAt !== 'number' ||
        typeof parsed.refreshExpiresAt !== 'number'
      ) {
        return null;
      }

      return {
        accessToken: parsed.accessToken,
        refreshToken: parsed.refreshToken,
        accessExpiresAt: parsed.accessExpiresAt,
        refreshExpiresAt: parsed.refreshExpiresAt
      };
    } catch {
      return null;
    }
  }

  private clearInMemory(): void {
    this.sessionState.set(null);
    this.user.set(null);
  }

  private clearSession(): void {
    this.clearInMemory();
    localStorage.removeItem(AUTH_STORAGE_KEY);
  }

  private isExpired(expiresAt: number): boolean {
    return expiresAt <= Math.floor(Date.now() / UNIX_TIME_MS_FACTOR);
  }
}
