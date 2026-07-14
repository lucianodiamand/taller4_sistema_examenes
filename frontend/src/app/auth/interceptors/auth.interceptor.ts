import {
  HttpErrorResponse,
  HttpInterceptorFn,
  HttpRequest,
} from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { Observable, catchError, switchMap, throwError } from 'rxjs';

import { APP_ROUTES, AUTH_ENDPOINTS, AUTH_HEADER, AUTH_SCHEME, HTTP_STATUS } from '../contracts/auth.contracts';
import { AuthService } from '../services/auth.service';

const RETRY_HEADER = 'x-auth-refresh-retried';

export const authInterceptor: HttpInterceptorFn = (request, next) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  const authRequest = attachToken(request, authService);
  return next(authRequest).pipe(
    catchError((error: HttpErrorResponse) => {
      if (!needsRefresh(authRequest, error)) {
        return throwError(() => error);
      }

      return authService.refresh().pipe(
        switchMap((ok) => {
          if (!ok) {
            void router.navigateByUrl(APP_ROUTES.login);
            return throwError(() => error);
          }

          const retryRequest = attachToken(
            authRequest.clone({ headers: authRequest.headers.set(RETRY_HEADER, 'true') }),
            authService
          );

          return next(retryRequest);
        }),
        catchError((refreshError) => {
          if (authService.shouldForceLogout((refreshError as HttpErrorResponse).status)) {
            authService.clearSession();
            void router.navigateByUrl(APP_ROUTES.login);
          }
          return throwError(() => refreshError);
        })
      );
    })
  );
};

function attachToken(request: HttpRequest<unknown>, authService: AuthService): HttpRequest<unknown> {
  const token = authService.currentAccessToken();
  if (!token) {
    return request;
  }

  return request.clone({
    headers: request.headers.set(AUTH_HEADER, `${AUTH_SCHEME} ${token}`),
  });
}

function needsRefresh(request: HttpRequest<unknown>, error: HttpErrorResponse): boolean {
  if (error.status !== HTTP_STATUS.UNAUTHORIZED) {
    return false;
  }

  if (request.headers.has(RETRY_HEADER)) {
    return false;
  }

  if (request.url.endsWith(AUTH_ENDPOINTS.login) || request.url.endsWith(AUTH_ENDPOINTS.register)) {
    return false;
  }

  return !request.url.endsWith(AUTH_ENDPOINTS.refresh);
}
