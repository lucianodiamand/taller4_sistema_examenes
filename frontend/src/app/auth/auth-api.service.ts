import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { AUTH_API_ENDPOINTS } from './auth.constants';
import {
  CurrentUser,
  LoginRequest,
  RefreshRequest,
  RegisterRequest,
  TokenResponse
} from './auth.models';

@Injectable({ providedIn: 'root' })
export class AuthApiService {
  constructor(private readonly http: HttpClient) {}

  login(request: LoginRequest): Observable<TokenResponse> {
    return this.http.post<TokenResponse>(AUTH_API_ENDPOINTS.LOGIN, request);
  }

  register(request: RegisterRequest): Observable<void> {
    return this.http.post<void>(AUTH_API_ENDPOINTS.REGISTER, request);
  }

  refresh(request: RefreshRequest): Observable<TokenResponse> {
    return this.http.post<TokenResponse>(AUTH_API_ENDPOINTS.REFRESH, request);
  }

  logout(): Observable<void> {
    return this.http.post<void>(AUTH_API_ENDPOINTS.LOGOUT, null);
  }

  me(): Observable<CurrentUser> {
    return this.http.get<CurrentUser>(AUTH_API_ENDPOINTS.USER_ME);
  }
}
