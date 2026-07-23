# Notas de Auth y RBAC Backend

## Roles

- `ADMIN`
- `PROFESSOR`
- `STUDENT`

## Modelo RBAC

No es solo "chequear el rol": cada endpoint valida rol + permiso puntual con `@PreAuthorize`, ej. `hasRole('PROFESSOR') and hasAuthority('exams.create')`. Los permisos (`exams.create`, `exams.solve`, `exams.grade`, `users.read.self`, etc.) se asignan a roles en la tabla `role_permissions` — ver `V1__auth_rbac.sql`.

## Expected Rules

- Solo `PROFESSOR` crea examenes/preguntas/convocatorias (endpoints de `/api/exams` exigen `hasRole('PROFESSOR')`, `ADMIN` no entra aunque tenga el permiso `exams.create` otorgado en la tabla — es una inconsistencia conocida entre el modelo de permisos y el `@PreAuthorize` real).
- Solo `STUDENT` rinde intentos (`/api/student/**` exige `hasRole('STUDENT')`, mismo caso: `ADMIN` tiene `exams.solve` otorgado pero no pasa el `hasRole`).
- Revision y calificacion solo rol docente (`/api/grading/**` exige `hasRole('PROFESSOR')` para corregir, `hasRole('STUDENT')` para ver resultados propios).
- `ADMIN` administra usuarios, roles y permisos (`/api/users`, `/api/roles`, `/api/permissions`), pero no opera el dominio de examenes en la practica pese a tener permisos otorgados — ver parrafo anterior.

## JWT Expectations

- Access token corto + refresh token.
- Endpoint `refresh` renueva access token.
- `logout` invalida sesion actual.

## Guardrails

- Validar autorizacion en backend siempre.
- Frontend role-check solo UX, no seguridad.
