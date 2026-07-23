import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

export type AttemptStatus = 'IN_PROGRESS' | 'SUBMITTED' | 'GRADED';

export interface GradingAttemptSummary {
  attemptId: number;
  examCallId: number;
  studentId: number;
  studentName: string;
  status: AttemptStatus;
  finalScore: number | null;
  startedAt: string;
  submittedAt: string | null;
}

export interface GradingAttemptQuestion {
  attemptQuestionId: number;
  questionId: number;
  statement: string;
  questionOrder: number;
  answerText: string | null;
  awardedScore: number | null;
  reviewComment: string | null;
}

export interface GradingAttemptDetail {
  attemptId: number;
  examCallId: number;
  studentId: number;
  studentName: string;
  status: AttemptStatus;
  finalScore: number | null;
  startedAt: string;
  submittedAt: string | null;
  questions: GradingAttemptQuestion[];
}

interface GradeQuestionRequest {
  awardedScore: number;
  reviewComment: string | null;
}

@Injectable({ providedIn: 'root' })
export class GradingService {
  private readonly baseUrl = '/api/grading';

  constructor(private readonly http: HttpClient) {}

  getAttempts(): Observable<GradingAttemptSummary[]> {
    return this.http.get<GradingAttemptSummary[]>(`${this.baseUrl}/attempts`);
  }

  getAttempt(attemptId: number): Observable<GradingAttemptDetail> {
    return this.http.get<GradingAttemptDetail>(`${this.baseUrl}/attempts/${attemptId}`);
  }

  gradeQuestion(
    attemptId: number,
    questionId: number,
    awardedScore: number,
    reviewComment: string | null
  ): Observable<unknown> {
    const body: GradeQuestionRequest = { awardedScore, reviewComment };
    return this.http.patch(`${this.baseUrl}/attempts/${attemptId}/questions/${questionId}`, body);
  }

  closeGrading(attemptId: number): Observable<GradingAttemptSummary> {
    return this.http.post<GradingAttemptSummary>(`${this.baseUrl}/attempts/${attemptId}/close`, {});
  }
}
