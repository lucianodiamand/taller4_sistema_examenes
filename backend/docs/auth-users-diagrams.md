# Diagramas de modulos Auth y Users

Este documento resume flujo de llamadas y modelo de datos para los modulos `auth` y `user`.

## Sequence diagram - Auth module

```mermaid
sequenceDiagram
    autonumber
    actor Client
    participant AuthController
    participant AuthService
    participant UserRepository
    participant RoleRepository
    participant PasswordEncoder
    participant JwtService
    participant AuthTokenRepository

    alt POST /api/auth/register
        Client->>AuthController: register(name, username, password)
        AuthController->>AuthService: registerStudent(...)
        AuthService->>UserRepository: existsByUsername(username)
        AuthService->>RoleRepository: findByName("STUDENT")
        AuthService->>PasswordEncoder: encode(password)
        AuthService->>UserRepository: save(User)
        AuthController-->>Client: 201 Created
    end

    alt POST /api/auth/login
        Client->>AuthController: login(username, password)
        AuthController->>AuthService: login(..., ip, userAgent)
        AuthService->>UserRepository: findByUsername(username)
        AuthService->>PasswordEncoder: matches(raw, encoded)
        AuthService->>JwtService: generateAccessToken(...)
        AuthService->>JwtService: generateRefreshToken(...)
        AuthService->>AuthTokenRepository: save(AuthToken)
        AuthController-->>Client: 200 TokenResponse
    end

    alt POST /api/auth/refresh
        Client->>AuthController: refresh(refreshToken)
        AuthController->>AuthService: refresh(..., ip, userAgent)
        AuthService->>JwtService: parseClaims(refreshToken)
        AuthService->>AuthTokenRepository: findByRefreshJti(jti)
        AuthService->>JwtService: generateAccessToken(...)
        AuthService->>JwtService: generateRefreshToken(...)
        AuthService->>AuthTokenRepository: save(new AuthToken)
        AuthService->>AuthTokenRepository: save(old revoked token)
        AuthController-->>Client: 200 TokenResponse
    end

    alt POST /api/auth/logout
        Client->>AuthController: logout(Authorization: Bearer ...)
        AuthController->>AuthService: logout(accessToken)
        AuthService->>JwtService: parseClaims(accessToken)
        AuthService->>AuthTokenRepository: findByAccessJti(jti)
        AuthService->>AuthTokenRepository: save(revoked token)
        AuthController-->>Client: 204 No Content
    end

    alt POST /api/auth/logout-all
        Client->>AuthController: logoutAll()
        AuthController->>AuthService: logoutAllCurrentUserSessions()
        AuthService->>AuthTokenRepository: findByUserIdAndRevokedAtIsNull(userId)
        AuthService->>AuthTokenRepository: saveAll(revoked tokens)
        AuthController-->>Client: 204 No Content
    end
```

## Sequence diagram - Users module

```mermaid
sequenceDiagram
    autonumber
    actor Client
    participant UserController
    participant RoleController
    participant PermissionController
    participant UserService
    participant RolePermissionService
    participant PermissionService
    participant CurrentUser
    participant UserRepository
    participant RoleRepository
    participant PermissionRepository

    Note over Client,PermissionController: Endpoints protegidos por @PreAuthorize (RBAC por permission)

    alt GET /api/users/me
        Client->>UserController: findMe()
        UserController->>CurrentUser: id()
        UserController->>UserService: findMe()
        UserService->>UserRepository: findById(currentUserId)
        UserController-->>Client: 200 UserResponse
    end

    alt PATCH /api/users/me
        Client->>UserController: updateMe(name,password)
        UserController->>UserService: updateMe(...)
        UserService->>UserRepository: findById(currentUserId)
        UserService->>UserRepository: save(updated user)
        UserController-->>Client: 200 UserResponse
    end

    alt Admin CRUD /api/users
        Client->>UserController: create/update/delete/list
        UserController->>CurrentUser: id()
        UserController->>UserService: createByAdmin | updateByAdmin | deleteByAdmin | findAll
        UserService->>RoleRepository: findByName(PROFESSOR|STUDENT)
        UserService->>UserRepository: save | delete | findAll
        UserController-->>Client: 200/201/204
    end

    alt Roles and role-permission mapping
        Client->>RoleController: get roles / update role / replace permissions
        RoleController->>RolePermissionService: findAllRoles | updateRoleDescription | replacePermissions
        RolePermissionService->>RoleRepository: findByName | findAll | save
        RolePermissionService->>PermissionRepository: findByCodeIn
        RoleController-->>Client: 200
    end

    alt Permission CRUD /api/permissions
        Client->>PermissionController: create/update/delete/list
        PermissionController->>PermissionService: create | update | delete | findAll
        PermissionService->>PermissionRepository: save | findById | findAll | delete
        PermissionService->>RoleRepository: existsByPermissions_Id(permissionId)
        PermissionController-->>Client: 200/201/204
    end
```

## ERD - Auth + Users schema

```mermaid
erDiagram
    ROLE {
        BIGINT id PK
        VARCHAR name UK
        VARCHAR description
    }

    USER {
        BIGINT id PK
        VARCHAR name
        VARCHAR username UK
        VARCHAR password
        BIGINT role_id FK
    }

    PERMISSION {
        BIGINT id PK
        VARCHAR code UK
        VARCHAR description
    }

    ROLE_PERMISSION {
        BIGINT role_id PK, FK
        BIGINT permission_id PK, FK
    }

    AUTH_TOKEN {
        UUID id PK
        BIGINT user_id FK
        VARCHAR access_token
        VARCHAR refresh_token
        VARCHAR access_jti UK
        VARCHAR refresh_jti UK
        VARCHAR session_id
        TIMESTAMP issued_at
        TIMESTAMP access_expires_at
        TIMESTAMP refresh_expires_at
        TIMESTAMP revoked_at
        UUID replaced_by_token_id FK
        VARCHAR created_ip
        VARCHAR created_user_agent
    }

    ROLE ||--o{ USER : "assigned to"
    ROLE ||--o{ ROLE_PERMISSION : "contains"
    PERMISSION ||--o{ ROLE_PERMISSION : "mapped in"
    USER ||--o{ AUTH_TOKEN : "owns sessions"
    AUTH_TOKEN }o--|| AUTH_TOKEN : "replaced by"
```

## Notas

- Diagramas usan nombres de entidades/campos en English para respetar codigo y schema.
- Este ERD cubre auth/users; el dominio de examenes esta en `backend/docs/domain-model.md`.
