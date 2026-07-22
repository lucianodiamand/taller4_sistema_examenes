import { Injectable, inject } from '@angular/core';
import { Router } from '@angular/router';
import { firstValueFrom } from 'rxjs';

import { APP_ROUTES } from '../contracts/auth.contracts';
import { AuthApiService } from './auth-api.service';
import { AuthStateService } from './auth-state.service';
import { AuthService } from './auth.service';

@Injectable({ providedIn: 'root' })
export class AuthBootstrapService {
  private readonly authService = inject(AuthService);
  private readonly authApi = inject(AuthApiService);
  private readonly authState = inject(AuthStateService);
  private readonly router = inject(Router);

  async initialize(): Promise<void> {
    const session = this.authService.currentSession();
    if (!session) {
      return;
    }

    if (this.authService.isRefreshExpired()) {
      this.authService.clearSession();
      return;
    }

    if (this.authService.isAccessExpired()) {
      const refreshed = await firstValueFrom(this.authService.refresh());
      if (!refreshed) {
        await this.router.navigateByUrl(APP_ROUTES.login);
        return;
      }
    }

    try {
      const user = await firstValueFrom(this.authApi.me());
      this.authState.patchSessionUser(user);
    } catch {
      this.authService.clearSession();
      await this.router.navigateByUrl(APP_ROUTES.login);
    }
  }
}
