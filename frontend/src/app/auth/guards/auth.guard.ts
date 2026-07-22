import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';

import { APP_ROUTES } from '../contracts/auth.contracts';
import { AuthService } from '../services/auth.service';

export const authGuard: CanActivateFn = () => {
  const authService = inject(AuthService);
  const router = inject(Router);

  if (authService.isAuthenticated()) {
    return true;
  }

  return router.parseUrl(APP_ROUTES.login);
};
