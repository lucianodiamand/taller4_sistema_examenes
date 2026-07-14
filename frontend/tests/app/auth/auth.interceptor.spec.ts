import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { vi } from 'vitest';

import {
  AUTH_API_ENDPOINTS,
  AUTH_BEARER_PREFIX,
  AUTH_HEADER_NAME,
  AUTH_RETRY_HEADER,
  HTTP_STATUS
} from '@app/auth/auth.constants';
import { authInterceptor } from '@app/auth/auth.interceptor';
import { SessionService } from '@app/auth/session.service';

const EXPIRED_TOKEN = 'expired-token';
const FRESH_TOKEN = 'fresh-token';
const PROTECTED_ENDPOINT = '/api/protected';

describe('authInterceptor', () => {
  const sessionServiceMock = {
    getAccessToken: vi.fn<() => string | null>(),
    refreshAccessToken: vi.fn<() => import('rxjs').Observable<boolean>>()
  };

  let http: HttpClient;
  let httpTesting: HttpTestingController;

  beforeEach(() => {
    vi.clearAllMocks();

    TestBed.configureTestingModule({
      providers: [
        { provide: SessionService, useValue: sessionServiceMock },
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting()
      ]
    });

    http = TestBed.inject(HttpClient);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTesting.verify();
  });

  it('adds bearer token to request', () => {
    sessionServiceMock.getAccessToken.mockReturnValue(FRESH_TOKEN);

    http.get(AUTH_API_ENDPOINTS.USER_ME).subscribe();

    const req = httpTesting.expectOne(AUTH_API_ENDPOINTS.USER_ME);
    expect(req.request.headers.get(AUTH_HEADER_NAME)).toBe(`${AUTH_BEARER_PREFIX}${FRESH_TOKEN}`);
    req.flush({});
  });

  it('refreshes and retries once on 401', () => {
    sessionServiceMock.getAccessToken
      .mockReturnValueOnce(EXPIRED_TOKEN)
      .mockReturnValueOnce(FRESH_TOKEN);
    sessionServiceMock.refreshAccessToken.mockReturnValue(of(true));

    http.get(PROTECTED_ENDPOINT).subscribe();

    const first = httpTesting.expectOne(PROTECTED_ENDPOINT);
    first.flush({}, { status: HTTP_STATUS.UNAUTHORIZED, statusText: 'Unauthorized' });

    const retried = httpTesting.expectOne(PROTECTED_ENDPOINT);
    expect(retried.request.headers.get(AUTH_HEADER_NAME)).toBe(`${AUTH_BEARER_PREFIX}${FRESH_TOKEN}`);
    expect(retried.request.headers.has(AUTH_RETRY_HEADER)).toBe(true);
    retried.flush({ ok: true });
  });
});
