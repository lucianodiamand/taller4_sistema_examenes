import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, OnInit, TemplateRef, ViewChild, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatDialog, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { finalize, take } from 'rxjs';

import { AUTH_UI } from '../auth/contracts/auth.contracts';
import { AuthService } from '../auth/services/auth.service';
import { GradingAttemptDetail, GradingAttemptSummary, GradingService } from './grading.service';

interface QuestionDraft {
  awardedScore: number | null;
  reviewComment: string;
}

@Component({
  selector: 'app-grading',
  imports: [
    CommonModule,
    FormsModule,
    MatCardModule,
    MatButtonModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatSnackBarModule,
  ],
  templateUrl: './grading.component.html',
  styleUrl: './grading.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class GradingComponent implements OnInit {
  private readonly gradingService = inject(GradingService);
  private readonly authService = inject(AuthService);
  private readonly dialog = inject(MatDialog);
  private readonly snackBar = inject(MatSnackBar);

  protected readonly attempts = signal<GradingAttemptSummary[]>([]);
  protected readonly activeAttempt = signal<GradingAttemptDetail | null>(null);
  protected readonly drafts = signal<Record<number, QuestionDraft>>({});
  protected readonly loading = signal(false);
  protected readonly saving = signal(false);
  protected readonly closing = signal(false);
  protected readonly error = signal('');

  private dialogRef: MatDialogRef<unknown> | null = null;

  @ViewChild('correctionDialog') private correctionDialog?: TemplateRef<unknown>;

  ngOnInit(): void {
    this.loadAttempts();
  }

  protected loadAttempts(): void {
    this.loading.set(true);
    this.error.set('');
    this.gradingService
      .getAttempts()
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (attempts) => this.attempts.set(attempts),
        error: (error) =>
          this.error.set(this.authService.errorMessage(error, 'No se pudo cargar las resoluciones')),
      });
  }

  protected openAttempt(attemptId: number): void {
    this.gradingService.getAttempt(attemptId).subscribe({
      next: (attempt) => {
        this.activeAttempt.set(attempt);
        this.drafts.set(this.buildDrafts(attempt));
        this.openDialog();
      },
      error: (error) => this.notify(this.authService.errorMessage(error, 'No se pudo abrir la resolución')),
    });
  }

  protected draftFor(attemptQuestionId: number): QuestionDraft {
    return this.drafts()[attemptQuestionId] ?? { awardedScore: null, reviewComment: '' };
  }

  protected updateScore(attemptQuestionId: number, value: number | string): void {
    const parsed = value === '' || value === null ? null : Number(value);
    this.drafts.update((current) => ({
      ...current,
      [attemptQuestionId]: { ...this.draftFor(attemptQuestionId), awardedScore: parsed },
    }));
  }

  protected updateComment(attemptQuestionId: number, value: string): void {
    this.drafts.update((current) => ({
      ...current,
      [attemptQuestionId]: { ...this.draftFor(attemptQuestionId), reviewComment: value },
    }));
  }

  protected saveScore(attemptId: number, attemptQuestionId: number, questionId: number): void {
    const draft = this.draftFor(attemptQuestionId);
    if (draft.awardedScore === null || draft.awardedScore < 0) {
      this.notify('Ingresá un puntaje válido antes de guardar.');
      return;
    }

    this.saving.set(true);
    this.gradingService
      .gradeQuestion(attemptId, questionId, draft.awardedScore, draft.reviewComment || null)
      .pipe(finalize(() => this.saving.set(false)))
      .subscribe({
        next: () => this.notify('Nota guardada.'),
        error: (error) => this.notify(this.authService.errorMessage(error, 'No se pudo guardar la nota')),
      });
  }

  protected allQuestionsGraded(): boolean {
    const attempt = this.activeAttempt();
    if (!attempt) {
      return false;
    }
    return attempt.questions.every((question) => this.draftFor(question.attemptQuestionId).awardedScore !== null);
  }

  protected closeGrading(): void {
    const attempt = this.activeAttempt();
    if (!attempt || !this.allQuestionsGraded()) {
      return;
    }

    const confirmed = window.confirm('¿Cerrar la corrección? Después no vas a poder modificar las notas.');
    if (!confirmed) {
      return;
    }

    this.closing.set(true);
    this.gradingService
      .closeGrading(attempt.attemptId)
      .pipe(finalize(() => this.closing.set(false)))
      .subscribe({
        next: () => {
          this.notify('Corrección cerrada, nota final calculada.');
          this.dialogRef?.close();
          this.loadAttempts();
        },
        error: (error) => this.notify(this.authService.errorMessage(error, 'No se pudo cerrar la corrección')),
      });
  }

  protected canGrade(attempt: GradingAttemptSummary): boolean {
    return attempt.status === 'SUBMITTED';
  }

  protected statusLabel(status: string): string {
    const labels: Record<string, string> = {
      IN_PROGRESS: 'En curso',
      SUBMITTED: 'Pendiente de corrección',
      GRADED: 'Corregido',
    };
    return labels[status] ?? status;
  }

  private buildDrafts(attempt: GradingAttemptDetail): Record<number, QuestionDraft> {
    const drafts: Record<number, QuestionDraft> = {};
    for (const question of attempt.questions) {
      drafts[question.attemptQuestionId] = {
        awardedScore: question.awardedScore,
        reviewComment: question.reviewComment ?? '',
      };
    }
    return drafts;
  }

  private openDialog(): void {
    if (!this.correctionDialog || this.dialogRef) {
      return;
    }
    this.dialogRef = this.dialog.open(this.correctionDialog, {
      width: 'min(900px, calc(100vw - 32px))',
      maxWidth: '900px',
    });
    this.dialogRef
      .afterClosed()
      .pipe(take(1))
      .subscribe(() => {
        this.dialogRef = null;
        this.activeAttempt.set(null);
        this.drafts.set({});
      });
  }

  private notify(message: string): void {
    this.snackBar.open(message, 'Cerrar', { duration: AUTH_UI.snackbarDurationMs });
  }
}
