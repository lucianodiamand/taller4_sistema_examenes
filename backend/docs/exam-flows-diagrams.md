# Diagramas de flujo - dominio Exam

Mismo formato que `auth-users-diagrams.md`. Cubre los 3 casos de uso principales del dominio de examenes: crear examen (profesor), resolver examen (estudiante), corregir examen (profesor).

## Sequence diagram - Crear examen (Integrante 2 / Roque, backend cubierto por Valen)

```mermaid
sequenceDiagram
    autonumber
    actor Client
    participant ExamController
    participant ExamService
    participant ExamRepository
    participant QuestionRepository
    participant ExamCallRepository
    participant UserRepository

    Note over Client,ExamController: Todos los endpoints requieren hasRole('PROFESSOR') + exams.create.<br/>El professorId sale siempre del JWT (CurrentUser), nunca del body.

    alt POST /api/exams
        Client->>ExamController: create(title, description, durationMinutes, questions[])
        ExamController->>ExamService: create(..., professorId, questions)
        ExamService->>UserRepository: findById(professorId)
        ExamService->>ExamRepository: save(Exam)
        loop por cada pregunta
            ExamService->>QuestionRepository: save(Question)
        end
        ExamController-->>Client: 201 ExamCreatedResponse
    end

    alt GET /api/exams
        Client->>ExamController: findMine()
        ExamController->>ExamService: findAllForProfessor(professorId)
        ExamService->>ExamRepository: findByProfessorId(professorId)
        ExamController-->>Client: 200 List~ExamResponse~
    end

    alt POST /api/exams/{examId}/calls
        Client->>ExamController: createCall(examId, startDate, endDate, totalCapacity)
        ExamController->>ExamService: createCall(examId, professorId, ...)
        ExamService->>ExamRepository: findByIdAndProfessorId(examId, professorId)
        Note right of ExamService: 404 si el examen no existe o no es del profesor
        ExamService->>ExamCallRepository: save(ExamCall)
        ExamController-->>Client: 201 ExamCallResponse
    end

    alt GET /api/exams/{examId}/calls
        Client->>ExamController: findCalls(examId)
        ExamController->>ExamService: findCallsForExam(examId, professorId)
        ExamService->>ExamRepository: findByIdAndProfessorId(examId, professorId)
        ExamService->>ExamCallRepository: findByExamIdOrderByStartDateDesc(examId)
        ExamController-->>Client: 200 List~ExamCallResponse~
    end
```

## Sequence diagram - Resolver examen (Integrante 3 / Ema)

```mermaid
sequenceDiagram
    autonumber
    actor Client
    participant StudentExamController
    participant StudentExamService
    participant ExamCallRepository
    participant ExamAttemptRepository
    participant QuestionRepository

    Note over Client,StudentExamController: Requiere hasRole('STUDENT') + exams.solve.<br/>El studentId siempre sale del JWT.

    alt GET /api/student/exams/available
        Client->>StudentExamController: findAvailableExams()
        StudentExamController->>StudentExamService: findAvailableExams()
        StudentExamService->>ExamCallRepository: findOpenCalls(now)
        StudentExamService->>ExamAttemptRepository: findByStudentIdAndExamCallIdIn(studentId, callIds)
        StudentExamController-->>Client: 200 List~AvailableExamView~
    end

    alt POST /api/student/exams/{examCallId}/attempts
        Client->>StudentExamController: startAttempt(examCallId)
        StudentExamController->>StudentExamService: startAttempt(examCallId)
        StudentExamService->>ExamCallRepository: findByIdForUpdate(examCallId)
        Note right of StudentExamService: lock pesimista, valida ventana horaria y cupo
        StudentExamService->>ExamAttemptRepository: findByExamCallIdAndStudentId(...)
        StudentExamService->>QuestionRepository: findByExamIdOrderByIdAsc(examId)
        Note right of StudentExamService: preguntas se mezclan al azar por intento
        StudentExamService->>ExamAttemptRepository: saveAndFlush(ExamAttempt IN_PROGRESS)
        StudentExamController-->>Client: 200 AttemptDetailView
    end

    alt PUT /api/student/attempts/{attemptId}/answers
        Client->>StudentExamController: saveAnswers(attemptId, answers[])
        StudentExamController->>StudentExamService: saveAnswers(attemptId, answers)
        StudentExamService->>ExamAttemptRepository: findByIdAndStudentId(attemptId, studentId)
        StudentExamService->>ExamAttemptRepository: saveAndFlush(attempt)
        StudentExamController-->>Client: 200 AttemptDetailView
    end

    alt POST /api/student/attempts/{attemptId}/submit
        Client->>StudentExamController: submitAttempt(attemptId, answers[])
        StudentExamController->>StudentExamService: submitAttempt(attemptId, answers)
        StudentExamService->>ExamAttemptRepository: findByIdAndStudentId(attemptId, studentId)
        Note right of StudentExamService: valida que todas las preguntas tengan respuesta
        StudentExamService->>ExamAttemptRepository: saveAndFlush(attempt SUBMITTED)
        StudentExamController-->>Client: 200 AttemptDetailView
    end

    alt GET /api/student/attempts
        Client->>StudentExamController: findMyAttempts()
        StudentExamController->>StudentExamService: findMyAttempts()
        StudentExamService->>ExamAttemptRepository: findByStudentIdOrderByStartedAtDesc(studentId)
        StudentExamController-->>Client: 200 List~AttemptSummaryView~
    end
```

