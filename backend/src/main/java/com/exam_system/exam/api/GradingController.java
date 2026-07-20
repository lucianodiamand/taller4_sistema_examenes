package com.exam_system.exam.api;

import com.exam_system.auth.security.CurrentUser;
import com.exam_system.exam.application.GradingService;
import com.exam_system.exam.domain.AttemptQuestion;
import com.exam_system.exam.domain.AttemptStatus;
import com.exam_system.exam.domain.ExamAttempt;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Endpoints de corrección e historial de exámenes.
 *
 * Profesor (permiso exams.grade):
 *   GET  /api/grading/attempts                         → todas las resoluciones de mis exámenes
 *   GET  /api/grading/exams/{examId}/attempts         → resoluciones de un examen específico
 *   GET  /api/grading/attempts/{attemptId}            → detalle de una resolución
 *   PATCH /api/grading/attempts/{attemptId}/questions/{qId} → calificar una pregunta
 *   POST  /api/grading/attempts/{attemptId}/close     → cerrar corrección y calcular nota
 *
 * Estudiante:
 *   GET  /api/grading/my-results                       → mis resoluciones con resultados
 *   GET  /api/grading/my-results/{attemptId}           → detalle de una resolución propia
 *   GET  /api/grading/my-validations                   → comentarios de corrección recibidos
 */
@RestController
@RequestMapping("/api/grading")
public class GradingController {

    private final GradingService gradingService;
    private final CurrentUser currentUser;

    public GradingController(GradingService gradingService, CurrentUser currentUser) {
        this.gradingService = gradingService;
        this.currentUser = currentUser;
    }

    @GetMapping("/attempts")
    @PreAuthorize("hasRole('PROFESSOR') and hasAuthority('exams.grade')")
    public List<AttemptSummaryResponse> getAttemptsForProfessor() {
        return gradingService.getAttemptsForProfessor(currentUser.id())
                .stream()
                .map(AttemptSummaryResponse::from)
                .toList();
    }

    @GetMapping("/exams/{examId}/attempts")
    @PreAuthorize("hasRole('PROFESSOR') and hasAuthority('exams.grade')")
    public List<AttemptSummaryResponse> getAttemptsForExam(@PathVariable Long examId) {
        return gradingService.getAttemptsForExam(examId, currentUser.id())
                .stream()
                .map(AttemptSummaryResponse::from)
                .toList();
    }

    @GetMapping("/attempts/{attemptId}")
    @PreAuthorize("hasRole('PROFESSOR') and hasAuthority('exams.grade')")
    public AttemptDetailResponse getAttemptDetail(@PathVariable Long attemptId) {
        ExamAttempt attempt = gradingService.getAttemptForProfessor(attemptId, currentUser.id());
        return AttemptDetailResponse.from(attempt);
    }

    @PatchMapping("/attempts/{attemptId}/questions/{questionId}")
    @PreAuthorize("hasRole('PROFESSOR') and hasAuthority('exams.grade')")
    public QuestionGradeResponse gradeQuestion(
            @PathVariable Long attemptId,
            @PathVariable Long questionId,
            @Valid @RequestBody GradeQuestionRequest request) {
        AttemptQuestion aq = gradingService.gradeQuestion(
                attemptId, questionId, request.awardedScore(), request.reviewComment(), currentUser.id());
        return new QuestionGradeResponse(aq.getId(), aq.getAwardedScore(), aq.getReviewComment());
    }

    @PostMapping("/attempts/{attemptId}/close")
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasRole('PROFESSOR') and hasAuthority('exams.grade')")
    public AttemptSummaryResponse closeGrading(@PathVariable Long attemptId) {
        ExamAttempt attempt = gradingService.closeGrading(attemptId, currentUser.id());
        return AttemptSummaryResponse.from(attempt);
    }

    @GetMapping("/my-results")
    @PreAuthorize("hasRole('STUDENT') and hasAuthority('exam.results.read.self')")
    public List<StudentResultResponse> getMyResults() {
        return gradingService.getResultsForStudent(currentUser.id())
                .stream()
                .map(StudentResultResponse::from)
                .toList();
    }

