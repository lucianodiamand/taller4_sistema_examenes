# Backend Auth + JWT + RBAC Specification

## Overview
Integrate authentication, authorization, and permissions into the REST API using JWT and DB-backed token/session management.
The design must be extensible (permissions are data-driven, not hardcoded to three roles).

## Goals
- Authenticate users with JWT access/refresh tokens.
- Authorize requests by permission.
- Support roles: `ADMIN`, `PROFESSOR`, `STUDENT`.
- Allow admin to modify role permissions.
- Enforce: only students can self-register.
- Enforce: professors are created by admin.
- Persist token management in DB (`access_token`, `refresh_token`), with revocation/rotation support.
- Include enough JWT payload data to validate identity, role context, and token session.

## Actors
- **Anonymous user**: can register (student only) and login.
- **Student**: solve exams, see professor validations, see own exam results, edit own profile.
- **Professor**: create exams, check/grade exams, edit own profile.
- **Admin**: modify permissions, create exams, solve exams, check/grade exams, view all user profiles, create professor users.

## Permission Model (Extensible)
Authorization is permission-based. Roles are collections of permissions.

### Permission catalog (initial seed)
- `permissions.manage`
- `users.read.any`
- `users.read.self`
- `users.update.self`
- `users.create.professor`
- `exams.create`
- `exams.solve`
- `exams.grade`
- `exam.validations.read.self`
- `exam.results.read.self`

### Default role mapping
- **ADMIN**
  - `permissions.manage`
  - `users.read.any`
  - `users.read.self`
  - `users.update.self`
  - `users.create.professor`
  - `exams.create`
  - `exams.solve`
  - `exams.grade`
- **PROFESSOR**
  - `users.read.self`
  - `users.update.self`
  - `exams.create`
  - `exams.grade`
- **STUDENT**
  - `users.read.self`
  - `users.update.self`
  - `exams.solve`
  - `exam.validations.read.self`
  - `exam.results.read.self`

## JWT and Token Session Model

### Access token
Short-lived JWT used on API calls.

### Refresh token
Long-lived JWT used only to obtain a new token pair.

### JWT payload (minimum required)
```json
{
  "sub": "user-id",
  "username": "jdoe",
  "role": "STUDENT",
  "sid": "session-uuid",
  "jti": "token-uuid",
  "type": "access",
  "iat": 1710000000,
  "exp": 1710000900
}
```

Notes:
- `sub`, `sid`, and `jti` are required for DB token validation/revocation.
- `role` is included for role context and audit.
- Permissions are resolved from DB (extensible + immediate admin changes), not permanently embedded in JWT.

## Database Changes

### Existing tables reused
- `users`
- `roles`

### New tables
1. `permissions`
   - `id` (PK)
   - `code` (unique, not null)
   - `description`

2. `role_permissions`
   - `role_id` (FK -> roles.id)
   - `permission_id` (FK -> permissions.id)
   - PK (`role_id`, `permission_id`)

3. `auth_tokens`
   - `id` (PK, UUID)
   - `user_id` (FK -> users.id)
   - `access_token` (text, not null)
   - `refresh_token` (text, not null)
   - `access_jti` (unique, not null)
   - `refresh_jti` (unique, not null)
   - `issued_at`
   - `access_expires_at`
   - `refresh_expires_at`
   - `revoked_at` (nullable)
   - `replaced_by_token_id` (nullable, self FK)
   - `created_ip` (nullable)
   - `created_user_agent` (nullable)

Optional hardening:
- Store token hashes instead of raw token strings while preserving the same lifecycle and constraints.

### User constraints
- `users.username` must be unique.
- Registration flow does not accept role assignment from client; it always assigns `STUDENT`.

## API Endpoints and Authorization

### Auth
- `POST /api/auth/register` -> anonymous
  - Creates student only.
- `POST /api/auth/login` -> anonymous
  - Returns access + refresh token pair.
- `POST /api/auth/refresh` -> anonymous (requires refresh token)
  - Rotates refresh token and issues a new pair.
- `POST /api/auth/logout` -> authenticated
  - Revokes current session/token pair.
- `POST /api/auth/logout-all` -> authenticated
  - Revokes all active sessions for current user.

### User/profile
- `GET /api/users` -> `users.read.any` (admin)
- `GET /api/users/{id}` -> `users.read.any` or owner (`users.read.self`)
- `PATCH /api/users/me` -> `users.update.self`
- `POST /api/users/professors` -> `users.create.professor` (admin)

