import { TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { provideRouter, Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { MatSnackBar } from '@angular/material/snack-bar';

import { APP_ROUTES } from '@/app/auth/contracts/auth.contracts';
import { AuthService } from '@/app/auth/services/auth.service';
import { ProfessorExamsComponent } from '@/app/professor/professor-exams.component';
import {
  ProfessorExamsApiService,
  type ExamCallResponse,
  type ExamResponse,
} from '@/app/professor/professor-exams-api.service';

describe('ProfessorExamsComponent', () => {
  const examList: ExamResponse[] = [
    {
      id: 1,
      title: 'Algebra 1',
      description: 'Parcial',
      durationMinutes: 90,
      professorId: 77,
    },
  ];

  const apiMock = {
    findMine: vi.fn(() => of(examList)),
    findCalls: vi.fn(() => of([] as ExamCallResponse[])),
    createExam: vi.fn(),
    createCall: vi.fn(() => of({ id: 2, examId: 1 })),
  };

  const authMock = {
    logout: vi.fn(() => of(void 0)),
    errorMessage: vi.fn((error: unknown, fallback: string) => {
      const message = (error as { error?: { message?: string } })?.error?.message;
      return message ?? fallback;
    }),
  };

  const snackBarMock = {
    open: vi.fn(),
  };

  beforeEach(async () => {
    apiMock.findMine.mockClear();
    apiMock.findCalls.mockClear();
    apiMock.createExam.mockReset();
    apiMock.createCall.mockClear();
    authMock.logout.mockClear();
    authMock.errorMessage.mockClear();
    snackBarMock.open.mockClear();

    await TestBed.configureTestingModule({
      imports: [ProfessorExamsComponent],
      providers: [
        provideRouter([]),
        provideNoopAnimations(),
        { provide: ProfessorExamsApiService, useValue: apiMock },
        { provide: AuthService, useValue: authMock },
        { provide: MatSnackBar, useValue: snackBarMock },
      ],
    }).compileComponents();
  });

  it('blocks exam submit when questions array is empty', () => {
    const fixture = TestBed.createComponent(ProfessorExamsComponent);
    fixture.detectChanges();
    const component = fixture.componentInstance as unknown as {
      examForm: any;
      submitExam: () => void;
    };

    component.examForm.controls.questions.clear();
    component.examForm.controls.questions.updateValueAndValidity();
    component.examForm.controls.title.setValue('Parcial Algebra');
    component.examForm.controls.durationMinutes.setValue(90);

    component.submitExam();

    expect(apiMock.createExam).not.toHaveBeenCalled();
  });

  it('marks date range invalid when end is before start', () => {
    const fixture = TestBed.createComponent(ProfessorExamsComponent);
    fixture.detectChanges();
    const component = fixture.componentInstance as unknown as {
      callForm: any;
      hasCallDateRangeError: () => boolean;
    };

    component.callForm.patchValue({
      examId: 1,
      startDate: '2026-08-01T11:00',
      endDate: '2026-08-01T09:00',
    });
    component.callForm.markAllAsTouched();

    expect(component.hasCallDateRangeError()).toBe(true);
  });

  it('jumps to call tab and preselects exam', () => {
    const fixture = TestBed.createComponent(ProfessorExamsComponent);
    fixture.detectChanges();
    const component = fixture.componentInstance as unknown as {
      jumpToCall: (exam: ExamResponse) => void;
      selectedTabIndex: () => number;
      callForm: any;
    };

    component.jumpToCall(examList[0] as ExamResponse);

    expect(component.selectedTabIndex()).toBe(2);
    expect(component.callForm.controls.examId.value).toBe(1);
    expect(apiMock.findCalls).toHaveBeenCalledWith(1);
  });

  it('loads calls when exam selection changes', () => {
    apiMock.findCalls.mockReturnValue(
      of([
        {
          id: 15,
          examId: 1,
          startDate: '2026-08-01T09:00:00',
          endDate: '2026-08-01T11:00:00',
          totalCapacity: 30,
          currentEnrollment: 5,
        },
      ])
    );

    const fixture = TestBed.createComponent(ProfessorExamsComponent);
    fixture.detectChanges();
    const component = fixture.componentInstance as unknown as {
      onCallExamChanged: (id: number | null) => void;
      examCalls: () => Array<{ id: number }>;
    };

    component.onCallExamChanged(1);

    expect(apiMock.findCalls).toHaveBeenCalledWith(1);
    expect(component.examCalls().length).toBe(1);
    expect(component.examCalls()[0]?.id).toBe(15);
  });

  it('creates exam, refreshes list, and returns to exams tab', () => {
    apiMock.createExam.mockReturnValue(
      of({
        id: 10,
        title: 'Algebra 2',
        description: null,
        durationMinutes: 100,
        professorId: 77,
        questions: [{ id: 33, statement: 'x?', type: 'OPEN', points: 5 }],
      })
    );

    const fixture = TestBed.createComponent(ProfessorExamsComponent);
    fixture.detectChanges();
    const component = fixture.componentInstance as unknown as {
      examForm: any;
      addQuestion: () => void;
      selectTab: (index: number) => void;
      selectedTabIndex: () => number;
      submitExam: () => void;
    };

    component.selectTab(1);
    component.examForm.controls.title.setValue('Nuevo Parcial');
    component.examForm.controls.durationMinutes.setValue(80);
    component.examForm.controls.questions.controls[0].controls.statement.setValue('Pregunta 1');
    component.examForm.controls.questions.controls[0].controls.type.setValue('OPEN');
    component.examForm.controls.questions.controls[0].controls.points.setValue(10);

    component.submitExam();

    expect(apiMock.createExam).toHaveBeenCalledTimes(1);
    expect(apiMock.findMine).toHaveBeenCalledTimes(2);
    expect(component.selectedTabIndex()).toBe(0);
  });

  it('shows backend message on create exam error', () => {
    apiMock.createExam.mockReturnValue(throwError(() => ({ error: { message: 'Error backend' } })));

    const fixture = TestBed.createComponent(ProfessorExamsComponent);
    fixture.detectChanges();
    const component = fixture.componentInstance as unknown as {
      examForm: any;
      submitExam: () => void;
      createExamError: () => string | null;
    };

    component.examForm.controls.title.setValue('Nuevo Parcial');
    component.examForm.controls.durationMinutes.setValue(80);
    component.examForm.controls.questions.controls[0].controls.statement.setValue('Pregunta 1');
    component.examForm.controls.questions.controls[0].controls.type.setValue('OPEN');
    component.examForm.controls.questions.controls[0].controls.points.setValue(10);

    component.submitExam();

    expect(component.createExamError()).toBe('Error backend');
  });

  it('logs out and redirects to login route', () => {
    const fixture = TestBed.createComponent(ProfessorExamsComponent);
    fixture.detectChanges();
    const component = fixture.componentInstance as unknown as { logout: () => void };
    const router = TestBed.inject(Router);
    const navigateSpy = vi.spyOn(router, 'navigateByUrl').mockResolvedValue(true);

    component.logout();

    expect(authMock.logout).toHaveBeenCalledTimes(1);
    expect(navigateSpy).toHaveBeenCalledWith(APP_ROUTES.login);
  });
});
