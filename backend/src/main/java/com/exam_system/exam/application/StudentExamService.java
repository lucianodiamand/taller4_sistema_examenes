package com.exam_system.exam.application;

import com.exam_system.auth.security.CurrentUser;
import com.exam_system.exam.domain.AttemptQuestion;
import com.exam_system.exam.domain.AttemptStatus;
import com.exam_system.exam.domain.Exam;
import com.exam_system.exam.domain.ExamAttempt;
import com.exam_system.exam.domain.ExamCall;
import com.exam_system.exam.domain.Question;
import com.exam_system.exam.repository.ExamAttemptRepository;
import com.exam_system.exam.repository.ExamCallRepository;
import com.exam_system.exam.repository.QuestionRepository;
import com.exam_system.shared.api.ConflictException;
import com.exam_system.user.domain.User;
import com.exam_system.user.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Reúne el flujo de resolución del estudiante. Toda búsqueda de intentos se hace
 * usando el usuario autenticado para no depender de ids enviados desde el frontend.
 */
@Service
public class StudentExamService {

    private static final Logger logger = LoggerFactory.getLogger(StudentExamService.class);

    private final ExamCallRepository examCallRepository;
    private final ExamAttemptRepository examAttemptRepository;
    private final QuestionRepository questionRepository;
    private final UserRepository userRepository;
    private final CurrentUser currentUser;

    public StudentExamService(ExamCallRepository examCallRepository,
                              ExamAttemptRepository examAttemptRepository,
                              QuestionRepository questionRepository,
                              UserRepository userRepository,
                              CurrentUser currentUser) {
        this.examCallRepository = examCallRepository;
        this.examAttemptRepository = examAttemptRepository;
        this.questionRepository = questionRepository;
        this.userRepository = userRepository;
        this.currentUser = currentUser;
    }

    @Transactional(readOnly = true)
    public List<AvailableExamView> findAvailableExams() {
        Long studentId = currentUser.id();
        LocalDateTime now = LocalDateTime.now();
        List<ExamCall> openCalls = examCallRepository.findOpenCalls(now);

        if (openCalls.isEmpty()) {
            return List.of();
        }

        List<Long> callIds = openCalls.stream().map(call -> call.getId()).toList();
        Map<Long, ExamAttempt> attemptsByCall = examAttemptRepository
                .findByStudentIdAndExamCallIdIn(studentId, callIds)
                .stream()
                .collect(Collectors.toMap(
                        attempt -> attempt.getExamCall().getId(),
                        Function.identity()
                ));

        return openCalls.stream()
                .filter(call -> isAvailableForStudent(call, attemptsByCall.get(call.getId()), now))
                .map(call -> toAvailableView(call, attemptsByCall.get(call.getId())))
                .toList();
    }

    @Transactional
    public AttemptDetailView startAttempt(Long examCallId) {
        Long studentId = currentUser.id();
        LocalDateTime now = LocalDateTime.now();

        // La convocatoria se bloquea sólo durante el alta para respetar el cupo.
        ExamCall examCall = examCallRepository.findByIdForUpdate(examCallId)
                .orElseThrow(() -> new EntityNotFoundException("Convocatoria no encontrada"));
        validateOpenCall(examCall, now);

        var existingAttempt = examAttemptRepository.findByExamCallIdAndStudentId(examCallId, studentId);
        if (existingAttempt.isPresent()) {
            ExamAttempt attempt = existingAttempt.get();
            validateEditableAttempt(attempt, now);
            return toAttemptDetail(attempt);
        }

        validateCapacity(examCall);

        Exam exam = requireExam(examCall);
        List<Question> questions = questionRepository.findByExamIdOrderByIdAsc(exam.getId());
        if (questions.isEmpty()) {
            throw new ConflictException("El examen todavía no tiene preguntas para resolver");
        }

        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new EntityNotFoundException("Estudiante no encontrado"));

        ExamAttempt attempt = new ExamAttempt();
        attempt.setExamCall(examCall);
        attempt.setStudent(student);
        attempt.setStartedAt(now);
        attempt.setStatus(AttemptStatus.IN_PROGRESS);

        // Cada intento conserva su propio orden. Las respuestas se guardan sobre
        // estas copias y nunca modifican las preguntas originales del examen.
        List<Question> shuffledQuestions = new ArrayList<>(questions);
        Collections.shuffle(shuffledQuestions);
        for (int index = 0; index < shuffledQuestions.size(); index++) {
            AttemptQuestion attemptQuestion = new AttemptQuestion();
            attemptQuestion.setAttempt(attempt);
            attemptQuestion.setQuestion(shuffledQuestions.get(index));
            attemptQuestion.setQuestionOrder(index + 1);
            attempt.getQuestions().add(attemptQuestion);
        }

        int currentEnrollment = valueOrZero(examCall.getCurrentEnrollment());
        examCall.setCurrentEnrollment(currentEnrollment + 1);
        examCallRepository.save(examCall);

