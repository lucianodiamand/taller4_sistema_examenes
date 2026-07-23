import { HttpErrorResponse } from '@angular/common/http';
import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import {
  AbstractControl,
  FormArray,
  FormBuilder,
  FormControl,
  FormGroup,
  ReactiveFormsModule,
  ValidationErrors,
  Validators,
} from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatTabsModule } from '@angular/material/tabs';
import { Router, RouterLink } from '@angular/router';
import { finalize, take } from 'rxjs';

import { APP_ROUTES, AUTH_UI } from '../auth/contracts/auth.contracts';
import { AuthService } from '../auth/services/auth.service';
import {
  type CreateExamCallRequest,
  type CreateExamRequest,
  type ExamCallResponse,
  type ExamResponse,
  ProfessorExamsApiService,
  type QuestionType,
} from './professor-exams-api.service';

type ProfessorTabIndex = 0 | 1 | 2;

type QuestionFormGroup = FormGroup<{
  statement: FormControl<string>;
  type: FormControl<QuestionType>;
  points: FormControl<number>;
}>;

function callDateRangeValidator(control: AbstractControl): ValidationErrors | null {
  const value = control.value as { startDate?: string | null; endDate?: string | null };
  const startDate = value.startDate;
  const endDate = value.endDate;
  if (!startDate || !endDate) {
    return null;
  }

  const start = Date.parse(startDate);
  const end = Date.parse(endDate);
  if (Number.isNaN(start) || Number.isNaN(end)) {
    return null;
  }

  return end > start ? null : { dateRange: true };
}

