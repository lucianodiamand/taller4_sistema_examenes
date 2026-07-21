# Flujo frontend de resolución de exámenes (EMA)

Este módulo cubre la parte del estudiante definida para EMA en `todo.md`. La implementación se mantuvo simple y concentrada en una sola pantalla para evitar agregar componentes y carpetas innecesarias.

## Ruta principal

`/app/student`

La ruta carga de forma diferida el componente `StudentExamsComponent`.

## Archivos principales

- `src/app/student/student-exams.component.ts`: lógica de la pantalla.
- `src/app/student/student-exams.component.html`: listado, historial y resolución.
- `src/app/student/student-exams.component.scss`: estilos responsive.
- `src/app/student/student-exams.service.ts`: contratos y llamadas al backend.

## Funcionalidades

### Exámenes disponibles

La pestaña **Disponibles** muestra las convocatorias habilitadas para el estudiante.

Desde cada tarjeta se puede:

- Comenzar un intento nuevo.
- Continuar un intento que ya se encuentre en curso.
- Consultar duración, profesor, fecha de cierre y cupos disponibles.

### Resolución del examen

Al comenzar o continuar un examen se abre un panel con sus preguntas.

Mientras el intento tenga estado `IN_PROGRESS`, el estudiante puede:

- Completar o modificar respuestas.
- Guardar el progreso sin enviar.
- Enviar definitivamente el examen.

Antes del envío se valida que todas las preguntas tengan respuesta y se solicita una confirmación. Una vez enviado, el intento deja de ser editable.

### Mis resoluciones

La pestaña **Mis resoluciones** muestra el historial del estudiante con los siguientes estados:

- `IN_PROGRESS`: en curso.
- `SUBMITTED`: pendiente de corrección.
- `GRADED`: corregido.

Cuando una resolución está corregida se muestran la nota final, el puntaje obtenido por pregunta y los comentarios del profesor.

## Endpoints utilizados

- `GET /api/student/exams/available`
- `POST /api/student/exams/{examCallId}/attempts`
- `GET /api/student/attempts`
- `GET /api/student/attempts/{attemptId}`
- `PUT /api/student/attempts/{attemptId}/answers`
- `POST /api/student/attempts/{attemptId}/submit`

## Autenticación

El servicio reutiliza la sesión creada por el módulo de autenticación existente.

La sesión se obtiene desde la clave:

`exam.auth.session`

Cuando contiene un `accessToken`, se envía en el encabezado:

`Authorization: Bearer <accessToken>`

El módulo no reemplaza ni modifica el flujo de login existente.

## Consideración actual

El contrato del backend recibe las respuestas mediante el campo `answerText`. Por ese motivo, la versión actual utiliza un campo de texto para todos los tipos de pregunta, incluyendo opción múltiple y verdadero o falso.

Esta decisión mantiene el frontend compatible con el backend actual y evita agregar lógica que todavía no está contemplada en el contrato de la API.
