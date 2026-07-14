import { Injectable } from '@angular/core';

import { AUTH_STORAGE_KEY, type AuthSession } from '../contracts/auth.contracts';

@Injectable({ providedIn: 'root' })
export class SessionService {
  read(): AuthSession | null {
    const raw = this.storage().getItem(AUTH_STORAGE_KEY);
    if (!raw) {
      return null;
    }

    try {
      return JSON.parse(raw) as AuthSession;
    } catch {
      this.clear();
      return null;
    }
  }

  write(session: AuthSession): void {
    this.storage().setItem(AUTH_STORAGE_KEY, JSON.stringify(session));
  }

  clear(): void {
    this.storage().removeItem(AUTH_STORAGE_KEY);
  }

  private storage(): Storage {
    return localStorage;
  }
}
