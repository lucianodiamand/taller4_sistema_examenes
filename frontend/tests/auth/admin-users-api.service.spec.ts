import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';

import { AUTH_ENDPOINTS, UserRole } from '@/app/auth/contracts/auth.contracts';
import { AdminUsersApiService } from '@/app/auth/services/admin-users-api.service';

describe('AdminUsersApiService', () => {
  let service: AdminUsersApiService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), AdminUsersApiService],
    });

    service = TestBed.inject(AdminUsersApiService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('gets users list', () => {
    service.findAll().subscribe((users) => {
      expect(users.length).toBe(1);
      expect(users[0]?.username).toBe('profe');
    });

    const req = httpMock.expectOne(AUTH_ENDPOINTS.users);
    expect(req.request.method).toBe('GET');
    req.flush([
      {
        id: 10,
        name: 'Profesor Uno',
        username: 'profe',
        role: UserRole.PROFESSOR,
        self: false,
        manageable: true,
      },
    ]);
  });
});
