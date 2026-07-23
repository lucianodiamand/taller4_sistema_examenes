# Demo local estudiante

Objetivo: abrir `http://localhost:4200`, ingresar como estudiante y ver:

- convocatoria abierta para resolver,
- intento previo corregido en historial,
- flujo completo de guardar y enviar.

## Credenciales demo

- Estudiante: `estudiante` / `estudiante123`
- Profesor: `profesor` / `profesor123`
- Admin: `admin` / `admin123`

## Datos que se crean en perfil dev

Al iniciar backend con perfil `dev`:

- Examen: `[DEMO] Simulacro de Programacion IV`
- Convocatoria abierta para iniciar intento desde `/app/student`
- Convocatoria histórica con intento `GRADED` del estudiante (con puntaje y comentario)

## Ejecutar demo

1. Backend

```bash
cd backend
bash ./mvnw spring-boot:run
```

2. Frontend

```bash
cd frontend
pnpm install
pnpm start
```

3. Navegar

- Abrir `http://localhost:4200`
- Login con `estudiante` / `estudiante123`
- Ir a `/app/student`
- En **Disponibles** iniciar `[DEMO] Simulacro de Programacion IV`
- Guardar respuestas
- Enviar examen
- En **Mis resoluciones** verificar:
  - intento demo viejo en `GRADED`
  - intento nuevo en `SUBMITTED`

## Verificación backend automática

```bash
cd backend
bash ./mvnw -Dtest=StudentExamFlowE2ETest test
```

Caso validado por test:

- login estudiante
- listar disponibles
- iniciar intento
- guardar respuestas
- enviar intento
- leer historial con estado `SUBMITTED` nuevo y `GRADED` previo
