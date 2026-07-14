export enum UserRole {
  ADMIN = 'ADMIN',
  PROFESSOR = 'PROFESSOR',
  STUDENT = 'STUDENT'
}

export const AUTH_STORAGE_KEY = 'exam.auth.session';
export const AUTH_RETRY_HEADER = 'x-auth-retry';
export const AUTH_RETRY_MARK = '1';
export const AUTH_HEADER_NAME = 'Authorization';
export const AUTH_BEARER_PREFIX = 'Bearer ';
export const AUTH_API_PREFIX = '/api/auth/';
export const UNIX_TIME_MS_FACTOR = 1000;
export const PASSWORD_MIN_LENGTH = 6;

export const HTTP_STATUS = {
  UNAUTHORIZED: 401
} as const;

export const AUTH_API_ENDPOINTS = {
  LOGIN: '/api/auth/login',
  REGISTER: '/api/auth/register',
  REFRESH: '/api/auth/refresh',
  LOGOUT: '/api/auth/logout',
  USER_ME: '/api/users/me'
} as const;

export const AUTH_ROUTE_PATHS = {
  ROOT: '',
  LOGIN: 'login',
  REGISTER: 'register',
  FORBIDDEN: 'forbidden',
  APP: 'app',
  APP_ADMIN: 'app/admin',
  APP_PROFESSOR: 'app/professor',
  APP_STUDENT: 'app/student',
  WILDCARD: '**'
} as const;

export const AUTH_ROUTE_DATA_KEYS = {
  ROLES: 'roles'
} as const;

export const AUTH_ROUTE_URLS = {
  LOGIN: '/login',
  REGISTER: '/register',
  FORBIDDEN: '/forbidden',
  APP: '/app',
  APP_ADMIN: '/app/admin',
  APP_PROFESSOR: '/app/professor',
  APP_STUDENT: '/app/student'
} as const;

export const ROLE_HOME_URL: Record<UserRole, string> = {
  [UserRole.ADMIN]: AUTH_ROUTE_URLS.APP_ADMIN,
  [UserRole.PROFESSOR]: AUTH_ROUTE_URLS.APP_PROFESSOR,
  [UserRole.STUDENT]: AUTH_ROUTE_URLS.APP_STUDENT
};

export const AUTH_MESSAGES = {
  LOGIN_ERROR: 'No se pudo iniciar sesion',
  REGISTER_ERROR: 'No se pudo completar registro'
} as const;
