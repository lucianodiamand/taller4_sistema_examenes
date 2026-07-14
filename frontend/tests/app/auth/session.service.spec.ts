import { TestBed } from '@angular/core/testing';
import { firstValueFrom, of } from 'rxjs';
import { vi } from 'vitest';

import { AUTH_STORAGE_KEY, UNIX_TIME_MS_FACTOR, UserRole } from '@app/auth/auth.constants';
import { AuthApiService } from '@app/auth/auth-api.service';
import { SessionService } from '@app/auth/session.service';

const ACCESS_TOKEN = 'access-token';
const REFRESH_TOKEN = 'refresh-token';
const TEST_USERNAME = 'test';
const TEST_PASSWORD = 'secret';
const TEST_NAME = 'Test';
const ACCESS_TTL_SECONDS = 60;
const REFRESH_TTL_SECONDS = 300;
const EXPIRED_ACCESS_OFFSET_SECONDS = 10;
const EXPIRED_REFRESH_OFFSET_SECONDS = 1;
const EXPIRED_ACCESS_TOKEN = 'expired-access';
const EXPIRED_REFRESH_TOKEN = 'expired-refresh';

describe('SessionService', () => {
  const authApiMock = {
    login: vi.fn(),
    register: vi.fn(),
    refresh: vi.fn(),
    logout: vi.fn(),
    me: vi.fn()
  };

  beforeEach(() => {
    localStorage.clear();
    vi.clearAllMocks();

    TestBed.configureTestingModule({
      providers: [SessionService, { provide: AuthApiService, useValue: authApiMock }]
    });
  });

  it('stores session and user on login', async () => {
    const service = TestBed.inject(SessionService);
    const now = Math.floor(Date.now() / UNIX_TIME_MS_FACTOR);

    authApiMock.login.mockReturnValue(
      of({
        accessToken: ACCESS_TOKEN,
        refreshToken: REFRESH_TOKEN,
        tokenType: 'Bearer',
        accessExpiresAt: now + ACCESS_TTL_SECONDS,
        refreshExpiresAt: now + REFRESH_TTL_SECONDS
      })
    );
    authApiMock.me.mockReturnValue(
      of({
        id: 1,
        name: TEST_NAME,
        username: TEST_USERNAME,
        role: UserRole.STUDENT
      })
    );

    await firstValueFrom(
      service.login({
        username: TEST_USERNAME,
        password: TEST_PASSWORD
      })
    );

    const stored = JSON.parse(localStorage.getItem(AUTH_STORAGE_KEY) ?? '{}');
    expect(stored.accessToken).toBe(ACCESS_TOKEN);
    expect(service.isAuthenticated()).toBe(true);
    expect(service.role()).toBe(UserRole.STUDENT);
  });

  it('clears expired refresh token during bootstrap', async () => {
    const service = TestBed.inject(SessionService);
    const now = Math.floor(Date.now() / UNIX_TIME_MS_FACTOR);

    localStorage.setItem(
      AUTH_STORAGE_KEY,
      JSON.stringify({
        accessToken: EXPIRED_ACCESS_TOKEN,
        refreshToken: EXPIRED_REFRESH_TOKEN,
        accessExpiresAt: now - EXPIRED_ACCESS_OFFSET_SECONDS,
        refreshExpiresAt: now - EXPIRED_REFRESH_OFFSET_SECONDS
      })
    );

    await firstValueFrom(service.bootstrap());

    expect(localStorage.getItem(AUTH_STORAGE_KEY)).toBeNull();
    expect(service.isAuthenticated()).toBe(false);
  });
});
