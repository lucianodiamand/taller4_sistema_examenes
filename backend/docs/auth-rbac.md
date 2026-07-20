# Notas de Auth y RBAC Backend

## Roles

- `PROFESSOR`
- `STUDENT`

## Expected Rules

- Solo `PROFESSOR` crea examenes/preguntas/convocatorias.
- Solo `STUDENT` rinde intentos.
- Revision y calificacion solo rol docente.

## JWT Expectations

- Access token corto + refresh token.
- Endpoint `refresh` renueva access token.
- `logout` invalida sesion actual.

## Guardrails

- Validar autorizacion en backend siempre.
- Frontend role-check solo UX, no seguridad.
