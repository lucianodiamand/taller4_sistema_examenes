import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import {
  AUTH_ENDPOINTS,
  type CreatePermissionRequest,
  type PermissionResponse,
  type ReplaceRolePermissionsRequest,
  type RoleAdminResponse,
  type RolePermissionsResponse,
  type UpdatePermissionRequest,
  type UpdateRoleRequest,
  type UserRole,
} from '../contracts/auth.contracts';

@Injectable({ providedIn: 'root' })
export class AdminAccessApiService {
  private readonly http = inject(HttpClient);

  roles(): Observable<RoleAdminResponse[]> {
    return this.http.get<RoleAdminResponse[]>(AUTH_ENDPOINTS.roles);
  }

  updateRole(role: UserRole, payload: UpdateRoleRequest): Observable<RoleAdminResponse> {
    return this.http.patch<RoleAdminResponse>(`${AUTH_ENDPOINTS.roles}/${role}`, payload);
  }

  rolePermissions(role: UserRole): Observable<RolePermissionsResponse> {
    return this.http.get<RolePermissionsResponse>(`${AUTH_ENDPOINTS.roles}/${role}/permissions`);
  }

  replaceRolePermissions(role: UserRole, payload: ReplaceRolePermissionsRequest): Observable<RolePermissionsResponse> {
    return this.http.patch<RolePermissionsResponse>(`${AUTH_ENDPOINTS.roles}/${role}/permissions`, payload);
  }

  permissions(): Observable<PermissionResponse[]> {
    return this.http.get<PermissionResponse[]>(AUTH_ENDPOINTS.permissions);
  }

  createPermission(payload: CreatePermissionRequest): Observable<PermissionResponse> {
    return this.http.post<PermissionResponse>(AUTH_ENDPOINTS.permissions, payload);
  }

  updatePermission(id: number, payload: UpdatePermissionRequest): Observable<PermissionResponse> {
    return this.http.patch<PermissionResponse>(`${AUTH_ENDPOINTS.permissions}/${id}`, payload);
  }

  deletePermission(id: number): Observable<void> {
    return this.http.delete<void>(`${AUTH_ENDPOINTS.permissions}/${id}`);
  }
}
