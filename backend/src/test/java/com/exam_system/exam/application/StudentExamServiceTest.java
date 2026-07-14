package com.exam_system.exam.application;

import com.exam_system.auth.security.CurrentUser;
import com.exam_system.exam.domain.AttemptQuestion;
import com.exam_system.exam.domain.AttemptStatus;
import com.exam_system.exam.domain.Exam;
import com.exam_system.exam.domain.ExamAttempt;
import com.exam_system.exam.domain.ExamCall;
import com.exam_system.exam.domain.Question;
import com.exam_system.exam.domain.QuestionType;
import com.exam_system.exam.repository.ExamAttemptRepository;
import com.exam_system.exam.repository.ExamCallRepository;
import com.exam_system.exam.repository.QuestionRepository;
import com.exam_system.user.domain.User;
import com.exam_system.user.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Cubre las reglas principales del módulo del estudiante sin levantar toda la aplicación.
 */
@ExtendWith(MockitoExtension.class)
class StudentExamServiceTest {

    @Mock
    private ExamCallRepository examCallRepository;

    @Mock
    private ExamAttemptRepository examAttemptRepository;

    @Mock
    private QuestionRepository questionRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CurrentUser currentUser;

    private StudentExamService studentExamService;

    @BeforeEach
    void setUp() {
        studentExamService = new StudentExamService(
                examCallRepository,
                examAttemptRepository,
                questionRepository,
                userRepository,
                currentUser
        );
        when(currentUser.id()).thenReturn(15L);
    }

    @Test
    void startAttemptCreatesQuestionCopiesAndUpdatesEnrollment() {
        Exam exam = exam(7L);
        ExamCall examCall = openCall(20L, exam);
        examCall.setCurrentEnrollment(0);
        examCall.setTotalCapacity(30);

        User student = new User();
        student.setId(15L);

        Question firstQuestion = question(101L, exam, "Primera pregunta");
        Question secondQuestion = question(102L, exam, "Segunda pregunta");

        when(examCallRepository.findByIdForUpdate(20L)).thenReturn(Optional.of(examCall));
        when(examAttemptRepository.findByExamCallIdAndStudentId(20L, 15L)).thenReturn(Optional.empty());
        when(questionRepository.findByExamIdOrderByIdAsc(7L))
                .thenReturn(List.of(firstQuestion, secondQuestion));
        when(userRepository.findById(15L)).thenReturn(Optional.of(student));
        when(examAttemptRepository.saveAndFlush(any(ExamAttempt.class))).thenAnswer(invocation -> {
            ExamAttempt attempt = invocation.getArgument(0);
            attempt.setId(501L);
            long questionId = 900L;
            for (AttemptQuestion attemptQuestion : attempt.getQuestions()) {
                attemptQuestion.setId(questionId++);
            }
            return attempt;
        });

        var result = studentExamService.startAttempt(20L);

        assertEquals(501L, result.attemptId());
        assertEquals(AttemptStatus.IN_PROGRESS.name(), result.status());
        assertEquals(2, result.questions().size());
        assertEquals(1, result.questions().get(0).order());
        assertEquals(2, result.questions().get(1).order());
        assertEquals(1, examCall.getCurrentEnrollment());
    }

    @Test
    void availableListHidesAnExamAlreadySubmittedByTheStudent() {
        Exam exam = exam(7L);
        ExamCall examCall = openCall(20L, exam);

        ExamAttempt submitted = new ExamAttempt();
        submitted.setId(501L);
        submitted.setExamCall(examCall);
        submitted.setStatus(AttemptStatus.SUBMITTED);

        when(examCallRepository.findOpenCalls(any(LocalDateTime.class))).thenReturn(List.of(examCall));
        when(examAttemptRepository.findByStudentIdAndExamCallIdIn(15L, List.of(20L)))
                .thenReturn(List.of(submitted));

        assertFalse(studentExamService.findAvailableExams().iterator().hasNext());
    }

