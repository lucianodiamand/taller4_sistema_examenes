# Frontend Auth JWT Flow Specification

## Overview

Define minimal frontend authentication flow using JWT access/refresh tokens exposed by backend API. Scope limited to frontend behavior and integration contract usage.

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

- Anonymous: can register and log in.
- Authenticated Student.
- Authenticated Professor.
- Authenticated Admin.

## Backend Endpoints Consumed

- `POST /api/auth/register`
- `POST /api/auth/login`
- `POST /api/auth/refresh`
- `POST /api/auth/logout`
- `POST /api/auth/logout-all` (optional UI action)
- `GET /api/users/me`

## Constants, Enums, and Interfaces (Required)

Use shared constants/enums/interfaces for auth module. Do not hardcode strings, status codes, route paths, storage keys, roles, or backend error codes in components/services/guards/interceptors.

### Core Constants

```ts
export const AUTH_STORAGE_KEY = 'exam.auth.session' as const;
export const AUTH_HEADER = 'Authorization' as const;
export const AUTH_SCHEME = 'Bearer' as const;

export const AUTH_ENDPOINTS = {
  register: '/api/auth/register',
  login: '/api/auth/login',
  refresh: '/api/auth/refresh',
  logout: '/api/auth/logout',
  logoutAll: '/api/auth/logout-all',
  me: '/api/users/me',
} as const;

export const APP_ROUTES = {
  login: '/login',
  register: '/register',
  app: '/app',
  professor: '/app/professor',
  student: '/app/student',
  admin: '/app/admin',
} as const;

export const HTTP_STATUS = {
  BAD_REQUEST: 400,
  UNAUTHORIZED: 401,
  FORBIDDEN: 403,
} as const;
```

### Enums

```ts
export enum UserRole {
  ADMIN = 'ADMIN',
  PROFESSOR = 'PROFESSOR',
  STUDENT = 'STUDENT',
}

export enum ApiErrorCode {
  BAD_REQUEST = 'BAD_REQUEST',
  VALIDATION_ERROR = 'VALIDATION_ERROR',
  UNAUTHORIZED = 'UNAUTHORIZED',
  FORBIDDEN = 'FORBIDDEN',
  NOT_FOUND = 'NOT_FOUND',
}
```

### Interfaces

```ts
export interface RegisterRequest {
  name: string;
  username: string;
  password: string;
}

export interface LoginRequest {
  username: string;
  password: string;
}

export interface RefreshRequest {
  refreshToken: string;
}

export interface TokenResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: 'Bearer';
  accessExpiresAt: number;
  refreshExpiresAt: number;
}

export interface CurrentUserResponse {
  id: number;
  name: string;
  username: string;
  role: UserRole;
}

export interface AuthSession {
  accessToken: string;
  refreshToken: string;
  accessExpiresAt: number;
  refreshExpiresAt: number;
  user: CurrentUserResponse | null;
}

export interface ApiErrorResponse {
  error: ApiErrorCode;
  message: string;
}
```

### Angular + Material UI Constants

```ts
export const AUTH_UI = {
  formFieldAppearance: 'outline',
  snackbarDurationMs: 4000,
} as const;
```

Use `AUTH_UI` values in auth components/services (no inline durations or repeated UI literals).

### Route Access Contract

```ts
export interface RouteRoleConfig {
  pathPrefix: string;
  allowedRoles: readonly UserRole[];
}

export const ROLE_ROUTE_ACCESS: readonly RouteRoleConfig[] = [
  { pathPrefix: APP_ROUTES.professor, allowedRoles: [UserRole.PROFESSOR, UserRole.ADMIN] },
  { pathPrefix: APP_ROUTES.student, allowedRoles: [UserRole.STUDENT, UserRole.ADMIN] },
  { pathPrefix: APP_ROUTES.admin, allowedRoles: [UserRole.ADMIN] },
];
```

## Angular Skills + Angular Material Baseline

Project uses Angular `22` + Angular Material. Auth implementation must follow installed Angular skills and Material-first UI.

### Angular Best-Practice Constraints

- Standalone components and route-first feature structure.
- `ChangeDetectionStrategy.OnPush` on auth components.
- Use signals (`signal`, `computed`) for local auth UI state.
- Use `inject()` instead of constructor injection for services in new auth code.
- Use modern control flow (`@if`, `@for`) in templates.
- Keep cross-cutting auth behavior in interceptor/guards, not duplicated in components.

### Angular Tooling Constraints

- Prefer Angular CLI generators for new auth artifacts (`ng g c`, `ng g s`, `ng g guard`, `ng g interceptor`).
- Keep generated code standalone-compatible.
- Do not add non-Angular tooling/dependencies for auth unless strictly required.

### Angular Material Baseline (Auth Screens)

- Login/register pages use Material components (`MatCard`, `MatFormField`, `MatInput`, `MatButton`).
- Loading states use `MatProgressSpinner`.
- Error/feedback messages use `MatSnackBar` with `AUTH_UI.snackbarDurationMs`.
- Form field appearance defaults to `AUTH_UI.formFieldAppearance`.
- Keep accessibility defaults: labels, `type=password`, keyboard-friendly submit.

