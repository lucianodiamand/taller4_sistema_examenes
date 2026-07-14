# Backend API Notes

Notas de referencia rápida para endpoints MVP.

## Base URL

- Local: `http://localhost:8080/api`

## Exam Endpoints

- `GET /api/exams`
- `POST /api/exams`

## Student Exam Endpoints (EMA)

- `GET /api/student/exams/available`
- `POST /api/student/exams/{examCallId}/attempts`
- `PUT /api/student/attempts/{attemptId}/answers`
- `POST /api/student/attempts/{attemptId}/submit`
- `GET /api/student/attempts`
- `GET /api/student/attempts/{attemptId}`

Detalle de payloads y reglas: `backend/docs/student-exam-flow.md`.

## Auth Endpoints

- `POST /api/auth/register`
- `POST /api/auth/login`
- `POST /api/auth/refresh`
- `POST /api/auth/logout`
- `POST /api/auth/logout-all`
- `GET /api/users/me`

## Notes

- Mantener contratos estables entre frontend/backend.
- Documentar cambios de payloads en PR de backend.
- Los endpoints del estudiante toman su identidad desde el JWT; no reciben `studentId`.
