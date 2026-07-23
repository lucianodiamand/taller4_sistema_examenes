# Notas de API Backend

Notas de referencia rápida para endpoints MVP.

## Base URL

- Local: `http://localhost:8080/api`

## Endpoints de examenes (profesor)

- `GET /api/exams` — lista solo los examenes del profesor autenticado.
- `POST /api/exams` — crea examen junto con sus preguntas, en una sola transaccion.
- `GET /api/exams/{examId}/calls` — lista las convocatorias de un examen propio.
- `POST /api/exams/{examId}/calls` — abre una convocatoria (ventana de fechas + cupo, cupo opcional = ilimitado).

## Endpoints de examenes de estudiante (EMA)

- `GET /api/student/exams/available`
- `POST /api/student/exams/{examCallId}/attempts`
- `PUT /api/student/attempts/{attemptId}/answers`
- `POST /api/student/attempts/{attemptId}/submit`
- `GET /api/student/attempts`
- `GET /api/student/attempts/{attemptId}`

Detalle de payloads y reglas: `backend/docs/student-exam-flow.md`.

## Endpoints de correccion (VALEN)

- `GET /api/grading/attempts`
- `GET /api/grading/exams/{examId}/attempts`
- `GET /api/grading/attempts/{attemptId}`
- `PATCH /api/grading/attempts/{attemptId}/questions/{questionId}`
- `POST /api/grading/attempts/{attemptId}/close`
- `GET /api/grading/my-results`
- `GET /api/grading/my-results/{attemptId}`
- `GET /api/grading/my-validations`

## Endpoints de autenticacion

- `POST /api/auth/register`
- `POST /api/auth/login`
- `POST /api/auth/refresh`
- `POST /api/auth/logout`
- `POST /api/auth/logout-all`
- `GET /api/users/me`

## Endpoints admin de usuarios

- `GET /api/users`
- `POST /api/users`
- `PATCH /api/users/{id}`
- `DELETE /api/users/{id}`

Restricciones:

- Admin CRUD limitado a usuarios `PROFESSOR` y `STUDENT`.
- Usuarios admin no se pueden modificar ni eliminar desde estos endpoints.
- Usuario logueado actual no puede auto-eliminarse.

## Endpoints de roles y permisos

- `GET /api/roles`
- `PATCH /api/roles/{role}`
- `GET /api/roles/{role}/permissions`
- `PATCH /api/roles/{role}/permissions`
- `GET /api/permissions`
- `POST /api/permissions`
- `PATCH /api/permissions/{id}`
- `DELETE /api/permissions/{id}`

Restricciones:

- Roles se mantienen fijos (`ADMIN`, `PROFESSOR`, `STUDENT`).
- El borrado de permiso falla cuando el permiso esta asignado a algun rol.

## Notas

- Mantener contratos estables entre frontend/backend.
- Documentar cambios de payloads en PR de backend.
- Los endpoints del estudiante toman su identidad desde el JWT; no reciben `studentId`.
- Los endpoints de corrección e historial toman identidad desde JWT (`CurrentUser`); no reciben `studentId` ni `professorId`.
- Los endpoints de examenes tambien toman el `professorId` desde el JWT, nunca del body (fix de seguridad: antes se podia crear un examen "a nombre de" otro profesor).