## Data Contracts Used

### Register Request (`RegisterRequest`)

```json
{
  "name": "string",
  "username": "string",
  "password": "string"
}
```

### Register Response

- `201 Created` with empty body.

### Login Request (`LoginRequest`)

```json
{
  "username": "string",
  "password": "string"
}
```

### Refresh Request (`RefreshRequest`)

```json
{
  "refreshToken": "jwt"
}
```

### Login/Refresh Response (`TokenResponse`)

```json
{
  "accessToken": "jwt",
  "refreshToken": "jwt",
  "tokenType": "Bearer",
  "accessExpiresAt": 1710000900,
  "refreshExpiresAt": 1710003600
}
```

### Current User Response (`CurrentUserResponse`)

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
- `RoleGuard`: checks `route.data.roles` against current role.
- No external JWT decode dependency required.

## Storage Strategy

- Persist token pair in `localStorage` under `AUTH_STORAGE_KEY`.
- Persist `accessExpiresAt` and `refreshExpiresAt` as Unix seconds.
- On app bootstrap:
  - Read stored session.
  - If refresh token expired, clear session.
  - If access token valid, fetch `AUTH_ENDPOINTS.me` and set user state.
  - If access token expired but refresh valid, attempt refresh on `AUTH_ENDPOINTS.refresh`, then fetch `AUTH_ENDPOINTS.me`.

## Route Model

### Public

- `APP_ROUTES.login`
- `APP_ROUTES.register`

### Protected (auth required)

- `APP_ROUTES.app/**`

### Role Restricted (examples)

- `APP_ROUTES.professor/**` -> `UserRole.PROFESSOR`, `UserRole.ADMIN`
- `APP_ROUTES.student/**` -> `UserRole.STUDENT`, `UserRole.ADMIN`
- `APP_ROUTES.admin/**` -> `UserRole.ADMIN`
- If authenticated user hits `APP_ROUTES.login` or `APP_ROUTES.register`, redirect to role home.

## Main Flows

### Scenario 0: Register Success

Given anonymous user with valid registration data. When user submits register form to `AUTH_ENDPOINTS.register`. Then backend creates user with `UserRole.STUDENT` role and returns `201` without tokens.

Acceptance criteria:

- Register request sends `name`, `username`, `password` only.
- Frontend does not assume auto-login after register.
- Frontend redirects to `APP_ROUTES.login` (or executes explicit login flow).

### Scenario 1: Login Success

Given anonymous user with valid credentials. When user submits login form to `AUTH_ENDPOINTS.login`. Then frontend stores token pair, loads `AUTH_ENDPOINTS.me`, updates auth state, and redirects to role home.

Acceptance criteria:

- Tokens stored once login succeeds.
- Current user loaded before first protected screen render.
- Redirect target depends on role.

### Scenario 2: Access Token Expired During API Call

Given authenticated user with expired access token and valid refresh token. When protected API call returns `HTTP_STATUS.UNAUTHORIZED`. Then interceptor calls `AUTH_ENDPOINTS.refresh`, updates session, retries original request once.

Acceptance criteria:

- Exactly one refresh attempt per failed request.
- Original request retried once after successful refresh.
- Infinite refresh loops prevented.

### Scenario 3: Refresh Token Expired or Invalid

Given session with invalid/expired refresh token. When refresh attempt fails (`HTTP_STATUS.BAD_REQUEST` or `HTTP_STATUS.UNAUTHORIZED`). Then frontend clears session and redirects to `APP_ROUTES.login`.

Acceptance criteria:

- Local session fully removed.
- User sees login screen.
- Protected routes blocked until new login.

### Scenario 4: Role-Restricted Route Access

Given authenticated user without required role. When user navigates to restricted route. Then route guard denies navigation and redirects to role-safe home.

Acceptance criteria:

- Unauthorized role cannot open restricted route.
- Redirection deterministic and non-looping.

### Scenario 5: Logout

Given authenticated user. When user clicks logout. Then frontend calls `AUTH_ENDPOINTS.logout` (best effort), clears session, and redirects to `APP_ROUTES.login`.

Acceptance criteria:

- Session cleared even if network/logout endpoint fails.
- Next protected call requires fresh login.

## Error Handling Rules

- `HTTP_STATUS.BAD_REQUEST` on register/refresh validation or business errors (`ApiErrorCode.BAD_REQUEST` / `ApiErrorCode.VALIDATION_ERROR`): show backend `message` near related form.
- `HTTP_STATUS.UNAUTHORIZED` on login invalid credentials (`ApiErrorCode.UNAUTHORIZED`): show backend `message` and keep user on login screen.
- `HTTP_STATUS.UNAUTHORIZED` on protected calls: trigger refresh flow.
- Refresh failure (`HTTP_STATUS.BAD_REQUEST` / `HTTP_STATUS.UNAUTHORIZED`): clear session and redirect to `APP_ROUTES.login`.
- `HTTP_STATUS.FORBIDDEN` (`ApiErrorCode.FORBIDDEN`): show forbidden screen/message, keep session.
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
| --- | --- | --- | --- |
| Register success returns 201 and redirects to login | [ ] | [ ] | [ ] |
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
