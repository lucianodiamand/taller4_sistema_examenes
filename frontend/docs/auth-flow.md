# Flujo de autenticacion Frontend

Resumen corto. Detalle completo en `frontend/docs/specs/front-auth-jwt.spec.md`.

## Objetivos

- Login/register de usuario anonimo
- Mantener sesion autenticada
- Proteger rutas por autenticacion y rol
- Refresh token en 401
- Logout deterministico

## Partes principales

- Interceptor agrega `Authorization: Bearer <accessToken>`
- Guard de auth bloquea rutas privadas
- Guard de rol valida acceso por rol
- Refresh intenta una vez por request fallido

## Pantallas de admin y perfil

- `GET/PATCH /api/users/me` alimenta pagina de perfil (`/app/perfil`).
- Usuarios admin tienen dos pantallas mobile-first extra:
  - `/app/admin/usuarios` para CRUD de usuarios (`PROFESSOR` y `STUDENT` unicamente)
  - `/app/admin/roles-permisos` para descripcion de rol, mapeo rol-permiso y CRUD de permisos
- Texto de UI en espanol; contratos, identificadores de codigo e infraestructura en english.