        ExamAttempt savedAttempt = examAttemptRepository.saveAndFlush(attempt);
        logger.info("Student {} started attempt {} for exam call {}", studentId, savedAttempt.getId(), examCallId);
        return toAttemptDetail(savedAttempt);
    }

    @Transactional
    public AttemptDetailView saveAnswers(Long attemptId, List<AnswerCommand> answers) {
        ExamAttempt attempt = findOwnedAttempt(attemptId);
        validateEditableAttempt(attempt, LocalDateTime.now());
        applyAnswers(attempt, answers);

        ExamAttempt savedAttempt = examAttemptRepository.saveAndFlush(attempt);
        return toAttemptDetail(savedAttempt);
    }

    @Transactional
    public AttemptDetailView submitAttempt(Long attemptId, List<AnswerCommand> answers) {
        ExamAttempt attempt = findOwnedAttempt(attemptId);
        LocalDateTime now = LocalDateTime.now();
        validateEditableAttempt(attempt, now);
        applyAnswers(attempt, answers);
        validateCompleteAttempt(attempt);

        attempt.setStatus(AttemptStatus.SUBMITTED);
        attempt.setSubmittedAt(now);

        ExamAttempt savedAttempt = examAttemptRepository.saveAndFlush(attempt);
        logger.info("Student {} submitted attempt {}", currentUser.id(), attemptId);
        return toAttemptDetail(savedAttempt);
    }

    @Transactional(readOnly = true)
    public List<AttemptSummaryView> findMyAttempts() {
        return examAttemptRepository.findByStudentIdOrderByStartedAtDesc(currentUser.id())
                .stream()
                .map(this::toAttemptSummary)
                .toList();
    }

    @Transactional(readOnly = true)
    public AttemptDetailView findMyAttempt(Long attemptId) {
        return toAttemptDetail(findOwnedAttempt(attemptId));
    }

    private ExamAttempt findOwnedAttempt(Long attemptId) {
        return examAttemptRepository.findByIdAndStudentId(attemptId, currentUser.id())
                .orElseThrow(() -> new EntityNotFoundException("Resolución no encontrada"));
    }

    private void applyAnswers(ExamAttempt attempt, List<AnswerCommand> answers) {
        if (answers == null || answers.isEmpty()) {
            return;
        }

        Map<Long, AttemptQuestion> questionsById = attempt.getQuestions().stream()
                .collect(Collectors.toMap(q -> q.getId(), q -> q));
        Set<Long> receivedIds = new HashSet<>();

        for (AnswerCommand answer : answers) {
            if (!receivedIds.add(answer.attemptQuestionId())) {
                throw new IllegalArgumentException("No se puede enviar dos veces la misma respuesta");
            }

            AttemptQuestion attemptQuestion = questionsById.get(answer.attemptQuestionId());
            if (attemptQuestion == null) {
                throw new IllegalArgumentException("Una de las preguntas no pertenece a esta resolución");
            }

            String answerText = answer.answerText();
            attemptQuestion.setAnswerText(answerText == null ? null : answerText.trim());
        }
    }

    private void validateCompleteAttempt(ExamAttempt attempt) {
        boolean hasMissingAnswers = attempt.getQuestions().stream()
                .anyMatch(question -> question.getAnswerText() == null || question.getAnswerText().isBlank());

        if (hasMissingAnswers) {
            throw new IllegalArgumentException("Tenés que responder todas las preguntas antes de enviar el examen");
        }
    }

    private void validateEditableAttempt(ExamAttempt attempt, LocalDateTime now) {
        if (attempt.getStatus() != AttemptStatus.IN_PROGRESS) {
            throw new ConflictException("La resolución ya fue enviada y no admite cambios");
        }

        validateOpenCall(attempt.getExamCall(), now);

        Exam exam = requireExam(attempt.getExamCall());
        LocalDateTime durationDeadline = attempt.getStartedAt().plusMinutes(exam.getDurationMinutes());
        if (now.isAfter(durationDeadline)) {
            throw new ConflictException("El tiempo disponible para resolver el examen terminó");
        }
    }

    private void validateOpenCall(ExamCall examCall, LocalDateTime now) {
        if (examCall.getStartDate() == null || examCall.getEndDate() == null) {
            throw new ConflictException("La convocatoria todavía no tiene una ventana horaria válida");
        }
        if (now.isBefore(examCall.getStartDate())) {
            throw new ConflictException("La convocatoria todavía no comenzó");
        }
        if (now.isAfter(examCall.getEndDate())) {
            throw new ConflictException("La convocatoria ya finalizó");
        }
    }

    private void validateCapacity(ExamCall examCall) {
        Integer totalCapacity = examCall.getTotalCapacity();
        int currentEnrollment = valueOrZero(examCall.getCurrentEnrollment());

        if (totalCapacity != null && currentEnrollment >= totalCapacity) {
            throw new ConflictException("La convocatoria ya no tiene cupos disponibles");
        }
    }

    private boolean isAvailableForStudent(ExamCall examCall, ExamAttempt attempt, LocalDateTime now) {
        if (attempt == null) {
            return hasCapacity(examCall);
        }
        if (attempt.getStatus() != AttemptStatus.IN_PROGRESS) {
            return false;
        }

        Exam exam = examCall.getExam();
        if (exam == null || attempt.getStartedAt() == null || exam.getDurationMinutes() == null) {
            return false;
        }
        return !now.isAfter(attempt.getStartedAt().plusMinutes(exam.getDurationMinutes()));
    }

    private boolean hasCapacity(ExamCall examCall) {
        return examCall.getTotalCapacity() == null
                || valueOrZero(examCall.getCurrentEnrollment()) < examCall.getTotalCapacity();
    }

    private Exam requireExam(ExamCall examCall) {
        if (examCall.getExam() == null) {
            throw new ConflictException("La convocatoria no tiene un examen asociado");
        }
        return examCall.getExam();
    }

    private AvailableExamView toAvailableView(ExamCall examCall, ExamAttempt attempt) {
        Exam exam = requireExam(examCall);
        User professor = exam.getProfessor();

        return new AvailableExamView(
                examCall.getId(),
                exam.getId(),
                exam.getTitle(),
                exam.getDescription(),
                exam.getDurationMinutes(),
                examCall.getStartDate(),
                examCall.getEndDate(),
                professor == null ? null : professor.getName(),
                remainingCapacity(examCall),
                attempt == null ? null : attempt.getId(),
                attempt == null ? null : attempt.getStatus().name()
        );
    }

    private AttemptSummaryView toAttemptSummary(ExamAttempt attempt) {
        Exam exam = requireExam(attempt.getExamCall());
        boolean graded = attempt.getStatus() == AttemptStatus.GRADED;

        return new AttemptSummaryView(
                attempt.getId(),
                attempt.getExamCall().getId(),
                exam.getId(),
                exam.getTitle(),
                attempt.getStatus().name(),
                attempt.getStartedAt(),
                attempt.getSubmittedAt(),
                graded ? attempt.getFinalScore() : null
        );
    }

    private AttemptDetailView toAttemptDetail(ExamAttempt attempt) {
        Exam exam = requireExam(attempt.getExamCall());
        boolean graded = attempt.getStatus() == AttemptStatus.GRADED;
        LocalDateTime deadline = calculateDeadline(attempt, exam);

        List<AttemptQuestionView> questions = attempt.getQuestions().stream()
                .map(question -> new AttemptQuestionView(
                        question.getId(),
                        question.getQuestion().getId(),
                        question.getQuestionOrder(),
                        question.getQuestion().getStatement(),
                        question.getQuestion().getType().name(),
                        question.getQuestion().getPoints(),
                        question.getAnswerText(),
                        graded ? question.getAwardedScore() : null,
                        graded ? question.getReviewComment() : null
                ))
                .toList();

        return new AttemptDetailView(
                attempt.getId(),
                attempt.getExamCall().getId(),
                exam.getId(),
                exam.getTitle(),
                exam.getDescription(),
                attempt.getStatus().name(),
                attempt.getStartedAt(),
                deadline,
                attempt.getSubmittedAt(),
                graded ? attempt.getFinalScore() : null,
                questions
        );
    }

    private LocalDateTime calculateDeadline(ExamAttempt attempt, Exam exam) {
        LocalDateTime durationDeadline = attempt.getStartedAt().plusMinutes(exam.getDurationMinutes());
        LocalDateTime callDeadline = attempt.getExamCall().getEndDate();

        if (callDeadline == null || durationDeadline.isBefore(callDeadline)) {
            return durationDeadline;
        }
        return callDeadline;
    }

    private Integer remainingCapacity(ExamCall examCall) {
        if (examCall.getTotalCapacity() == null) {
            return null;
        }
        return Math.max(examCall.getTotalCapacity() - valueOrZero(examCall.getCurrentEnrollment()), 0);
    }

    private int valueOrZero(Integer value) {
        return value == null ? 0 : value;
    }

    public record AnswerCommand(Long attemptQuestionId, String answerText) {
    }

    public record AvailableExamView(
            Long examCallId,
            Long examId,
            String title,
            String description,
            Integer durationMinutes,
            LocalDateTime startsAt,
            LocalDateTime endsAt,
            String professorName,
            Integer remainingCapacity,
            Long attemptId,
            String attemptStatus
    ) {
    }

    public record AttemptSummaryView(
            Long attemptId,
            Long examCallId,
            Long examId,
            String examTitle,
            String status,
            LocalDateTime startedAt,
            LocalDateTime submittedAt,
            BigDecimal finalScore
    ) {
    }

    public record AttemptDetailView(
            Long attemptId,
            Long examCallId,
            Long examId,
            String examTitle,
            String examDescription,
            String status,
            LocalDateTime startedAt,
            LocalDateTime deadline,
            LocalDateTime submittedAt,
            BigDecimal finalScore,
            List<AttemptQuestionView> questions
    ) {
    }

    public record AttemptQuestionView(
            Long attemptQuestionId,
            Long questionId,
            Integer order,
            String statement,
            String type,
            Integer points,
            String answerText,
            BigDecimal awardedScore,
            String reviewComment
    ) {
    }
}
