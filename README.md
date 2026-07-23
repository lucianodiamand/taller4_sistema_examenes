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
- `backend/docs/`: documentacion backend (modelo/ER, API, auth/RBAC, diagramas de secuencia)
- `frontend/`: app Angular
- `frontend/docs/`: documentacion frontend (setup, auth, testing)

## Arquitectura

```mermaid
flowchart TB
    subgraph Frontend["Frontend - Angular 22 standalone + Material"]
        UI[Componentes por rol<br/>auth / student / professor]
        INT[authInterceptor<br/>adjunta JWT, refresca en 401]
        UI --> INT
    end

    subgraph Backend["Backend - Spring Boot"]
        AUTH["auth<br/>login, JWT, refresh, logout"]
        USER["user<br/>CRUD admin: usuarios, roles, permisos"]
        EXAM["exam<br/>crear examen / resolver examen / corregir examen"]
        SEC["'@PreAuthorize' - RBAC por rol + permiso"]
        AUTH --> SEC
        USER --> SEC
        EXAM --> SEC
    end

    DB[("H2 en memoria<br/>(perfil dev)")]

    INT -- "HTTP + Bearer JWT" --> AUTH
    INT -- "HTTP + Bearer JWT" --> USER
    INT -- "HTTP + Bearer JWT" --> EXAM
    SEC --> DB
```

Detalle de los 3 flujos del dominio de examenes (sequence diagrams + ERD): `backend/docs/exam-flows-diagrams.md`. Auth y users: `backend/docs/auth-users-diagrams.md`.

## Contribution Flow

Una rama por feature completa (varios commits chicos adentro), un PR por feature contra `main` - no un PR por cada cambio tecnico suelto.

```mermaid
gitGraph
    commit id: "main"
    branch feat/mi-feature
    checkout feat/mi-feature
    commit id: "paso 1"
    commit id: "paso 2"
    commit id: "paso 3"
    checkout main
    merge feat/mi-feature
```

1. Crear rama (`feat/...`, `fix/...`, `refactor/...`, `docs/...`, `chore/...`)
2. Commits chicos con formato `action(scope): summary`
3. Abrir PR contra `main` cuando la feature esta completa y sus tests pasan
4. Merge luego de revision

Tipos comunes de commit: `feat`, `fix`, `refactor`, `docs`, `test`, `chore`.

## Links

- Backend docs: `backend/docs/README.md`
- Backend auth JWT spec: `backend/docs/specs/backend-auth-jwt.spec.md`
- Backend create exam spec: `backend/docs/specs/create-exam-use-case.spec.md`
- Frontend docs: `frontend/docs/README.md`
- Frontend auth JWT spec: `frontend/docs/specs/front-auth-jwt.spec.md`
