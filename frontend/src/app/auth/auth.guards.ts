import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';

import { AUTH_ROUTE_DATA_KEYS, AUTH_ROUTE_URLS, UserRole } from './auth.constants';
import { SessionService } from './session.service';

export const authGuard: CanActivateFn = () => {
  const sessionService = inject(SessionService);
  const router = inject(Router);

  if (sessionService.isAuthenticated()) {
    return true;
  }

  return router.createUrlTree([AUTH_ROUTE_URLS.LOGIN]);
};

export const roleGuard: CanActivateFn = (route) => {
  const sessionService = inject(SessionService);
  const router = inject(Router);

  const expectedRoles = (route.data?.[AUTH_ROUTE_DATA_KEYS.ROLES] as UserRole[] | undefined) ?? [];
  if (expectedRoles.length === 0 || sessionService.hasRole(expectedRoles)) {
    return true;
  }

  if (!sessionService.isAuthenticated()) {
    return router.createUrlTree([AUTH_ROUTE_URLS.LOGIN]);
  }

  return router.createUrlTree([AUTH_ROUTE_URLS.FORBIDDEN]);
};

export const guestOnlyGuard: CanActivateFn = () => {
  const sessionService = inject(SessionService);
  const router = inject(Router);

  if (!sessionService.isAuthenticated()) {
    return true;
  }

  return router.createUrlTree([sessionService.homeUrlByRole()]);
};
