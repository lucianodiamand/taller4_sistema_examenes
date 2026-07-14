# Flujo backend de resolución de exámenes (EMA)

Este módulo cubre la parte del estudiante indicada en `todo.md`. Todas las rutas requieren un JWT de un usuario con rol `STUDENT` y permiso `exams.solve`.

## Endpoints

### Listar exámenes disponibles

`GET /api/student/exams/available`

Devuelve convocatorias que estén dentro de su fecha de inicio y fin. No muestra exámenes que el estudiante ya envió o que ya fueron corregidos. Si existe un intento en curso, devuelve también su `attemptId` para poder continuarlo.

### Iniciar o continuar un intento

`POST /api/student/exams/{examCallId}/attempts`

- Verifica horario y cupo.
- Impide más de un intento por estudiante y convocatoria.
- Copia las preguntas al intento con un orden aleatorio.
- Si el intento ya estaba en curso, devuelve el mismo sin crear otro.

### Guardar respuestas sin enviar

`PUT /api/student/attempts/{attemptId}/answers`

```json
{
  "answers": [
    {
      "attemptQuestionId": 12,
      "answerText": "Mi respuesta"
    }
  ]
}
```

Permite guardar avances mientras el intento siga abierto. Cada `attemptQuestionId` debe pertenecer al intento del estudiante autenticado.

### Enviar resolución

`POST /api/student/attempts/{attemptId}/submit`

Usa el mismo cuerpo que el guardado de respuestas. Antes de enviar comprueba que todas las preguntas tengan respuesta. Al completarse, el estado pasa de `IN_PROGRESS` a `SUBMITTED` y ya no admite cambios.

### Ver historial propio

- `GET /api/student/attempts`
- `GET /api/student/attempts/{attemptId}`

Las consultas incluyen siempre el id del usuario autenticado. Por eso un estudiante no puede leer una resolución ajena aunque conozca su identificador.

## Estados

- `IN_PROGRESS`: resolución iniciada y editable.
- `SUBMITTED`: enviada y pendiente de corrección.
- `GRADED`: corregida por el profesor.

La nota, el puntaje por pregunta y los comentarios se exponen al estudiante solamente cuando el intento está en estado `GRADED`.

## Reglas aplicadas

- Sólo el estudiante dueño puede consultar o modificar su resolución.
- La convocatoria debe estar abierta.
- Se respeta tanto la fecha de cierre como la duración del examen.
- El cupo se actualiza al crear un intento nuevo.
- No se aceptan preguntas que no pertenezcan al intento.
- No se aceptan respuestas duplicadas en una misma petición.
- Una resolución enviada no puede volver a editarse.
