import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

export type QuestionType = 'OPEN' | 'TRUE_FALSE' | 'MULTIPLE_CHOICE';

export interface ExamResponse {
  id: number;
  title: string;
  description: string | null;
  durationMinutes: number;
  professorId: number | null;
}

export interface QuestionResponse {
  id: number;
  statement: string;
  type: QuestionType;
  points: number;
}

export interface ExamCreatedResponse {
  id: number;
  title: string;
  description: string | null;
  durationMinutes: number;
  professorId: number;
  questions: QuestionResponse[];
}

export interface ExamCallResponse {
  id: number;
  examId: number;
  startDate: string;
  endDate: string;
  totalCapacity: number | null;
  currentEnrollment: number;
}

export interface CreateExamRequest {
  title: string;
  description: string | null;
  durationMinutes: number;
  questions: Array<{
    statement: string;
    type: QuestionType;
    points: number;
  }>;
}

export interface CreateExamCallRequest {
  startDate: string;
  endDate: string;
  totalCapacity: number | null;
}

const EXAMS_ENDPOINT = '/api/exams';

@Injectable({ providedIn: 'root' })
export class ProfessorExamsApiService {
  private readonly http = inject(HttpClient);

  findMine(): Observable<ExamResponse[]> {
    return this.http.get<ExamResponse[]>(EXAMS_ENDPOINT);
  }

  createExam(payload: CreateExamRequest): Observable<ExamCreatedResponse> {
    return this.http.post<ExamCreatedResponse>(EXAMS_ENDPOINT, payload);
  }

  findCalls(examId: number): Observable<ExamCallResponse[]> {
    return this.http.get<ExamCallResponse[]>(`${EXAMS_ENDPOINT}/${examId}/calls`);
  }

  createCall(examId: number, payload: CreateExamCallRequest): Observable<ExamCallResponse> {
    return this.http.post<ExamCallResponse>(`${EXAMS_ENDPOINT}/${examId}/calls`, payload);
  }
}