### Permissions admin
- `GET /api/roles/{role}/permissions` -> `permissions.manage` (admin)
- `PATCH /api/roles/{role}/permissions` -> `permissions.manage` (admin)

### Exam actions
- Create exam -> `exams.create` (admin, professor)
- Solve exam -> `exams.solve` (admin, student)
- Grade/check exam -> `exams.grade` (admin, professor)
- Read own validations -> `exam.validations.read.self` (student owner)
- Read own results -> `exam.results.read.self` (student owner)

## Main Flows

### Scenario 1: Student registration + login + solve exam
Given anonymous user.
When the user registers via `/api/auth/register`.
Then the user is created with role `STUDENT`.
When the user logs in.
Then valid access/refresh tokens are issued and persisted in `auth_tokens`.
When the user calls solve endpoint.
Then request succeeds only if token is valid and permission `exams.solve` is present.

Acceptance criteria:
- [ ] Register ignores/rejects role in payload.
- [ ] Student can authenticate and solve.
- [ ] Token session row is created in DB.

---

### Scenario 2: Admin creates professor + updates permissions
Given authenticated admin.
When admin creates professor.
Then new user has role `PROFESSOR`.
When admin updates role permissions.
Then authorization behavior changes accordingly for that role.

Acceptance criteria:
- [ ] Non-admin cannot create professor.
- [ ] Non-admin cannot update permissions.
- [ ] Permission updates affect access decisions without code change.

---

### Scenario 3: Refresh token rotation and reuse protection
Given authenticated user with a valid refresh token.
When `/api/auth/refresh` is called.
Then old refresh token is revoked and replaced.
When revoked/old refresh token is reused.
Then request is denied (`401`).

Acceptance criteria:
- [ ] Refresh rotation works.
- [ ] Old refresh token cannot be reused.
- [ ] DB stores token replacement chain.

---

### Scenario 4: Forbidden access
Given student access token.
When student calls professor/admin-only endpoint.
Then response is `403 FORBIDDEN`.

Acceptance criteria:
- [ ] Missing permission returns `403`.
- [ ] Invalid/expired/revoked token returns `401`.

## Security Rules
- Stateless API + JWT auth filter.
- Validate JWT signature, expiration, `type`, and `jti`.
- Validate token exists in DB and is not revoked.
- Passwords stored with BCrypt.
- Consistent error shape for `401/403/404/400`.

## Implementation Plan (Execution Order)
1. Add entities/repositories for `Permission`, `RolePermission`, `AuthToken`.
2. Add migration and seed data for roles, permissions, and role-permission mapping.
3. Add auth service (`register`, `login`, `refresh`, `logout`, `logout-all`).
4. Add JWT service + token persistence + refresh rotation logic.
5. Replace open security config with JWT filter + permission checks.
6. Add permission management endpoints for admin.
7. Apply authorization checks to existing and new API endpoints.
8. Add tests (unit + WebMvc + integration) for auth, permission checks, and token lifecycle.

## Spring Boot Best Practices Applied
- Feature-oriented packages (`auth`, `user`, `exam`, `config`, `shared`).
- Constructor injection with `private final` dependencies.
- DTOs in API layer, entities hidden from direct API exposure.
- Validation on request DTOs via Bean Validation.
- `@ControllerAdvice` for consistent API errors.
- `@Transactional` at service methods.
- Spring Data JPA repositories for persistence.
- SLF4J parameterized logging.
- Test slices (`@WebMvcTest`) + unit/integration tests.
- Spring Security with BCrypt and method-level authorization.

## Test Coverage Matrix

| Scenario | Unit | WebMvc | Integration |
|---|---|---|---|
| Student register/login | [ ] | [ ] | [ ] |
| Professor creation by admin | [ ] | [ ] | [ ] |
| Permission update by admin | [ ] | [ ] | [ ] |
| Forbidden by missing permission | [ ] | [ ] | [ ] |
| Refresh rotation/reuse denial | [ ] | [ ] | [ ] |
| Logout/logout-all revocation | [ ] | [ ] | [ ] |

## Acceptance Criteria (Overall)
- [ ] JWT auth active on protected API routes.
- [ ] Registration is student-only.
- [ ] Professor creation is admin-only.
- [ ] Admin can modify role-permission mappings.
- [ ] Access/refresh tokens are stored and managed in DB with revocation/rotation.
- [ ] Permission checks enforce role capabilities as defined.
- [ ] Auth/RBAC tests pass in CI.
