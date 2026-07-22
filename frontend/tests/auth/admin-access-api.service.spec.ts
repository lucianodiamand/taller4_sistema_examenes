import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';

import { AUTH_ENDPOINTS, UserRole } from '@/app/auth/contracts/auth.contracts';
import { AdminAccessApiService } from '@/app/auth/services/admin-access-api.service';

describe('AdminAccessApiService', () => {
  let service: AdminAccessApiService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), AdminAccessApiService],
    });

    service = TestBed.inject(AdminAccessApiService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('loads role permissions', () => {
    service.rolePermissions(UserRole.ADMIN).subscribe((response) => {
      expect(response.role).toBe(UserRole.ADMIN);
      expect(response.permissionCodes).toContain('permissions.manage');
    });

    const req = httpMock.expectOne(`${AUTH_ENDPOINTS.roles}/${UserRole.ADMIN}/permissions`);
    expect(req.request.method).toBe('GET');
    req.flush({ role: UserRole.ADMIN, permissionCodes: ['permissions.manage'] });
  });
});
