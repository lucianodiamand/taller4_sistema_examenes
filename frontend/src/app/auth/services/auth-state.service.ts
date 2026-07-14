import { Injectable, computed, inject, signal } from '@angular/core';

import {
  type AuthSession,
  type CurrentUserResponse,
  type TokenResponse,
  type UserRole,
} from '../contracts/auth.contracts';
import { SessionService } from './session.service';

@Injectable({ providedIn: 'root' })
export class AuthStateService {
  private readonly sessionService = inject(SessionService);
  private readonly sessionState = signal<AuthSession | null>(this.sessionService.read());

  readonly session = this.sessionState.asReadonly();
  readonly user = computed<CurrentUserResponse | null>(() => this.sessionState()?.user ?? null);
  readonly role = computed<UserRole | null>(() => this.user()?.role ?? null);
  readonly isAuthenticated = computed<boolean>(() => Boolean(this.sessionState()?.accessToken));

  setSession(session: AuthSession): void {
    this.sessionService.write(session);
    this.sessionState.set(session);
  }

  patchSessionUser(user: CurrentUserResponse): void {
    const current = this.sessionState();
    if (!current) {
      return;
    }

    this.setSession({ ...current, user });
  }

  setSessionFromTokens(tokens: TokenResponse): void {
    const currentUser = this.sessionState()?.user ?? null;
    this.setSession({
      accessToken: tokens.accessToken,
      refreshToken: tokens.refreshToken,
      accessExpiresAt: tokens.accessExpiresAt,
      refreshExpiresAt: tokens.refreshExpiresAt,
      user: currentUser,
    });
  }

  clear(): void {
    this.sessionService.clear();
    this.sessionState.set(null);
  }
}