## Sequence diagram - Corregir examen (Integrante 4 / Valen)

```mermaid
sequenceDiagram
    autonumber
    actor Client
    participant GradingController
    participant GradingService
    participant ExamAttemptRepository

    Note over Client,GradingController: Profesor: hasRole('PROFESSOR') + exams.grade.<br/>Estudiante: hasRole('STUDENT') + exam.results.read.self / exam.validations.read.self

    alt GET /api/grading/attempts (profesor)
        Client->>GradingController: getAttemptsForProfessor()
        GradingController->>GradingService: getAttemptsForProfessor(professorId)
        GradingService->>ExamAttemptRepository: findAllByProfessorId(professorId)
        GradingController-->>Client: 200 List~AttemptSummaryResponse~
    end

    alt GET /api/grading/attempts/{attemptId} (profesor)
        Client->>GradingController: getAttemptDetail(attemptId)
        GradingController->>GradingService: getAttemptForProfessor(attemptId, professorId)
        GradingService->>ExamAttemptRepository: findByIdAndProfessorId(attemptId, professorId)
        Note right of GradingService: 404 si la resolucion no es de un examen propio
        GradingController-->>Client: 200 AttemptDetailResponse
    end

    alt PATCH /api/grading/attempts/{attemptId}/questions/{questionId} (profesor)
        Client->>GradingController: gradeQuestion(attemptId, questionId, awardedScore, reviewComment)
        GradingController->>GradingService: gradeQuestion(...)
        GradingService->>ExamAttemptRepository: findByIdAndProfessorId(attemptId, professorId)
        Note right of GradingService: exige estado SUBMITTED
        GradingService->>ExamAttemptRepository: save(attempt)
        GradingController-->>Client: 200 QuestionGradeResponse
    end

    alt POST /api/grading/attempts/{attemptId}/close (profesor)
        Client->>GradingController: closeGrading(attemptId)
        GradingController->>GradingService: closeGrading(attemptId, professorId)
        GradingService->>ExamAttemptRepository: findByIdAndProfessorId(attemptId, professorId)
        Note right of GradingService: finalScore = suma de awardedScore, status = GRADED
        GradingService->>ExamAttemptRepository: save(attempt)
        GradingController-->>Client: 200 AttemptSummaryResponse
    end

    alt GET /api/grading/my-results (estudiante)
        Client->>GradingController: getMyResults()
        GradingController->>GradingService: getResultsForStudent(studentId)
        GradingService->>ExamAttemptRepository: findByStudentIdOrderByStartedAtDesc(studentId)
        GradingController-->>Client: 200 List~StudentResultResponse~
    end

    alt GET /api/grading/my-validations (estudiante)
        Client->>GradingController: getMyValidations()
        GradingController->>GradingService: getValidationCommentsForStudent(studentId)
        GradingService->>ExamAttemptRepository: findByStudentId(studentId)
        GradingController-->>Client: 200 List~ValidationCommentResponse~
    end
```

## ERD - dominio Exam

```mermaid
erDiagram
    USER {
        BIGINT id PK
        VARCHAR name
        VARCHAR username UK
        BIGINT role_id FK
    }

    EXAM {
        BIGINT id PK
        VARCHAR title
        VARCHAR description
        INT duration_minutes
        BIGINT professor_id FK
    }

    QUESTION {
        BIGINT id PK
        BIGINT exam_id FK
        TEXT statement
        VARCHAR type "MULTIPLE_CHOICE | TRUE_FALSE | OPEN"
        INT points
    }

    EXAM_CALL {
        BIGINT id PK
        BIGINT exam_id FK
        TIMESTAMP start_date
        TIMESTAMP end_date
        INT total_capacity "null = sin limite"
        INT current_enrollment
    }

    EXAM_ATTEMPT {
        BIGINT id PK
        BIGINT exam_call_id FK
        BIGINT student_id FK
        TIMESTAMP started_at
        TIMESTAMP submitted_at
        VARCHAR status "IN_PROGRESS | SUBMITTED | GRADED"
        DECIMAL final_score
    }

    ATTEMPT_QUESTION {
        BIGINT id PK
        BIGINT attempt_id FK
        BIGINT question_id FK
        INT question_order
        TEXT answer_text
        DECIMAL awarded_score
        TEXT review_comment
    }

    USER ||--o{ EXAM : "crea (profesor)"
    EXAM ||--o{ QUESTION : "tiene"
    EXAM ||--o{ EXAM_CALL : "abre convocatorias"
    EXAM_CALL ||--o{ EXAM_ATTEMPT : "recibe intentos"
    USER ||--o{ EXAM_ATTEMPT : "resuelve (estudiante)"
    EXAM_ATTEMPT ||--o{ ATTEMPT_QUESTION : "contiene"
    QUESTION ||--o{ ATTEMPT_QUESTION : "respondida como"
```

## Notas

- Mismo criterio que `auth-users-diagrams.md`: nombres de entidades/campos en ingles, texto de flujo en espanol.
- El estado GRADED de `EXAM_ATTEMPT` es lo unico visible para el estudiante con nota final (`finalScore`); mientras esta en SUBMITTED, el estudiante ve el intento pero sin nota.
- `total_capacity` nulo en `EXAM_CALL` significa cupo ilimitado (ver `StudentExamService.hasCapacity`).