    @Test
    void studentCannotReadAnAttemptThatDoesNotBelongToThem() {
        when(examAttemptRepository.findByIdAndStudentId(999L, 15L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> studentExamService.findMyAttempt(999L));
    }

    @Test
    void submitRejectsAttemptWithMissingAnswers() {
        ExamAttempt attempt = attemptWithQuestions(false);
        when(examAttemptRepository.findByIdAndStudentId(501L, 15L)).thenReturn(Optional.of(attempt));

        var answers = List.of(new StudentExamService.AnswerCommand(901L, "Respuesta uno"));

        assertThrows(IllegalArgumentException.class,
                () -> studentExamService.submitAttempt(501L, answers));
    }

    @Test
    void submitStoresAnswersAndChangesStatus() {
        ExamAttempt attempt = attemptWithOneQuestion();
        when(examAttemptRepository.findByIdAndStudentId(501L, 15L)).thenReturn(Optional.of(attempt));
        when(examAttemptRepository.saveAndFlush(attempt)).thenReturn(attempt);

        var result = studentExamService.submitAttempt(
                501L,
                List.of(new StudentExamService.AnswerCommand(901L, "  respuesta final  "))
        );

        assertEquals(AttemptStatus.SUBMITTED, attempt.getStatus());
        assertEquals("respuesta final", attempt.getQuestions().get(0).getAnswerText());
        assertNotNull(attempt.getSubmittedAt());
        assertEquals(AttemptStatus.SUBMITTED.name(), result.status());
    }

    private ExamAttempt attemptWithQuestions(boolean answerSecondQuestion) {
        Exam exam = exam(7L);
        ExamCall call = openCall(20L, exam);

        ExamAttempt attempt = new ExamAttempt();
        attempt.setId(501L);
        attempt.setExamCall(call);
        attempt.setStartedAt(LocalDateTime.now().minusMinutes(5));
        attempt.setStatus(AttemptStatus.IN_PROGRESS);

        AttemptQuestion first = attemptQuestion(901L, attempt, question(101L, exam, "Pregunta 1"), 1);
        AttemptQuestion second = attemptQuestion(902L, attempt, question(102L, exam, "Pregunta 2"), 2);
        if (answerSecondQuestion) {
            second.setAnswerText("Respuesta dos");
        }
        attempt.getQuestions().addAll(List.of(first, second));
        return attempt;
    }

    private ExamAttempt attemptWithOneQuestion() {
        Exam exam = exam(7L);
        ExamCall call = openCall(20L, exam);

        ExamAttempt attempt = new ExamAttempt();
        attempt.setId(501L);
        attempt.setExamCall(call);
        attempt.setStartedAt(LocalDateTime.now().minusMinutes(5));
        attempt.setStatus(AttemptStatus.IN_PROGRESS);
        attempt.getQuestions().add(
                attemptQuestion(901L, attempt, question(101L, exam, "Pregunta 1"), 1)
        );
        return attempt;
    }

    private AttemptQuestion attemptQuestion(Long id,
                                            ExamAttempt attempt,
                                            Question question,
                                            int order) {
        AttemptQuestion attemptQuestion = new AttemptQuestion();
        attemptQuestion.setId(id);
        attemptQuestion.setAttempt(attempt);
        attemptQuestion.setQuestion(question);
        attemptQuestion.setQuestionOrder(order);
        return attemptQuestion;
    }

    private Question question(Long id, Exam exam, String statement) {
        Question question = new Question();
        question.setId(id);
        question.setExam(exam);
        question.setStatement(statement);
        question.setType(QuestionType.OPEN);
        question.setPoints(10);
        return question;
    }

    private Exam exam(Long id) {
        User professor = new User();
        professor.setId(3L);
        professor.setName("Profesor Demo");

        Exam exam = new Exam();
        ReflectionTestUtils.setField(exam, "id", id);
        exam.setTitle("Examen de prueba");
        exam.setDescription("Descripción");
        exam.setDurationMinutes(60);
        exam.setProfessor(professor);
        return exam;
    }

    private ExamCall openCall(Long id, Exam exam) {
        ExamCall examCall = new ExamCall();
        examCall.setId(id);
        examCall.setExam(exam);
        examCall.setStartDate(LocalDateTime.now().minusHours(1));
        examCall.setEndDate(LocalDateTime.now().plusHours(1));
        return examCall;
    }
}
