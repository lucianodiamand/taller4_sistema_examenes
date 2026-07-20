# Domain Model and Diagrams

## MVP Scope

- Autenticacion con roles: `PROFESSOR` y `STUDENT`
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

    ROLE ||--o{ USER : "has"
    USER ||--o{ EXAM : "creates (professor)"
    EXAM ||--o{ QUESTION : "contains"
    EXAM ||--o{ EXAM_CALL : "published in"
    EXAM_CALL ||--o{ EXAM_ATTEMPT : "generates attempts"
    USER ||--o{ EXAM_ATTEMPT : "takes (student)"
    EXAM_ATTEMPT ||--o{ ATTEMPT_QUESTION : "answers"
    QUESTION ||--o{ ATTEMPT_QUESTION : "instance"
```

## Use Case Diagram

```mermaid
flowchart LR
    P[Profesor]
    E[Estudiante]

    UC1((Create exam))
    UC2((Add questions))
    UC3((Publish exam call))
    UC4((Review and grade attempt))
    UC5((View exam history))

    UC6((Join open exam))
    UC7((Answer questions))
    UC8((Submit exam))
    UC9((View result))

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
