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

## Grading Endpoints (VALEN)

- `GET /api/grading/attempts`
- `GET /api/grading/exams/{examId}/attempts`
- `GET /api/grading/attempts/{attemptId}`
- `PATCH /api/grading/attempts/{attemptId}/questions/{questionId}`
- `POST /api/grading/attempts/{attemptId}/close`
- `GET /api/grading/my-results`
- `GET /api/grading/my-results/{attemptId}`
- `GET /api/grading/my-validations`

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
- Los endpoints de corrección e historial toman identidad desde JWT (`CurrentUser`); no reciben `studentId` ni `professorId`.
