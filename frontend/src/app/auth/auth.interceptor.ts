import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, switchMap, throwError } from 'rxjs';

import {
  AUTH_API_ENDPOINTS,
  AUTH_API_PREFIX,
  AUTH_BEARER_PREFIX,
  AUTH_HEADER_NAME,
  AUTH_RETRY_HEADER,
  AUTH_RETRY_MARK,
  HTTP_STATUS
} from './auth.constants';
import { SessionService } from './session.service';

export const authInterceptor: HttpInterceptorFn = (request, next) => {
  const sessionService = inject(SessionService);

  const accessToken = sessionService.getAccessToken();
  const authorizedRequest = accessToken ? addAuthorization(request, accessToken) : request;

  return next(authorizedRequest).pipe(
    catchError((error) => {
      if (!(error instanceof HttpErrorResponse) || error.status !== HTTP_STATUS.UNAUTHORIZED) {
        return throwError(() => error);
      }

      if (request.headers.has(AUTH_RETRY_HEADER) || isAuthRefreshRequest(request.url)) {
        return throwError(() => error);
      }

      return sessionService.refreshAccessToken().pipe(
        switchMap((refreshed) => {
          if (!refreshed) {
            return throwError(() => error);
          }

          const nextToken = sessionService.getAccessToken();
          if (!nextToken) {
            return throwError(() => error);
          }

          const retriedRequest = request.clone({
            setHeaders: {
              [AUTH_HEADER_NAME]: `${AUTH_BEARER_PREFIX}${nextToken}`,
              [AUTH_RETRY_HEADER]: AUTH_RETRY_MARK
            }
          });

          return next(retriedRequest);
        })
      );
    })
  );
};

function addAuthorization(request: Parameters<HttpInterceptorFn>[0], token: string) {
  return request.clone({
    setHeaders: {
      [AUTH_HEADER_NAME]: `${AUTH_BEARER_PREFIX}${token}`
    }
  });
}

function isAuthRefreshRequest(url: string): boolean {
  return url.includes(`${AUTH_API_PREFIX}refresh`) || url === AUTH_API_ENDPOINTS.REFRESH;
}
