# Backend Auth and RBAC Notes

## Roles

- `PROFESOR`
- `ESTUDIANTE`

## Expected Rules

- Solo `PROFESOR` crea examenes/preguntas/convocatorias.
- Solo `ESTUDIANTE` rinde intentos.
- Revision y calificacion solo rol docente.

## JWT Expectations

- Access token corto + refresh token.
- Endpoint `refresh` renueva access token.
- `logout` invalida sesion actual.

## Guardrails

- Validar autorizacion en backend siempre.
- Frontend role-check solo UX, no seguridad.
