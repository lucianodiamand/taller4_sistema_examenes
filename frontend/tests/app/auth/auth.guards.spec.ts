import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { vi } from 'vitest';

import { AUTH_ROUTE_DATA_KEYS, AUTH_ROUTE_URLS, UserRole } from '@app/auth/auth.constants';
import { authGuard, guestOnlyGuard, roleGuard } from '@app/auth/auth.guards';
import { SessionService } from '@app/auth/session.service';

const EMPTY_ROUTE = {} as never;
const EMPTY_STATE = {} as never;
const ADMIN_ROUTE = { data: { [AUTH_ROUTE_DATA_KEYS.ROLES]: [UserRole.ADMIN] } } as never;

describe('auth guards', () => {
  const sessionServiceMock = {
    isAuthenticated: vi.fn<() => boolean>(),
    hasRole: vi.fn<(roles: readonly UserRole[]) => boolean>(),
    homeUrlByRole: vi.fn<() => string>()
  };

  const routerMock = {
    createUrlTree: vi.fn((commands: unknown[]) => commands)
  };

  beforeEach(() => {
    vi.clearAllMocks();

    TestBed.configureTestingModule({
      providers: [
        { provide: SessionService, useValue: sessionServiceMock },
        { provide: Router, useValue: routerMock }
      ]
    });
  });

  it('authGuard redirects anonymous user to login', () => {
    sessionServiceMock.isAuthenticated.mockReturnValue(false);

    const result = TestBed.runInInjectionContext(() => authGuard(EMPTY_ROUTE, EMPTY_STATE));

    expect(result).toEqual([AUTH_ROUTE_URLS.LOGIN]);
  });

  it('roleGuard redirects forbidden user', () => {
    sessionServiceMock.isAuthenticated.mockReturnValue(true);
    sessionServiceMock.hasRole.mockReturnValue(false);

    const result = TestBed.runInInjectionContext(() => roleGuard(ADMIN_ROUTE, EMPTY_STATE));

    expect(result).toEqual([AUTH_ROUTE_URLS.FORBIDDEN]);
  });

  it('guestOnlyGuard redirects authenticated user to role home', () => {
    sessionServiceMock.isAuthenticated.mockReturnValue(true);
    sessionServiceMock.homeUrlByRole.mockReturnValue(AUTH_ROUTE_URLS.APP_STUDENT);

    const result = TestBed.runInInjectionContext(() => guestOnlyGuard(EMPTY_ROUTE, EMPTY_STATE));

    expect(result).toEqual([AUTH_ROUTE_URLS.APP_STUDENT]);
  });
});
