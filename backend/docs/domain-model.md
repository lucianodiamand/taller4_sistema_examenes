# Domain Model and Diagrams

## MVP Scope

- Autenticacion con roles: `PROFESOR` y `ESTUDIANTE`
- Profesor crea examenes y preguntas
- Profesor publica convocatoria con ventana de tiempo
- Estudiante rinde solo en ventana abierta
- Intento unico por estudiante con orden aleatorio de preguntas
- Profesor revisa y califica sin modificar respuestas

## Entity Relationship Diagram

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

## Use Case Diagram

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
