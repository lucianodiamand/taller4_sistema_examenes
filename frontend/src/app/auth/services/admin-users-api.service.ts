import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import {
  AUTH_ENDPOINTS,
  type CreateUserAdminRequest,
  type UpdateUserAdminRequest,
  type UserAdminResponse,
} from '../contracts/auth.contracts';

@Injectable({ providedIn: 'root' })
export class AdminUsersApiService {
  private readonly http = inject(HttpClient);

  findAll(): Observable<UserAdminResponse[]> {
    return this.http.get<UserAdminResponse[]>(AUTH_ENDPOINTS.users);
  }

  create(payload: CreateUserAdminRequest): Observable<UserAdminResponse> {
    return this.http.post<UserAdminResponse>(AUTH_ENDPOINTS.users, payload);
  }

  update(id: number, payload: UpdateUserAdminRequest): Observable<UserAdminResponse> {
    return this.http.patch<UserAdminResponse>(`${AUTH_ENDPOINTS.users}/${id}`, payload);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${AUTH_ENDPOINTS.users}/${id}`);
  }
}
