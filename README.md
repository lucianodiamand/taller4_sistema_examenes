# Sistema de Examenes Online - MVP

Indice central del repositorio. Detalle tecnico vive en `backend/docs/` y `frontend/docs/`.

## Overview

Plataforma para crear y rendir examenes online.
Profesor crea examen y convocatoria. Estudiante responde en ventana habilitada. Profesor revisa y publica resultado.

## Stack

- Backend: Java 21 + Spring Boot 4 + Maven + H2
- Frontend: Angular 22 + pnpm
- Orquestacion local: Docker Compose

## Quick Start

Levantar app completa con Docker:

```bash
docker compose up --build
```

Servicios:

- Frontend: `http://localhost:4200`
- Backend API: `http://localhost:8080/api`

Detener entorno:

```bash
docker compose down
```

## Repo Map

- `backend/`: API Spring Boot
- `backend/docs/`: documentacion backend (modelo/ER, API, auth/RBAC)
- `frontend/`: app Angular
- `frontend/docs/`: documentacion frontend (setup, auth, testing)

## Contribution Flow

1. Crear rama (`feature/...`, `fix/...`, `docs/...`)
2. Commits con formato `action(scope): summary`
3. Abrir PR contra `main`
4. Merge luego de revision

Tipos comunes de commit: `feat`, `fix`, `refactor`, `docs`, `test`, `chore`.

## Links

- Backend docs: `backend/docs/README.md`
- Frontend docs: `frontend/docs/README.md`
- Frontend auth JWT spec: `frontend/docs/front-auth-jwt.spec.md`
