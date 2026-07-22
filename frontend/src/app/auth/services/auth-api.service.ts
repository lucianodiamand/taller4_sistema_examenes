import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import {
  AUTH_ENDPOINTS,
  type CurrentUserResponse,
  type LoginRequest,
  type RefreshRequest,
  type RegisterRequest,
  type TokenResponse,
  type UpdateMeRequest,
} from '../contracts/auth.contracts';

@Injectable({ providedIn: 'root' })
export class AuthApiService {
  private readonly http = inject(HttpClient);

  register(payload: RegisterRequest): Observable<void> {
    return this.http.post<void>(AUTH_ENDPOINTS.register, payload);
  }

  login(payload: LoginRequest): Observable<TokenResponse> {
    return this.http.post<TokenResponse>(AUTH_ENDPOINTS.login, payload);
  }

  refresh(payload: RefreshRequest): Observable<TokenResponse> {
    return this.http.post<TokenResponse>(AUTH_ENDPOINTS.refresh, payload);
  }

  logout(): Observable<void> {
    return this.http.post<void>(AUTH_ENDPOINTS.logout, {});
  }

  logoutAll(): Observable<void> {
    return this.http.post<void>(AUTH_ENDPOINTS.logoutAll, {});
  }

  me(): Observable<CurrentUserResponse> {
    return this.http.get<CurrentUserResponse>(AUTH_ENDPOINTS.me);
  }

  updateMe(payload: UpdateMeRequest): Observable<CurrentUserResponse> {
    return this.http.patch<CurrentUserResponse>(AUTH_ENDPOINTS.me, payload);
  }
}
