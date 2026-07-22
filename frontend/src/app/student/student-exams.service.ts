import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

export type AttemptStatus = 'IN_PROGRESS' | 'SUBMITTED' | 'GRADED';

export interface AvailableExam {
  examCallId: number;
  examId: number;
  title: string;
  description: string | null;
  durationMinutes: number;
  startsAt: string;
  endsAt: string;
  professorName: string | null;
  remainingCapacity: number | null;
  attemptId: number | null;
  attemptStatus: AttemptStatus | null;
}

export interface AttemptSummary {
  attemptId: number;
  examCallId: number;
  examId: number;
  examTitle: string;
  status: AttemptStatus;
  startedAt: string;
  submittedAt: string | null;
  finalScore: number | null;
}

export interface AttemptQuestion {
  attemptQuestionId: number;
  questionId: number;
  order: number;
  statement: string;
  type: string;
  points: number;
  answerText: string | null;
  awardedScore: number | null;
  reviewComment: string | null;
}

export interface AttemptDetail {
  attemptId: number;
  examCallId: number;
  examId: number;
  examTitle: string;
  examDescription: string | null;
  status: AttemptStatus;
  startedAt: string;
  deadline: string;
  submittedAt: string | null;
  finalScore: number | null;
  questions: AttemptQuestion[];
}

interface AnswersRequest {
  answers: Array<{
    attemptQuestionId: number;
    answerText: string;
  }>;
}

@Injectable({ providedIn: 'root' })
export class StudentExamsService {
  private readonly baseUrl = '/api/student';

  constructor(private readonly http: HttpClient) {}

  getAvailableExams(): Observable<AvailableExam[]> {
    return this.http.get<AvailableExam[]>(`${this.baseUrl}/exams/available`, {
      headers: this.authHeaders()
    });
  }

  getMyAttempts(): Observable<AttemptSummary[]> {
    return this.http.get<AttemptSummary[]>(`${this.baseUrl}/attempts`, {
      headers: this.authHeaders()
    });
  }

  getAttempt(attemptId: number): Observable<AttemptDetail> {
    return this.http.get<AttemptDetail>(`${this.baseUrl}/attempts/${attemptId}`, {
      headers: this.authHeaders()
    });
  }

  startAttempt(examCallId: number): Observable<AttemptDetail> {
    return this.http.post<AttemptDetail>(
      `${this.baseUrl}/exams/${examCallId}/attempts`,
      {},
      { headers: this.authHeaders() }
    );
  }

  saveAnswers(attempt: AttemptDetail): Observable<AttemptDetail> {
    return this.http.put<AttemptDetail>(
      `${this.baseUrl}/attempts/${attempt.attemptId}/answers`,
      this.toAnswersRequest(attempt),
      { headers: this.authHeaders() }
    );
  }

  submitAttempt(attempt: AttemptDetail): Observable<AttemptDetail> {
    return this.http.post<AttemptDetail>(
      `${this.baseUrl}/attempts/${attempt.attemptId}/submit`,
      this.toAnswersRequest(attempt),
      { headers: this.authHeaders() }
    );
  }

  private toAnswersRequest(attempt: AttemptDetail): AnswersRequest {
    return {
      answers: attempt.questions.map((question) => ({
        attemptQuestionId: question.attemptQuestionId,
        answerText: question.answerText?.trim() ?? ''
      }))
    };
  }

  private authHeaders(): HttpHeaders {
    // Se usa la misma clave definida por el módulo de autenticación del proyecto.
    const rawSession = localStorage.getItem('exam.auth.session');
    if (!rawSession) {
      return new HttpHeaders();
    }

    try {
      const session = JSON.parse(rawSession) as { accessToken?: string };
      return session.accessToken
        ? new HttpHeaders({ Authorization: `Bearer ${session.accessToken}` })
        : new HttpHeaders();
    } catch {
      return new HttpHeaders();
    }
  }
}
