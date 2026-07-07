# TODO MVP - Sistema de Examenes (2 semanas)

## Objetivos del MVP

- Autenticacion JWT con roles `profesor` y `estudiante`.
- Profesor: crear examen.
- Estudiante: resolver examen.
- Profesor: corregir examen.
- Historial de examenes.
- Reglas de visualizacion:
  - Un estudiante solo puede ver sus propios examenes/resoluciones.
  - Un profesor solo puede ver los examenes que creo y sus resoluciones.

## Division de tareas (4 integrantes, enfoque fullstack)

### Integrante 1 - Auth + Base tecnica | 👤 **STEF**

- Backend:
  - Implementar login con JWT (registro opcional si ya hay usuarios precargados).
  - Definir middleware de autenticacion y autorizacion por rol.
  - Crear seeds/datos iniciales de usuarios `profesor` y `estudiante`.
- Frontend:
  - Pantalla de login y manejo de sesion (token, logout, guardado seguro).
  - Guardas de rutas por rol.
- Integracion:
  - Documentar flujo de autenticacion para el equipo.
  - Ayudar al resto a proteger endpoints y vistas.

### Integrante 2 - Crear examen (profesor) | 👤 **ROQUE**

- Backend:
  - Modelo y endpoints para crear/listar examenes del profesor autenticado.
  - Validaciones basicas (titulo, descripcion, preguntas, puntaje).
- Frontend:
  - Formulario de creacion de examen (preguntas y opciones/respuesta esperada).
  - Vista "Mis examenes" para profesor.
- Integracion:
  - Alinear contrato API con integrante 3 (resolver) e integrante 4 (corregir/historial).
  - Ayudar a probar casos de permisos.

### Integrante 3 - Resolver examen (estudiante) | 👤 **EMA**

- Backend:
  - Endpoints para listar examenes disponibles y enviar resolucion.
  - Persistencia de respuestas por estudiante y examen.
  - Regla: evitar que un estudiante vea/envi e resoluciones de otros.
- Frontend:
  - Pantalla de listado de examenes disponibles para estudiante.
  - Flujo de resolucion y envio.
  - Vista "Mis resoluciones" (historial de estudiante).
- Integracion:
  - Probar integracion end-to-end con examenes creados por integrante 2.
  - Coordinar con integrante 4 para estado "pendiente/corregido".

### Integrante 4 - Corregir examen + Historial + Reglas de visualizacion | 👤 **VALEN**

- Backend:
  - Endpoints para que profesor vea resoluciones de SUS examenes y corrija.
  - Calificacion/feedback y cambio de estado de resolucion.
  - Aplicar reglas de visibilidad en todos los endpoints clave.
- Frontend:
  - Vista de correccion para profesor (lista de resoluciones + detalle + nota).
  - Historial para profesor (examenes creados y resultados) y para estudiante (sus notas).
- Integracion:
  - Tests de autorizacion (profesor vs estudiante, acceso indebido).
  - Checklist final de seguridad y consistencia de permisos.

## Plan de trabajo (2 semanas)

### Semana 1 (base funcional)

- Dia 1-2: Definir modelos/contratos API y tablero de tareas.
- Dia 2-4: Auth + crear examen + resolver examen (version inicial).
- Dia 5: Integracion parcial entre los 4, demo interna y ajuste de bugs.

### Semana 2 (cierre MVP)

- Dia 6-8: Corregir examen + historial completo + reglas de visualizacion finas.
- Dia 9: QA cruzado (cada integrante prueba modulo de otro).
- Dia 10: Correcciones finales, datos demo, documentacion y presentacion.

## Criterios de listo (Definition of Done)

- Login JWT funciona para profesor y estudiante.
- Profesor puede crear examenes y ver solo los suyos.
- Estudiante puede resolver examenes y ver solo sus resultados.
- Profesor puede corregir resoluciones de sus examenes.
- Historial visible segun rol y reglas de acceso cumplidas.
- Flujo demo completo funcionando de punta a punta.
