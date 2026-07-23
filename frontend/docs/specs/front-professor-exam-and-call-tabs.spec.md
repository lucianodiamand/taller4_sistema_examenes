# Frontend Professor Exam + Call (Tabs) Specification

## Overview

Define professor flow to create exams and create exam calls using Angular Material, without overloading UI. Flow uses tabs to separate concerns and follows backend contract from `ExamController`.

## Goal

- Let professor list own exams.
- Let professor create new exam with questions.
- Let professor create exam call for existing exam.
- Keep one screen, split by tabs, low cognitive load.

## Scope

### In Scope

- New professor page under app protected routes.
- 3-tab UX with Material components.
- API integration for `GET /api/exams`, `POST /api/exams`, `POST /api/exams/{examId}/calls`.
- Form validation mapped to backend constraints.
- Success/error/loading states.

### Out of Scope

- Backend changes.
- Student flows.
- Exam grading/correction flows.
- File attachments or rich text editors.

## Actors

- Primary: `PROFESSOR`.
- Secondary: `ADMIN` (same access as professor route in current app policy).

## Route and Access

- New route: `/app/professor/exams`.
- Guard: authenticated + `roleGuard` with `PROFESSOR | ADMIN`.
- Entry points:
  - Professor home CTA: `Mis examenes`.
  - Direct URL navigation.

## Backend Contract (Source of Truth)

From `backend/src/main/java/com/exam_system/exam/api/ExamController.java`:

- `GET /api/exams`
  - Auth required.
  - Authority: `exams.create`.
  - Response item:
    - `id: number`
    - `title: string`
    - `description: string | null`
    - `durationMinutes: number`
    - `professorId: number | null`

- `POST /api/exams`
  - Request:
    - `title` required, not blank.
    - `description` optional, max 2000 chars.
    - `durationMinutes` required, min 1.
    - `questions` required, at least 1.
    - question fields:
      - `statement` required.
      - `type` required: `OPEN | TRUE_FALSE | MULTIPLE_CHOICE`.
      - `points` required, min 0.
  - Response: created exam + created questions.

- `POST /api/exams/{examId}/calls`
  - Path param: `examId`.
  - Request:
    - `startDate` required (`LocalDateTime`).
    - `endDate` required (`LocalDateTime`), must be after `startDate`.
    - `totalCapacity` optional; if present min 1; null means unlimited.
  - Response:
    - `id`, `examId`, `startDate`, `endDate`, `totalCapacity`, `currentEnrollment`.

Error envelope from `backend/src/main/java/com/exam_system/shared/api/ApiExceptionHandler.java`:

- `400` -> `{ error: "VALIDATION_ERROR" | "BAD_REQUEST", message }`
- `403` -> `{ error: "FORBIDDEN", message: "Access denied" }`
- `404` -> `{ error: "NOT_FOUND", message }`
- `409` -> `{ error: "CONFLICT", message }` (generic handler exists)

## UX Model (Tab-based)

Use one page with `MatTabGroup` and 3 tabs:

1. `Mis examenes`
   - Read-only list of professor exams.
   - Fast scan cards/table: title, duration, short description.
   - Primary action on each row/card: `Crear llamado`.

2. `Crear examen`
   - Exam metadata + dynamic questions.
   - Dedicated save action.
   - On success:
     - show snackbar success,
     - reset form,
     - refresh exam list,
     - move focus to `Mis examenes` tab (recommended default).

3. `Crear llamado`
   - Form to create call for selected exam.
   - First control: exam selector (required).
   - Date/time + optional capacity.
   - On success:
     - show snackbar,
     - keep selected exam,
     - clear date/capacity fields.

### Why tabs (UX rationale)

- Split high-density flow into focused tasks.
- Keep context in one route.
- Avoid long single-form scroll fatigue.
- Reduce accidental mistakes from mixed actions.

## UI Components (Angular Material)

- Layout: `mat-card` shell with page title/subtitle.
- Tabs: `mat-tab-group`.
- Forms: `mat-form-field`, `matInput`, `mat-select`, `mat-error`.
- Actions: `mat-flat-button` (primary), `mat-stroked-button` (secondary), `mat-icon-button` (question remove).
- Feedback: `mat-progress-spinner` inline in submit buttons, `MatSnackBar` for async outcomes.

## UI/UX Best Practices (Required)

