import { inject } from '@angular/core';
import { ActivatedRouteSnapshot, CanActivateFn, Router } from '@angular/router';

import { APP_ROUTES, type UserRole } from '../contracts/auth.contracts';
import { AuthService } from '../services/auth.service';

function resolveAllowedRoles(route: ActivatedRouteSnapshot): readonly UserRole[] {
  return (route.data['roles'] as readonly UserRole[] | undefined) ?? [];
}

export const roleGuard: CanActivateFn = (route) => {
  const authService = inject(AuthService);
  const router = inject(Router);
  const role = authService.role();
  const allowedRoles = resolveAllowedRoles(route);

  if (!role) {
    return router.parseUrl(APP_ROUTES.login);
  }

  if (allowedRoles.includes(role)) {
    return true;
  }

  return router.parseUrl(APP_ROUTES.forbidden);
};
