# Frontend Auth Flow

Resumen corto. Detalle completo en `frontend/docs/front-auth-jwt.spec.md`.

## Goals

- Login/register usuario anonimo
- Mantener sesion autenticada
- Proteger rutas por auth y rol
- Refresh token en 401
- Logout deterministico

## Main Parts

- Interceptor agrega `Authorization: Bearer <accessToken>`
- Guard de auth bloquea rutas privadas
- Guard de rol valida acceso por rol
- Refresh intenta una vez por request fallido
