import { AUTH_STORAGE_KEY, type AuthSession, UserRole } from '@/app/auth/contracts/auth.contracts';
import { SessionService } from '@/app/auth/services/session.service';

describe('SessionService', () => {
  let service: SessionService;

  beforeEach(() => {
    localStorage.clear();
    service = new SessionService();
  });

  it('writes and reads auth session', () => {
    const session: AuthSession = {
      accessToken: 'a',
      refreshToken: 'r',
      accessExpiresAt: 1,
      refreshExpiresAt: 2,
      user: {
        id: 1,
        name: 'Name',
        username: 'user',
        role: UserRole.STUDENT,
      },
    };

    service.write(session);

    expect(service.read()).toEqual(session);
  });

  it('returns null and clears bad json', () => {
    localStorage.setItem(AUTH_STORAGE_KEY, '{bad-json');

    expect(service.read()).toBeNull();
    expect(localStorage.getItem(AUTH_STORAGE_KEY)).toBeNull();
  });
});
