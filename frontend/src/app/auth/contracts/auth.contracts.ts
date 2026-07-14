export const AUTH_STORAGE_KEY = 'exam.auth.session' as const;
export const AUTH_HEADER = 'Authorization' as const;
export const AUTH_SCHEME = 'Bearer' as const;

export const APP_ROUTE_PATHS = {
  login: 'login',
  register: 'register',
  app: 'app',
  professor: 'professor',
  student: 'student',
  admin: 'admin',
  forbidden: 'forbidden',
} as const;

export const APP_ROUTES = {
  login: `/${APP_ROUTE_PATHS.login}`,
  register: `/${APP_ROUTE_PATHS.register}`,
  app: `/${APP_ROUTE_PATHS.app}`,
  professor: `/${APP_ROUTE_PATHS.app}/${APP_ROUTE_PATHS.professor}`,
  student: `/${APP_ROUTE_PATHS.app}/${APP_ROUTE_PATHS.student}`,
  admin: `/${APP_ROUTE_PATHS.app}/${APP_ROUTE_PATHS.admin}`,
  forbidden: `/${APP_ROUTE_PATHS.forbidden}`,
} as const;

export const AUTH_ENDPOINTS = {
  register: '/api/auth/register',
  login: '/api/auth/login',
  refresh: '/api/auth/refresh',
  logout: '/api/auth/logout',
  logoutAll: '/api/auth/logout-all',
  me: '/api/users/me',
} as const;

export const HTTP_STATUS = {
  BAD_REQUEST: 400,
  UNAUTHORIZED: 401,
  FORBIDDEN: 403,
} as const;

export const AUTH_UI = {
  formFieldAppearance: 'outline',
  snackbarDurationMs: 4000,
} as const;

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

export const ROLE_HOME_ROUTE: Record<UserRole, string> = {
  [UserRole.ADMIN]: APP_ROUTES.admin,
  [UserRole.PROFESSOR]: APP_ROUTES.professor,
  [UserRole.STUDENT]: APP_ROUTES.student,
};

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

export interface RouteRoleConfig {
  pathPrefix: string;
  allowedRoles: readonly UserRole[];
}

export const ROLE_ROUTE_ACCESS: readonly RouteRoleConfig[] = [
  { pathPrefix: APP_ROUTES.professor, allowedRoles: [UserRole.PROFESSOR, UserRole.ADMIN] },
  { pathPrefix: APP_ROUTES.student, allowedRoles: [UserRole.STUDENT, UserRole.ADMIN] },
  { pathPrefix: APP_ROUTES.admin, allowedRoles: [UserRole.ADMIN] },
];