    @GetMapping("/my-results/{attemptId}")
    @PreAuthorize("hasRole('STUDENT') and hasAuthority('exam.results.read.self')")
    public AttemptDetailResponse getMyResult(@PathVariable Long attemptId) {
        ExamAttempt attempt = gradingService.getResultForStudent(attemptId, currentUser.id());
        return AttemptDetailResponse.from(attempt);
    }

    @GetMapping("/my-validations")
    @PreAuthorize("hasRole('STUDENT') and hasAuthority('exam.validations.read.self')")
    public List<ValidationCommentResponse> getMyValidations() {
        return gradingService.getValidationCommentsForStudent(currentUser.id())
                .stream()
                .map(comment -> new ValidationCommentResponse(
                        comment.attemptId(),
                        comment.attemptQuestionId(),
                        comment.comment()
                ))
                .toList();
    }

    public record GradeQuestionRequest(
            @NotNull(message = "El puntaje asignado es obligatorio")
            @DecimalMin(value = "0.0", message = "El puntaje no puede ser negativo")
            BigDecimal awardedScore,
            String reviewComment
    ) {
    }

    public record AttemptSummaryResponse(
            Long attemptId,
            Long examCallId,
            Long studentId,
            String studentName,
            AttemptStatus status,
            BigDecimal finalScore,
            LocalDateTime startedAt,
            LocalDateTime submittedAt
    ) {
        static AttemptSummaryResponse from(ExamAttempt a) {
            return new AttemptSummaryResponse(
                    a.getId(),
                    a.getExamCall().getId(),
                    a.getStudent().getId(),
                    a.getStudent().getName(),
                    a.getStatus(),
                    a.getFinalScore(),
                    a.getStartedAt(),
                    a.getSubmittedAt()
            );
        }
    }

    public record AttemptDetailResponse(
            Long attemptId,
            Long examCallId,
            Long studentId,
            String studentName,
            AttemptStatus status,
            BigDecimal finalScore,
            LocalDateTime startedAt,
            LocalDateTime submittedAt,
            List<AttemptQuestionResponse> questions
    ) {
        static AttemptDetailResponse from(ExamAttempt a) {
            List<AttemptQuestionResponse> qs = a.getQuestions().stream()
                    .map(q -> new AttemptQuestionResponse(
                            q.getId(),
                            q.getQuestion().getId(),
                            q.getQuestion().getStatement(),
                            q.getQuestionOrder(),
                            q.getAnswerText(),
                            q.getAwardedScore(),
                            q.getReviewComment()
                    ))
                    .toList();
            return new AttemptDetailResponse(
                    a.getId(),
                    a.getExamCall().getId(),
                    a.getStudent().getId(),
                    a.getStudent().getName(),
                    a.getStatus(),
                    a.getFinalScore(),
                    a.getStartedAt(),
                    a.getSubmittedAt(),
                    qs
            );
        }
    }

    public record AttemptQuestionResponse(
            Long attemptQuestionId,
            Long questionId,
            String statement,
            Integer questionOrder,
            String answerText,
            BigDecimal awardedScore,
            String reviewComment
    ) {
    }

    public record QuestionGradeResponse(
            Long attemptQuestionId,
            BigDecimal awardedScore,
            String reviewComment
    ) {
    }

    public record StudentResultResponse(
            Long attemptId,
            Long examCallId,
            AttemptStatus status,
            BigDecimal finalScore,
            LocalDateTime submittedAt
    ) {
        static StudentResultResponse from(ExamAttempt a) {
            return new StudentResultResponse(
                    a.getId(),
                    a.getExamCall().getId(),
                    a.getStatus(),
                    a.getFinalScore(),
                    a.getSubmittedAt()
            );
        }
    }

    public record ValidationCommentResponse(
            Long attemptId,
            Long attemptQuestionId,
            String comment
    ) {
    }
}
