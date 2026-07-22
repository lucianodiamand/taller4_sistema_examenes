import { ROLE_HOME_ROUTE, UserRole } from '@/app/auth/contracts/auth.contracts';

describe('auth role contracts', () => {
  it('maps every role to home route', () => {
    expect(ROLE_HOME_ROUTE[UserRole.ADMIN]).toContain('/app/admin');
    expect(ROLE_HOME_ROUTE[UserRole.PROFESSOR]).toContain('/app/professor');
    expect(ROLE_HOME_ROUTE[UserRole.STUDENT]).toContain('/app/student');
  });
});