- Progressive disclosure: hide complexity by tab separation.
- One primary CTA per tab.
- Keep labels explicit, avoid placeholder-only forms.
- Disable submit while saving.
- Preserve typed data when switching tabs unless user explicitly resets.
- Surface backend error `message` near form and via snackbar.
- Keyboard support:
  - tab headers focusable,
  - all buttons reachable,
  - Enter submits focused form.
- Mobile first:
  - stacked fields,
  - no horizontal scroll,
  - touch targets >= 44px.

## Data Contracts (Frontend)

```ts
type QuestionType = 'OPEN' | 'TRUE_FALSE' | 'MULTIPLE_CHOICE';

interface ExamResponse {
  id: number;
  title: string;
  description: string | null;
  durationMinutes: number;
  professorId: number | null;
}

interface CreateExamRequest {
  title: string;
  description: string | null;
  durationMinutes: number;
  questions: Array<{
    statement: string;
    type: QuestionType;
    points: number;
  }>;
}

interface CreateExamCallRequest {
  startDate: string; // yyyy-MM-ddTHH:mm:ss
  endDate: string;   // yyyy-MM-ddTHH:mm:ss
  totalCapacity: number | null;
}
```

## Forms and Validation

### Crear examen

- `title`: required.
- `description`: optional, max 2000.
- `durationMinutes`: required, integer >= 1.
- `questions`: min length 1.
- each question:
  - `statement` required.
  - `type` required.
  - `points` required, integer >= 0.

### Crear llamado

- `examId`: required.
- `startDate`: required.
- `endDate`: required and `endDate > startDate` (custom validator).
- `totalCapacity`: optional; if filled then integer >= 1.

## State Model (Signals)

- `loadingExams`: load list state.
- `savingExam`: create exam state.
- `savingCall`: create call state.
- `selectedTab`: `0 | 1 | 2`.
- `selectedExamIdForCall`: `number | null`.
- `apiError`: string.
- `apiMessage`: string.

## Interaction Flows

### Flow A - Load Professor Exams

1. User opens `/app/professor/exams`.
2. App calls `GET /api/exams`.
3. Exams render in `Mis examenes` tab.
4. Empty state shown if list empty.

Acceptance:

- List only includes authenticated professor exams.
- Loading/skeleton visible while request pending.

### Flow B - Create Exam Success

1. User switches to `Crear examen`.
2. Completes form + at least one question.
3. Submits.
4. App calls `POST /api/exams`.
5. On success: snackbar + list refresh + switch to `Mis examenes`.

Acceptance:

- Request body matches backend contract.
- New exam appears in list after save.

### Flow C - Create Call Success

1. User opens `Crear llamado` tab.
2. Picks exam.
3. Sets start/end and optional capacity.
4. Submits.
5. App calls `POST /api/exams/{examId}/calls`.
6. On success: snackbar + call form partial reset.

Acceptance:

- Date validation prevents invalid submit (`end <= start`).
- Empty capacity sent as `null`.

### Flow D - Validation / Business Error

1. Backend returns `400`, `404`, `403`, or `409`.
2. App shows backend `message`.
3. Keep user data in form for quick fix/retry.

Acceptance:

- No silent failures.
- No full page reset after error.

## Accessibility Requirements

- Semantic structure: `main`, heading hierarchy `h1` then tab content headings.
- Error region uses `role="alert"` and/or `aria-live="polite"`.
- Contrast meets WCAG AA defaults from Material theme.
- Tab switching and submit actions fully keyboard operable.

## Frontend Architecture

- Feature folder: `frontend/src/app/professor/`.
- Service split:
  - `ProfessorExamsApiService` handles HTTP only.
  - Component handles view state/forms.
- Reuse global auth interceptor; do not duplicate manual auth headers.
- Keep enums/constants in existing contracts file when route constants are added.

## Test Matrix (Minimum)

| Scenario | Unit | Integration |
| --- | --- | --- |
| Load exams list success | [ ] | [ ] |
| Create exam payload valid | [ ] | [ ] |
| Question array min=1 enforced | [ ] | [ ] |
| Create call endDate validator | [ ] | [ ] |
| Empty capacity maps to null | [ ] | [ ] |
| API error message surfaced | [ ] | [ ] |

## Definition of Done

- Route available and guarded for professor/admin.
- 3-tab page implemented with Angular Material.
- `GET /api/exams`, `POST /api/exams`, `POST /api/exams/{examId}/calls` wired.
- Validations aligned with backend.
- Loading/success/error states visible and usable.
- Tests for core flows added and passing.
