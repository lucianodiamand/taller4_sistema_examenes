import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';

import {
  ProfessorExamsApiService,
  type CreateExamCallRequest,
  type CreateExamRequest,
} from '@/app/professor/professor-exams-api.service';

describe('ProfessorExamsApiService', () => {
  let service: ProfessorExamsApiService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), ProfessorExamsApiService],
    });

    service = TestBed.inject(ProfessorExamsApiService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('gets professor exams', () => {
    service.findMine().subscribe((exams) => {
      expect(exams.length).toBe(1);
      expect(exams[0]?.title).toBe('Parcial Algebra');
    });

    const req = httpMock.expectOne('/api/exams');
    expect(req.request.method).toBe('GET');
    req.flush([
      {
        id: 1,
        title: 'Parcial Algebra',
        description: 'Parcial 1',
        durationMinutes: 90,
        professorId: 77,
      },
    ]);
  });

  it('creates exam with expected payload', () => {
    const payload: CreateExamRequest = {
      title: 'Parcial Algebra',
      description: 'Parcial 1',
      durationMinutes: 90,
      questions: [{ statement: '2 + 2?', type: 'OPEN', points: 10 }],
    };

    service.createExam(payload).subscribe((response) => {
      expect(response.id).toBe(9);
      expect(response.questions.length).toBe(1);
    });

    const req = httpMock.expectOne('/api/exams');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(payload);
    req.flush({
      id: 9,
      title: 'Parcial Algebra',
      description: 'Parcial 1',
      durationMinutes: 90,
      professorId: 77,
      questions: [{ id: 10, statement: '2 + 2?', type: 'OPEN', points: 10 }],
    });
  });

  it('creates call and allows null totalCapacity', () => {
    const payload: CreateExamCallRequest = {
      startDate: '2026-08-01T09:00:00',
      endDate: '2026-08-01T11:00:00',
      totalCapacity: null,
    };

    service.createCall(1, payload).subscribe((response) => {
      expect(response.examId).toBe(1);
      expect(response.totalCapacity).toBeNull();
    });

    const req = httpMock.expectOne('/api/exams/1/calls');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(payload);
    req.flush({
      id: 11,
      examId: 1,
      startDate: '2026-08-01T09:00:00',
      endDate: '2026-08-01T11:00:00',
      totalCapacity: null,
      currentEnrollment: 0,
    });
  });

  it('gets calls for selected exam', () => {
    service.findCalls(1).subscribe((calls) => {
      expect(calls.length).toBe(1);
      expect(calls[0]?.id).toBe(15);
    });

    const req = httpMock.expectOne('/api/exams/1/calls');
    expect(req.request.method).toBe('GET');
    req.flush([
      {
        id: 15,
        examId: 1,
        startDate: '2026-08-01T09:00:00',
        endDate: '2026-08-01T11:00:00',
        totalCapacity: 30,
        currentEnrollment: 12,
      },
    ]);
  });
});
