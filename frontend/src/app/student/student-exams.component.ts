import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, TemplateRef, ViewChild, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatDialog, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatTabsModule } from '@angular/material/tabs';
import { Router, RouterLink } from '@angular/router';
import { finalize, forkJoin, take } from 'rxjs';
import { APP_ROUTES } from '../auth/contracts/auth.contracts';
import { AuthService } from '../auth/services/auth.service';
import {
  AttemptDetail,
  AttemptStatus,
  AttemptSummary,
  AvailableExam,
  StudentExamsService
} from './student-exams.service';

type Section = 'available' | 'history';

@Component({
  selector: 'app-student-exams',
  imports: [
    CommonModule,
    FormsModule,
    MatCardModule,
    MatButtonModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatTabsModule,
    RouterLink
  ],
  templateUrl: './student-exams.component.html',
  styleUrl: './student-exams.component.scss'
})
export class StudentExamsComponent implements OnInit {
  readonly availableExams = signal<AvailableExam[]>([]);
  readonly attempts = signal<AttemptSummary[]>([]);
  readonly activeAttempt = signal<AttemptDetail | null>(null);
  readonly section = signal<Section>('available');
  readonly loading = signal(false);
  readonly saving = signal(false);
  readonly message = signal('');
  readonly error = signal('');
  dialogRef: MatDialogRef<unknown> | null = null;
  readonly routes = APP_ROUTES;

  @ViewChild('attemptDialog') private attemptDialog?: TemplateRef<unknown>;

  constructor(
    private readonly studentExamsService: StudentExamsService,
    private readonly dialog: MatDialog,
    private readonly authService: AuthService,
    private readonly router: Router
  ) {}

  ngOnInit(): void {
    this.loadData();
  }

  get selectedTabIndex(): number {
    return this.section() === 'available' ? 0 : 1;
  }

  loadData(): void {
    this.loading.set(true);
    this.error.set('');

    forkJoin({
      exams: this.studentExamsService.getAvailableExams(),
      attempts: this.studentExamsService.getMyAttempts()
    })
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: ({ exams, attempts }) => {
          this.availableExams.set(exams);
          this.attempts.set(attempts);
        },
        error: (error) => {
          this.error.set(this.getErrorMessage(error));
        }
      });
  }

  changeSection(section: Section): void {
    this.section.set(section);
    this.message.set('');
    this.error.set('');
  }

  changeTab(index: number): void {
    this.changeSection(index === 0 ? 'available' : 'history');
  }

  startOrContinue(exam: AvailableExam): void {
    if (exam.attemptId) {
      this.openAttempt(exam.attemptId);
      return;
    }

    this.loading.set(true);
    this.error.set('');
    this.studentExamsService.startAttempt(exam.examCallId).subscribe({
      next: (attempt) => {
        this.activeAttempt.set(attempt);
        this.loading.set(false);
        this.openAttemptDialog();
      },
      error: (error) => this.finishWithError(error)
    });
  }

  openAttempt(attemptId: number): void {
    this.loading.set(true);
    this.error.set('');
    this.studentExamsService.getAttempt(attemptId).subscribe({
      next: (attempt) => {
        this.activeAttempt.set(attempt);
        this.loading.set(false);
        this.openAttemptDialog();
      },
      error: (error) => this.finishWithError(error)
    });
  }

  closeAttempt(): void {
    if (this.dialogRef) {
      this.dialogRef.close();
      return;
    }
    this.resetAttemptView();
  }

  logout(): void {
    this.authService
      .logout()
      .pipe(take(1))
      .subscribe(() => {
        void this.router.navigateByUrl(APP_ROUTES.login);
      });
  }

  saveProgress(): void {
    const activeAttempt = this.activeAttempt();
    if (!activeAttempt || !this.isEditable(activeAttempt.status)) {
      return;
    }

    this.saving.set(true);
    this.message.set('');
    this.error.set('');
    this.studentExamsService.saveAnswers(activeAttempt).subscribe({
      next: (attempt) => {
        this.activeAttempt.set(attempt);
        this.saving.set(false);
        this.message.set('El progreso se guardó correctamente.');
      },
      error: (error) => this.finishSavingWithError(error)
    });
  }

  submitAttempt(): void {
    const activeAttempt = this.activeAttempt();
    if (!activeAttempt || !this.isEditable(activeAttempt.status)) {
      return;
    }

    const hasMissingAnswers = activeAttempt.questions.some(
      (question) => !question.answerText?.trim()
    );
    if (hasMissingAnswers) {
      this.error.set('Tenés que responder todas las preguntas antes de enviar el examen.');
      return;
    }

    const confirmed = window.confirm(
      '¿Querés enviar el examen? Después de enviarlo no vas a poder modificarlo.'
    );
    if (!confirmed) {
      return;
    }

    this.saving.set(true);
    this.message.set('');
    this.error.set('');
    this.studentExamsService.submitAttempt(activeAttempt).subscribe({
      next: (attempt) => {
        this.activeAttempt.set(attempt);
        this.saving.set(false);
        this.message.set('El examen fue enviado y quedó pendiente de corrección.');
        this.refreshLists();
      },
      error: (error) => this.finishSavingWithError(error)
    });
  }

  isEditable(status: AttemptStatus): boolean {
    return status === 'IN_PROGRESS';
  }

  statusLabel(status: AttemptStatus): string {
    const labels: Record<AttemptStatus, string> = {
      IN_PROGRESS: 'En curso',
      SUBMITTED: 'Pendiente de corrección',
      GRADED: 'Corregido'
    };
    return labels[status];
  }

  questionTypeLabel(type: string): string {
    const labels: Record<string, string> = {
      OPEN: 'Respuesta abierta',
      TRUE_FALSE: 'Verdadero o falso',
      MULTIPLE_CHOICE: 'Opción múltiple'
    };
    return labels[type] ?? type;
  }

  private refreshLists(): void {
    this.studentExamsService.getAvailableExams().subscribe({
      next: (exams) => this.availableExams.set(exams)
    });
    this.studentExamsService.getMyAttempts().subscribe({
      next: (attempts) => this.attempts.set(attempts)
    });
  }

  private finishWithError(error: unknown): void {
    this.loading.set(false);
    this.error.set(this.getErrorMessage(error));
  }

  private finishSavingWithError(error: unknown): void {
    this.saving.set(false);
    this.error.set(this.getErrorMessage(error));
  }

  private getErrorMessage(error: unknown): string {
    if (error instanceof HttpErrorResponse) {
      if (error.status === 401 || error.status === 403) {
        return 'Tu sesión no permite acceder a los exámenes del estudiante.';
      }
      if (typeof error.error?.message === 'string') {
        return error.error.message;
      }
    }
    return 'No se pudo completar la operación. Intentá nuevamente.';
  }

  private openAttemptDialog(): void {
    if (!this.activeAttempt() || !this.attemptDialog || this.dialogRef) {
      return;
    }

    this.dialogRef = this.dialog.open(this.attemptDialog, {
      width: 'min(900px, calc(100vw - 32px))',
      maxWidth: '900px'
    });

    this.dialogRef
      .afterClosed()
      .pipe(take(1))
      .subscribe(() => {
        this.dialogRef = null;
        this.resetAttemptView();
      });
  }

  private resetAttemptView(): void {
    this.activeAttempt.set(null);
    this.message.set('');
    this.error.set('');
  }
}
