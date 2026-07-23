export const AUTH_STORAGE_KEY = 'exam.auth.session' as const;
export const AUTH_HEADER = 'Authorization' as const;
export const AUTH_SCHEME = 'Bearer' as const;

export const APP_ROUTE_PATHS = {
  login: 'login',
  register: 'register',
  app: 'app',
  profile: 'perfil',
  professor: 'professor',
  professorExams: 'exams',
  student: 'student',
  admin: 'admin',
  adminUsers: 'usuarios',
  adminAccess: 'roles-permisos',
  forbidden: 'forbidden',
} as const;

export const APP_ROUTES = {
  login: `/${APP_ROUTE_PATHS.login}`,
  register: `/${APP_ROUTE_PATHS.register}`,
  app: `/${APP_ROUTE_PATHS.app}`,
  profile: `/${APP_ROUTE_PATHS.app}/${APP_ROUTE_PATHS.profile}`,
  professor: `/${APP_ROUTE_PATHS.app}/${APP_ROUTE_PATHS.professor}`,
  professorExams: `/${APP_ROUTE_PATHS.app}/${APP_ROUTE_PATHS.professor}/${APP_ROUTE_PATHS.professorExams}`,
  student: `/${APP_ROUTE_PATHS.app}/${APP_ROUTE_PATHS.student}`,
  admin: `/${APP_ROUTE_PATHS.app}/${APP_ROUTE_PATHS.admin}`,
  adminUsers: `/${APP_ROUTE_PATHS.app}/${APP_ROUTE_PATHS.admin}/${APP_ROUTE_PATHS.adminUsers}`,
  adminAccess: `/${APP_ROUTE_PATHS.app}/${APP_ROUTE_PATHS.admin}/${APP_ROUTE_PATHS.adminAccess}`,
  forbidden: `/${APP_ROUTE_PATHS.forbidden}`,
} as const;

export const AUTH_ENDPOINTS = {
  register: '/api/auth/register',
  login: '/api/auth/login',
  refresh: '/api/auth/refresh',
  logout: '/api/auth/logout',
  logoutAll: '/api/auth/logout-all',
  me: '/api/users/me',
  users: '/api/users',
  roles: '/api/roles',
  permissions: '/api/permissions',
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

export interface UpdateMeRequest {
  name?: string;
  password?: string | null;
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

export interface UserAdminResponse {
  id: number;
  name: string;
  username: string;
  role: UserRole;
  self: boolean;
  manageable: boolean;
}

export interface CreateUserAdminRequest {
  name: string;
  username: string;
  password: string;
  role: UserRole;
}

export interface UpdateUserAdminRequest {
  name?: string;
  password?: string;
  role?: UserRole;
}

export interface RoleAdminResponse {
  id: number;
  name: UserRole;
  description: string | null;
}

export interface UpdateRoleRequest {
  description: string;
}

export interface RolePermissionsResponse {
  role: UserRole;
  permissionCodes: string[];
}

export interface ReplaceRolePermissionsRequest {
  permissionCodes: string[];
}

export interface PermissionResponse {
  id: number;
  code: string;
  description: string | null;
}

export interface CreatePermissionRequest {
  code: string;
  description: string;
}

export interface UpdatePermissionRequest {
  code: string;
  description: string;
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
  { pathPrefix: APP_ROUTES.professor, allowedRoles: [UserRole.PROFESSOR] },
  { pathPrefix: APP_ROUTES.student, allowedRoles: [UserRole.STUDENT, UserRole.ADMIN] },
  { pathPrefix: APP_ROUTES.admin, allowedRoles: [UserRole.ADMIN] },
];
