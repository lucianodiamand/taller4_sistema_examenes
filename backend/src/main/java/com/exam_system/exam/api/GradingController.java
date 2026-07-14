package com.exam_system.exam.api;

import com.exam_system.exam.application.GradingService;
import com.exam_system.exam.domain.AttemptQuestion;
import com.exam_system.exam.domain.ExamAttempt;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * REST endpoints for grading and history.
 *
 * Professor endpoints (requires exams.grade permission when auth is integrated):
 *   GET  /api/grading/attempts?professorId={id}          → all attempts for professor's exams
 *   GET  /api/grading/exams/{examId}/attempts?professorId → attempts for a specific exam
 *   GET  /api/grading/attempts/{attemptId}?professorId    → single attempt detail
 *   PATCH /api/grading/attempts/{attemptId}/questions/{qId} → grade one question
 *   POST  /api/grading/attempts/{attemptId}/close?professorId → compute finalScore + set GRADED
 *
 * Student endpoints (requires exam.results.read.self when auth is integrated):
 *   GET  /api/grading/my-results?studentId={id}          → all attempts with results
 *   GET  /api/grading/my-results/{attemptId}?studentId   → single result detail
 *
 * TODO: Replace ?professorId / ?studentId query params with JWT principal once
 *       feat/back-auth-jwt is merged. Use @CurrentUser annotation from auth module.
 */
@RestController
@RequestMapping("/api/grading")
public class GradingController {

    private final GradingService gradingService;

    public GradingController(GradingService gradingService) {
        this.gradingService = gradingService;
    }

    // =========================================================================
    // Professor — views
    // =========================================================================

    @GetMapping("/attempts")
    public List<AttemptSummaryResponse> getAttemptsForProfessor(
            @RequestParam @NotNull Long professorId) {
        return gradingService.getAttemptsForProfessor(professorId)
                .stream()
                .map(AttemptSummaryResponse::from)
                .toList();
    }

    @GetMapping("/exams/{examId}/attempts")
    public List<AttemptSummaryResponse> getAttemptsForExam(
            @PathVariable Long examId,
            @RequestParam @NotNull Long professorId) {
        return gradingService.getAttemptsForExam(examId, professorId)
                .stream()
                .map(AttemptSummaryResponse::from)
                .toList();
    }

    @GetMapping("/attempts/{attemptId}")
    public AttemptDetailResponse getAttemptDetail(
            @PathVariable Long attemptId,
            @RequestParam @NotNull Long professorId) {
        ExamAttempt attempt = gradingService.getAttemptForProfessor(attemptId, professorId);
        return AttemptDetailResponse.from(attempt);
    }

    // =========================================================================
    // Professor — grading actions
    // =========================================================================

    @PatchMapping("/attempts/{attemptId}/questions/{questionId}")
    public QuestionGradeResponse gradeQuestion(
            @PathVariable Long attemptId,
            @PathVariable Long questionId,
            @Valid @RequestBody GradeQuestionRequest request,
            @RequestParam @NotNull Long professorId) {
        AttemptQuestion aq = gradingService.gradeQuestion(
                attemptId, questionId, request.awardedScore(), request.reviewComment(), professorId);
        return new QuestionGradeResponse(aq.getId(), aq.getAwardedScore(), aq.getReviewComment());
    }

    @PostMapping("/attempts/{attemptId}/close")
    @ResponseStatus(HttpStatus.OK)
    public AttemptSummaryResponse closeGrading(
            @PathVariable Long attemptId,
            @RequestParam @NotNull Long professorId) {
        ExamAttempt attempt = gradingService.closeGrading(attemptId, professorId);
        return AttemptSummaryResponse.from(attempt);
    }

    // =========================================================================
    // Student — result views
    // =========================================================================

    @GetMapping("/my-results")
    public List<StudentResultResponse> getMyResults(
            @RequestParam @NotNull Long studentId) {
        return gradingService.getResultsForStudent(studentId)
                .stream()
                .map(StudentResultResponse::from)
                .toList();
    }

    @GetMapping("/my-results/{attemptId}")
    public AttemptDetailResponse getMyResult(
            @PathVariable Long attemptId,
            @RequestParam @NotNull Long studentId) {
        ExamAttempt attempt = gradingService.getResultForStudent(attemptId, studentId);
        return AttemptDetailResponse.from(attempt);
    }

    // =========================================================================
    // Records (DTOs)
    // =========================================================================

    public record GradeQuestionRequest(
            @NotNull(message = "awardedScore is required")
            @DecimalMin(value = "0.0", message = "Score must be non-negative")
            BigDecimal awardedScore,
            String reviewComment
    ) {
    }

    public record AttemptSummaryResponse(
            Long attemptId,
            Long examCallId,
            Long studentId,
            String studentName,
            String status,
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
            String status,
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
            String status,
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
}
