import { CommonModule } from '@angular/common';
import { Component, OnChanges, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
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
  imports: [CommonModule, FormsModule],
  templateUrl: './student-exams.component.html',
  styleUrl: './student-exams.component.scss'
})
export class StudentExamsComponent implements OnInit {
  availableExams: AvailableExam[] = [];
  attempts: AttemptSummary[] = [];
  activeAttempt: AttemptDetail | null = null;
  section: Section = 'available';
  loading = false;
  saving = false;
  message = '';
  error = '';

  constructor(private readonly studentExamsService: StudentExamsService) {}

  ngOnInit(): void {
    this.loadData();
  }

  loadData(): void {
    this.loading = true;
    this.error = '';

    this.studentExamsService.getAvailableExams().subscribe({
      next: (exams) => {
        this.availableExams = exams;
        this.loadAttempts();
      },
      error: (error) => this.finishWithError(error)
    });
  }

  changeSection(section: Section): void {
    this.section = section;
    this.message = '';
    this.error = '';
  }

  startOrContinue(exam: AvailableExam): void {
    if (exam.attemptId) {
      this.openAttempt(exam.attemptId);
      return;
    }

    this.loading = true;
    this.error = '';
    this.studentExamsService.startAttempt(exam.examCallId).subscribe({
      next: (attempt) => {
        this.activeAttempt = attempt;
        this.loading = false;
      },
      error: (error) => this.finishWithError(error)
    });
  }

  openAttempt(attemptId: number): void {
    this.loading = true;
    this.error = '';
    this.studentExamsService.getAttempt(attemptId).subscribe({
      next: (attempt) => {
        this.activeAttempt = attempt;
        this.loading = false;
      },
      error: (error) => this.finishWithError(error)
    });
  }

  closeAttempt(): void {
    this.activeAttempt = null;
    this.message = '';
    this.error = '';
  }

  saveProgress(): void {
    if (!this.activeAttempt || !this.isEditable(this.activeAttempt.status)) {
      return;
    }

    this.saving = true;
    this.message = '';
    this.error = '';
    this.studentExamsService.saveAnswers(this.activeAttempt).subscribe({
      next: (attempt) => {
        this.activeAttempt = attempt;
        this.saving = false;
        this.message = 'El progreso se guardó correctamente.';
      },
      error: (error) => this.finishSavingWithError(error)
    });
  }

  submitAttempt(): void {
    if (!this.activeAttempt || !this.isEditable(this.activeAttempt.status)) {
      return;
    }

    const hasMissingAnswers = this.activeAttempt.questions.some(
      (question) => !question.answerText?.trim()
    );
    if (hasMissingAnswers) {
      this.error = 'Tenés que responder todas las preguntas antes de enviar el examen.';
      return;
    }

    const confirmed = window.confirm(
      '¿Querés enviar el examen? Después de enviarlo no vas a poder modificarlo.'
    );
    if (!confirmed) {
      return;
    }

    this.saving = true;
    this.message = '';
    this.error = '';
    this.studentExamsService.submitAttempt(this.activeAttempt).subscribe({
      next: (attempt) => {
        this.activeAttempt = attempt;
        this.saving = false;
        this.message = 'El examen fue enviado y quedó pendiente de corrección.';
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

  private loadAttempts(): void {
    this.studentExamsService.getMyAttempts().subscribe({
      next: (attempts) => {
        this.attempts = attempts;
        this.loading = false;
      },
      error: (error) => this.finishWithError(error)
    });
  }

  private refreshLists(): void {
    this.studentExamsService.getAvailableExams().subscribe({
      next: (exams) => (this.availableExams = exams)
    });
    this.studentExamsService.getMyAttempts().subscribe({
      next: (attempts) => (this.attempts = attempts)
    });
  }

  private finishWithError(error: unknown): void {
    this.loading = false;
    this.error = this.getErrorMessage(error);
  }

  private finishSavingWithError(error: unknown): void {
    this.saving = false;
    this.error = this.getErrorMessage(error);
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
}
