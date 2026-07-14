import { Routes } from '@angular/router';

import { AUTH_ROUTE_DATA_KEYS, AUTH_ROUTE_PATHS, UserRole } from './auth/auth.constants';
import { authGuard, guestOnlyGuard, roleGuard } from './auth/auth.guards';
import { ForbiddenPage } from './pages/forbidden/forbidden.page';
import { HomePage } from './pages/home/home.page';
import { LoginPage } from './pages/login/login.page';
import { RegisterPage } from './pages/register/register.page';

export const routes: Routes = [
  {
    path: AUTH_ROUTE_PATHS.ROOT,
    pathMatch: 'full',
    redirectTo: AUTH_ROUTE_PATHS.APP
  },
  {
    path: AUTH_ROUTE_PATHS.LOGIN,
    canActivate: [guestOnlyGuard],
    component: LoginPage
  },
  {
    path: AUTH_ROUTE_PATHS.REGISTER,
    canActivate: [guestOnlyGuard],
    component: RegisterPage
  },
  {
    path: AUTH_ROUTE_PATHS.FORBIDDEN,
    canActivate: [authGuard],
    component: ForbiddenPage
  },
  {
    path: AUTH_ROUTE_PATHS.APP,
    canActivate: [authGuard],
    component: HomePage
  },
  {
    path: AUTH_ROUTE_PATHS.APP_ADMIN,
    canActivate: [authGuard, roleGuard],
    data: { [AUTH_ROUTE_DATA_KEYS.ROLES]: [UserRole.ADMIN] },
    component: HomePage
  },
  {
    path: AUTH_ROUTE_PATHS.APP_PROFESSOR,
    canActivate: [authGuard, roleGuard],
    data: { [AUTH_ROUTE_DATA_KEYS.ROLES]: [UserRole.PROFESSOR, UserRole.ADMIN] },
    component: HomePage
  },
  {
    path: AUTH_ROUTE_PATHS.APP_STUDENT,
    canActivate: [authGuard, roleGuard],
    data: { [AUTH_ROUTE_DATA_KEYS.ROLES]: [UserRole.STUDENT, UserRole.ADMIN] },
    component: HomePage
  },
  {
    path: AUTH_ROUTE_PATHS.WILDCARD,
    redirectTo: AUTH_ROUTE_PATHS.APP
  }
];
