# Sistema de Examenes Online - MVP

Plataforma para crear y rendir examenes online.
El profesor crea examenes con preguntas, el estudiante responde durante una ventana habilitada y luego espera la revision del profesor para ver su resultado.

## Integrantes

- Redruello
- Mazziotta
- Duarte
- Orzusa

## Objetivo del MVP

Optimizar:

- la generacion de examenes,
- el proceso de correccion,
- la experiencia de evaluacion del estudiante.

## Alcance funcional (MVP)

- Autenticacion con roles: `PROFESOR` y `ESTUDIANTE`.
- El profesor crea examenes y preguntas.
- El profesor habilita una convocatoria (fecha/hora de inicio y fin).
- El estudiante solo puede rendir si la convocatoria esta abierta.
- Al iniciar, el sistema genera un intento unico por estudiante con preguntas en orden aleatorio.
- El estudiante envia respuestas y su intento queda cerrado (no editable).
- El profesor revisa y califica intentos (sin modificar respuestas del estudiante).
- El estudiante consulta su resultado al finalizar la revision.

## Arquitectura

- Frontend: Angular
- Backend: Java + Spring Boot
- Base de datos: Relacional (JPA/Hibernate)

## Estructura del repositorio

- `backend/`: API Spring Boot (Maven).
- `frontend/`: aplicacion Angular 22 (pnpm).

## Tecnologias

- Java 21
- Spring Boot 4.0.6
- Maven
- H2 Database
- Angular 22
- pnpm

## Ejecucion local (backend)

```bash
cd backend
mvn clean install
mvn spring-boot:run
```

## Ejecucion local (frontend)

Requisito: Node.js `>= 22.22.3`.

```bash
cd frontend
pnpm install
pnpm start
```

## Flujo de contribucion (Git)

Modelo recomendado: ramas por funcionalidad + Pull Request a `main`.

1. Crear rama:
   - `feature/...`
   - `fix/...`
   - `docs/...`
2. Commits con formato:
   - `action(scope): summary`
3. Abrir PR y solicitar revision.
4. Merge a `main` luego de aprobacion.

### Convencion de commits

Formato:

`action(scope): summary`

Acciones sugeridas:

- `feat`: nueva funcionalidad
- `fix`: correccion de bug
- `refactor`: mejora interna sin cambio funcional
- `docs`: documentacion
- `test`: pruebas
- `chore`: tareas tecnicas

Ejemplos:

- `feat(exam): agregar creacion de preguntas`
- `fix(auth): validar rol de estudiante en inicio de intento`
- `docs(readme): actualizar alcance mvp`

## Como crear un Pull Request

1. Actualizar tu rama con `main`.
2. Verificar que compile y pasen los tests.
3. Hacer push de la rama al remoto.
4. Crear PR con:
   - Titulo corto y descriptivo.
   - Descripcion del problema, solucion y evidencia (capturas/logs si aplica).
5. Asignar revisores.
6. Resolver comentarios y actualizar el PR.

## Diagrama Entidad-Relacion (MVP)

```mermaid
erDiagram
    ROLE {
        Long id PK
        String name
        String description
    }

    USER {
        Long id PK
        String name
        String username
        String password
        Long role_id FK
    }

    EXAM {
        Long id PK
        String title
        String description
        Integer durationMinutes
        Long professor_id FK
    }

    QUESTION {
        Long id PK
        Long exam_id FK
        String statement
        String type
        Integer points
    }

    EXAM_CALL {
        Long id PK
        Long exam_id FK
        LocalDateTime startDate
        LocalDateTime endDate
        Integer totalCapacity
        Integer currentEnrollment
    }

    EXAM_ATTEMPT {
        Long id PK
        Long exam_call_id FK
        Long student_id FK
        LocalDateTime startedAt
        LocalDateTime submittedAt
        String status
        BigDecimal finalScore
    }

    ATTEMPT_QUESTION {
        Long id PK
        Long attempt_id FK
        Long question_id FK
        Integer questionOrder
        String answerText
        BigDecimal awardedScore
        String reviewComment
    }

    ROLE ||--o{ USER : "tiene"
    USER ||--o{ EXAM : "crea (profesor)"
    EXAM ||--o{ QUESTION : "contiene"
    EXAM ||--o{ EXAM_CALL : "se publica en"
    EXAM_CALL ||--o{ EXAM_ATTEMPT : "genera intentos"
    USER ||--o{ EXAM_ATTEMPT : "rinde (estudiante)"
    EXAM_ATTEMPT ||--o{ ATTEMPT_QUESTION : "respuestas"
    QUESTION ||--o{ ATTEMPT_QUESTION : "instancia"
```

## Diagrama de Casos de Uso (MVP)

```mermaid
flowchart LR
    P[Profesor]
    E[Estudiante]

    UC1((Crear examen))
    UC2((Agregar preguntas))
    UC3((Publicar convocatoria))
    UC4((Revisar y calificar intento))
    UC5((Ver historial de examenes))

    UC6((Unirse a examen abierto))
    UC7((Responder preguntas))
    UC8((Enviar examen))
    UC9((Ver resultado))

    P --> UC1
    P --> UC2
    P --> UC3
    P --> UC4
    P --> UC5

    E --> UC6
    E --> UC7
    E --> UC8
    E --> UC9
```

## Uso de IA (OpenAI / Claude / DeepSeek)

La IA no es obligatoria para el MVP, pero puede aportar valor en:

1. Asistencia al profesor para crear preguntas.
   - Generar borradores por tema y dificultad.
2. Pre-correccion de respuestas abiertas.
   - Sugerir puntaje y feedback (el profesor confirma la nota final).
3. Feedback automatico al estudiante.
   - Explicacion breve de fortalezas y puntos de mejora.

### Recomendacion practica

- Empezar con IA en modo asistente (sugerencias), no en modo automatico.
- Guardar trazabilidad: prompt, respuesta del modelo y decision final del profesor.
- Implementar un servicio desacoplado en backend (`ai-provider`) para poder cambiar entre OpenAI, Claude o DeepSeek.
