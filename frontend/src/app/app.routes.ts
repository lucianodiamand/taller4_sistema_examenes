import { Routes } from '@angular/router';

import { APP_ROUTE_PATHS, UserRole } from './auth/contracts/auth.contracts';
import { authGuard } from './auth/guards/auth.guard';
import { publicOnlyGuard } from './auth/guards/public-only.guard';
import { roleGuard } from './auth/guards/role.guard';

export const routes: Routes = [
  {
    path: '',
    pathMatch: 'full',
    redirectTo: APP_ROUTE_PATHS.login,
  },
  {
    path: APP_ROUTE_PATHS.login,
    canActivate: [publicOnlyGuard],
    loadComponent: () => import('./auth/pages/login/login.component').then((m) => m.LoginComponent),
  },
  {
    path: APP_ROUTE_PATHS.register,
    canActivate: [publicOnlyGuard],
    loadComponent: () => import('./auth/pages/register/register.component').then((m) => m.RegisterComponent),
  },
  {
    path: APP_ROUTE_PATHS.forbidden,
    canActivate: [authGuard],
    loadComponent: () => import('./auth/pages/forbidden/forbidden.component').then((m) => m.ForbiddenComponent),
  },
  {
    path: APP_ROUTE_PATHS.app,
    canActivate: [authGuard],
    children: [
      {
        path: '',
        pathMatch: 'full',
        loadComponent: () =>
          import('./auth/pages/role-redirect/role-redirect.component').then((m) => m.RoleRedirectComponent),
      },
      {
        path: APP_ROUTE_PATHS.profile,
        canActivate: [roleGuard],
        data: {
          roles: [UserRole.ADMIN, UserRole.PROFESSOR, UserRole.STUDENT],
          title: 'Mi perfil',
        },
        loadComponent: () => import('./auth/pages/profile/profile.component').then((m) => m.ProfileComponent),
      },
      {
        path: APP_ROUTE_PATHS.professor,
        canActivate: [roleGuard],
        data: {
          roles: [UserRole.PROFESSOR],
          title: 'Inicio profesor',
        },
        children: [
          {
            path: '',
            pathMatch: 'full',
            loadComponent: () =>
              import('./auth/pages/role-home/role-home.component').then((m) => m.RoleHomeComponent),
          },
          {
            path: APP_ROUTE_PATHS.professorExams,
            loadComponent: () =>
              import('./professor/professor-exams.component').then((m) => m.ProfessorExamsComponent),
          },
          {
            path: APP_ROUTE_PATHS.professorGrading,
           
            // GradingController solo autoriza hasRole('PROFESSOR'), ADMIN queda afuera
            // a diferencia del resto de las rutas de /professor.
            data: { roles: [UserRole.PROFESSOR], title: 'Corregir exámenes' },
            loadComponent: () => import('./professor/grading.component').then((m) => m.GradingComponent),
          },
        ],
      },
      {
        path: APP_ROUTE_PATHS.student,
        canActivate: [roleGuard],
        data: {
          roles: [UserRole.STUDENT, UserRole.ADMIN],
          title: 'Inicio estudiante',
        },
        loadComponent: () => import('./student/student-exams.component').then((m) => m.StudentExamsComponent),
      },
      {
        path: APP_ROUTE_PATHS.admin,
        canActivate: [roleGuard],
        data: {
          roles: [UserRole.ADMIN],
          title: 'Inicio administrador',
        },
        children: [
          {
            path: '',
            pathMatch: 'full',
            loadComponent: () => import('./auth/pages/role-home/role-home.component').then((m) => m.RoleHomeComponent),
          },
          {
            path: APP_ROUTE_PATHS.adminUsers,
            loadComponent: () => import('./auth/pages/admin-users/admin-users.component').then((m) => m.AdminUsersComponent),
          },
          {
            path: APP_ROUTE_PATHS.adminAccess,
            loadComponent: () =>
              import('./auth/pages/admin-access/admin-access.component').then((m) => m.AdminAccessComponent),
          },
        ],
      },
    ],
  },
  {
    path: '**',
    redirectTo: APP_ROUTE_PATHS.login,
  },
];
