# Backend API Notes

Notas de referencia rapida para endpoints MVP.

## Base URL

- Local: `http://localhost:8080/api`

## Exam Endpoints (actuales)

- `GET /api/exams`
- `POST /api/exams`

## Auth Endpoints (contrato esperado por frontend)

- `POST /api/auth/register`
- `POST /api/auth/login`
- `POST /api/auth/refresh`
- `POST /api/auth/logout`
- `POST /api/auth/logout-all`
- `GET /api/users/me`

## Notes

- Mantener contratos estables entre frontend/backend.
- Documentar cambios de payloads en PR de backend.