@Component({
  selector: 'app-professor-exams',
  imports: [
    ReactiveFormsModule,
    DatePipe,
    RouterLink,
    MatCardModule,
    MatTabsModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatSelectModule,
    MatProgressSpinnerModule,
    MatSnackBarModule,
  ],
  templateUrl: './professor-exams.component.html',
  styleUrl: './professor-exams.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ProfessorExamsComponent implements OnInit {
  private readonly formBuilder = inject(FormBuilder);
  private readonly professorExamsApi = inject(ProfessorExamsApiService);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  private readonly snackBar = inject(MatSnackBar);

  protected readonly routes = APP_ROUTES;
  protected readonly tabs = {
    exams: 0,
    createExam: 1,
    createCall: 2,
  } as const;

  protected readonly exams = signal<ExamResponse[]>([]);
  protected readonly selectedTabIndex = signal<ProfessorTabIndex>(0);
  protected readonly loadingExams = signal(false);
  protected readonly loadingCalls = signal(false);
  protected readonly savingExam = signal(false);
  protected readonly savingCall = signal(false);
  protected readonly examCalls = signal<ExamCallResponse[]>([]);
  protected readonly loadError = signal<string | null>(null);
  protected readonly createExamError = signal<string | null>(null);
  protected readonly createCallError = signal<string | null>(null);
  protected readonly callsError = signal<string | null>(null);

  protected readonly examForm = this.formBuilder.group({
    title: ['', [Validators.required]],
    description: ['', [Validators.maxLength(2000)]],
    durationMinutes: [null as number | null, [Validators.required, Validators.min(1)]],
    questions: this.formBuilder.array<QuestionFormGroup>([this.buildQuestionGroup()], [Validators.minLength(1)]),
  });

  protected readonly callForm = this.formBuilder.group(
    {
      examId: [null as number | null, [Validators.required]],
      startDate: ['', [Validators.required]],
      endDate: ['', [Validators.required]],
      totalCapacity: [null as number | null, [Validators.min(1)]],
    },
    {
      validators: [callDateRangeValidator],
    }
  );

  ngOnInit(): void {
    this.loadExams();
  }

  protected questionControls(): QuestionFormGroup[] {
    return this.questions.controls;
  }

  protected addQuestion(): void {
    this.questions.push(this.buildQuestionGroup());
  }

  protected removeQuestion(index: number): void {
    if (this.questions.length <= 1) {
      return;
    }
    this.questions.removeAt(index);
  }

  protected selectTab(index: number): void {
    if (index === 0 || index === 1 || index === 2) {
      this.selectedTabIndex.set(index);
    }
  }

  protected loadExams(): void {
    this.loadingExams.set(true);
    this.loadError.set(null);

    this.professorExamsApi
      .findMine()
      .pipe(
        take(1),
        finalize(() => this.loadingExams.set(false))
      )
      .subscribe({
        next: (exams) => {
          this.exams.set(exams);
          const selectedExamId = this.callForm.controls.examId.value;
          if (selectedExamId) {
            this.loadCallsForExam(selectedExamId);
          }
        },
        error: (error: unknown) => {
          this.loadError.set(this.errorMessage(error, 'No se pudieron cargar tus examenes.'));
        },
      });
  }

  protected onCallExamChanged(examId: number | null): void {
    if (!examId) {
      this.examCalls.set([]);
      this.callsError.set(null);
      return;
    }

    this.loadCallsForExam(examId);
  }

  protected jumpToCall(exam: ExamResponse): void {
    this.callForm.controls.examId.setValue(exam.id);
    this.createCallError.set(null);
    this.onCallExamChanged(exam.id);
    this.selectTab(this.tabs.createCall);
  }

  protected submitExam(): void {
    if (this.questions.length === 0) {
      this.questions.markAllAsTouched();
      this.questions.updateValueAndValidity();
      return;
    }

    if (this.examForm.invalid || this.savingExam()) {
      this.examForm.markAllAsTouched();
      return;
    }

    const payload = this.toCreateExamRequest();
    this.savingExam.set(true);
    this.createExamError.set(null);

    this.professorExamsApi
      .createExam(payload)
      .pipe(
        take(1),
        finalize(() => this.savingExam.set(false))
      )
      .subscribe({
        next: (createdExam) => {
          this.snackBar.open('Examen creado correctamente.', 'Cerrar', { duration: AUTH_UI.snackbarDurationMs });
          this.resetExamForm();
          this.callForm.controls.examId.setValue(createdExam.id);
          this.selectTab(this.tabs.exams);
          this.loadExams();
        },
        error: (error: unknown) => {
          this.createExamError.set(this.errorMessage(error, 'No se pudo crear el examen.'));
        },
      });
  }

  protected submitCall(): void {
    if (this.callForm.invalid || this.savingCall()) {
      this.callForm.markAllAsTouched();
      return;
    }

    const examId = this.callForm.controls.examId.value;
    if (!examId) {
      return;
    }

    const payload = this.toCreateExamCallRequest();
    this.savingCall.set(true);
    this.createCallError.set(null);

    this.professorExamsApi
      .createCall(examId, payload)
      .pipe(
        take(1),
        finalize(() => this.savingCall.set(false))
      )
      .subscribe({
        next: () => {
          this.snackBar.open('Llamado creado correctamente.', 'Cerrar', { duration: AUTH_UI.snackbarDurationMs });
          this.callForm.patchValue({
            startDate: '',
            endDate: '',
            totalCapacity: null,
          });
          this.loadCallsForExam(examId);
          this.callForm.markAsPristine();
          this.callForm.markAsUntouched();
        },
        error: (error: unknown) => {
          this.createCallError.set(this.errorMessage(error, 'No se pudo crear el llamado.'));
        },
      });
  }

  protected hasExamQuestionsMinError(): boolean {
    return this.questions.hasError('minlength') && this.questions.touched;
  }

  protected hasCallDateRangeError(): boolean {
    return !!this.callForm.errors?.['dateRange'] && this.callForm.touched;
  }

  protected logout(): void {
    this.authService
      .logout()
      .pipe(take(1))
      .subscribe(() => {
        void this.router.navigateByUrl(APP_ROUTES.login);
      });
  }

  private get questions(): FormArray<QuestionFormGroup> {
    return this.examForm.controls.questions;
  }

  private buildQuestionGroup(): QuestionFormGroup {
    return this.formBuilder.nonNullable.group({
      statement: ['', [Validators.required]],
      type: ['OPEN' as QuestionType, [Validators.required]],
      points: [0, [Validators.required, Validators.min(0)]],
    });
  }

  private toCreateExamRequest(): CreateExamRequest {
    const raw = this.examForm.getRawValue();
    const description = raw.description?.trim() ?? '';

    return {
      title: raw.title?.trim() ?? '',
      description: description.length > 0 ? description : null,
      durationMinutes: raw.durationMinutes ?? 0,
      questions: raw.questions.map((question) => ({
        statement: question.statement?.trim() ?? '',
        type: (question.type ?? 'OPEN') as QuestionType,
        points: question.points ?? 0,
      })),
    };
  }

  private toCreateExamCallRequest(): CreateExamCallRequest {
    const raw = this.callForm.getRawValue();
    return {
      startDate: this.toLocalDateTimeWithSeconds(raw.startDate ?? ''),
      endDate: this.toLocalDateTimeWithSeconds(raw.endDate ?? ''),
      totalCapacity: raw.totalCapacity ?? null,
    };
  }

  private resetExamForm(): void {
    this.examForm.controls.title.reset('');
    this.examForm.controls.description.reset('');
    this.examForm.controls.durationMinutes.reset(null);

    while (this.questions.length > 0) {
      this.questions.removeAt(0);
    }
    this.questions.push(this.buildQuestionGroup());

    this.examForm.markAsPristine();
    this.examForm.markAsUntouched();
  }

  private toLocalDateTimeWithSeconds(value: string): string {
    if (value.length === 16) {
      return `${value}:00`;
    }
    return value;
  }

  private loadCallsForExam(examId: number): void {
    this.loadingCalls.set(true);
    this.callsError.set(null);

    this.professorExamsApi
      .findCalls(examId)
      .pipe(
        take(1),
        finalize(() => this.loadingCalls.set(false))
      )
      .subscribe({
        next: (calls) => this.examCalls.set(calls),
        error: (error: unknown) => {
          this.examCalls.set([]);
          this.callsError.set(this.errorMessage(error, 'No se pudieron cargar los llamados de este examen.'));
        },
      });
  }

  private errorMessage(error: unknown, fallback: string): string {
    if (error instanceof HttpErrorResponse && (error.status === 401 || error.status === 403)) {
      return 'Tu sesion no tiene permisos para gestionar examenes.';
    }
    return this.authService.errorMessage(error, fallback);
  }
}
