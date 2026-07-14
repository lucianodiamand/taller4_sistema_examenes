# Frontend Auth JWT Flow - Specification

## Overview
Define minimal frontend authentication flow using JWT access/refresh tokens exposed by backend API.
Scope limited to frontend behavior and integration contract usage.

## Goal
- Let anonymous user register or log in.
- Keep authenticated session in frontend.
- Protect routes by authentication and role.
- Refresh expired access token automatically.
- Provide deterministic logout behavior.

## Scope

### In Scope
- Login form and register form.
- Session persistence across page reload.
- HTTP interceptor for `Authorization: Bearer <accessToken>`.
- Auth guard for protected routes.
- Role guard for role-restricted routes.
- Silent refresh on `401` once per failed request.
- Logout current session and local cleanup.

### Out of Scope
- Backend API/schema changes.
- MFA, social login, password recovery.
- Permission management UI.
- Cross-tab session sync and device management UI.

## Actors
- **Anonymous**: can register and log in.
- **Authenticated Student**
- **Authenticated Professor**
- **Authenticated Admin**

## Backend Endpoints Consumed
- `POST /api/auth/register`
- `POST /api/auth/login`
- `POST /api/auth/refresh`
- `POST /api/auth/logout`
- `POST /api/auth/logout-all` (optional UI action)
- `GET /api/users/me`

## Data Contracts Used

### Login/Register Request
```json
{
  "name": "string",
  "username": "string",
  "password": "string"
}
```

### Login/Refresh Response
```json
{
  "accessToken": "jwt",
  "refreshToken": "jwt",
  "tokenType": "Bearer",
  "accessExpiresAt": 1710000900,
  "refreshExpiresAt": 1710003600
}
```

### Current User Response
```json
{
  "id": 1,
  "name": "string",
  "username": "string",
  "role": "STUDENT"
}
```

## Frontend Architecture (Minimal)
- `AuthApiService`: thin HTTP wrapper for auth endpoints.
- `SessionService`: stores/reads token pair + expiry + current user.
- `AuthState`: signal/store exposing `isAuthenticated`, `role`, `user`.
- `AuthInterceptor`: attaches token and handles one refresh retry.
- `AuthGuard`: blocks anonymous access.
- `RoleGuard`: checks route `data.roles` against current role.

No external JWT decode dependency required.

## Storage Strategy
- Persist token pair in `localStorage` under one key: `exam.auth.session`.
- Persist `accessExpiresAt` and `refreshExpiresAt` as unix seconds.
- On app bootstrap:
  1. Read stored session.
  2. If refresh token expired, clear session.
  3. If access token valid, fetch `/api/users/me` and set user state.
  4. If access token expired but refresh valid, attempt refresh then fetch `/api/users/me`.

## Route Model

### Public
- `/login`
- `/register`

### Protected (auth required)
- `/app/**`

### Role Restricted (examples)
- `/app/professor/**` -> `PROFESSOR`, `ADMIN`
- `/app/student/**` -> `STUDENT`, `ADMIN`
- `/app/admin/**` -> `ADMIN`

If authenticated user hits `/login` or `/register`, redirect to role home.

## Main Flows

### Scenario 1: Login Success
Given anonymous user with valid credentials.
When user submits login form to `/api/auth/login`.
Then frontend stores token pair, loads `/api/users/me`, updates auth state, and redirects to role home.

Acceptance criteria:
- [ ] Tokens stored once login succeeds.
- [ ] Current user loaded before first protected screen render.
- [ ] Redirect target depends on role.

---

### Scenario 2: Access Token Expired During API Call
Given authenticated user with expired access token and valid refresh token.
When protected API call returns `401`.
Then interceptor calls `/api/auth/refresh`, updates session, retries original request once.

Acceptance criteria:
- [ ] Exactly one refresh attempt per failed request.
- [ ] Original request retried once after successful refresh.
- [ ] Infinite refresh loops prevented.

---

### Scenario 3: Refresh Token Expired or Invalid
Given session with invalid/expired refresh token.
When refresh attempt fails with `401`.
Then frontend clears session and redirects to `/login`.

Acceptance criteria:
- [ ] Local session fully removed.
- [ ] User sees login screen.
- [ ] Protected routes blocked until new login.

---

### Scenario 4: Role-Restricted Route Access
Given authenticated user without required role.
When user navigates to restricted route.
Then route guard denies navigation and redirects to role-safe home.

Acceptance criteria:
- [ ] Unauthorized role cannot open restricted route.
- [ ] Redirection deterministic and non-looping.

---

### Scenario 5: Logout
Given authenticated user.
When user clicks logout.
Then frontend calls `/api/auth/logout` (best effort), clears session, and redirects to `/login`.

Acceptance criteria:
- [ ] Session cleared even if network/logout endpoint fails.
- [ ] Next protected call requires fresh login.

## Error Handling Rules
- `400/422` on login/register: show server message near form.
- `401` on protected calls: trigger refresh flow.
- `403`: show forbidden screen/message, keep session.
- Any unexpected transport error: show generic retry message.

## Security and UX Constraints
- Never store password.
- Never include token in URL/query params.
- Avoid console logging JWTs.
- Keep role checks in frontend for UX only; backend remains authority.

## Implementation Sequence
1. Add auth models and `AuthApiService`.
2. Add `SessionService` + bootstrap restore flow.
3. Add `AuthInterceptor` with one-retry refresh logic.
4. Add `AuthGuard` and `RoleGuard`.
5. Add login/register/logout UI wiring.
6. Add minimal auth tests.

## Test Matrix

| Scenario | Unit | Integration | E2E |
|---|---|---|---|
| Login success stores session | [ ] | [ ] | [ ] |
| Interceptor adds bearer token | [ ] | [ ] | - |
| Refresh + retry once on 401 | [ ] | [ ] | [ ] |
| Refresh fail clears session | [ ] | [ ] | [ ] |
| Role guard denies route | [ ] | [ ] | [ ] |
| Logout clears session | [ ] | [ ] | [ ] |

## PR Guardrails
- Frontend-only changes.
- No file changes outside `frontend/**`.
- No dependency additions unless strictly required.
