package com.exam_system.exam.api;

import com.exam_system.exam.application.ExamWorkflowService;
import com.exam_system.exam.application.ExamWorkflowService.AnswerInput;
import com.exam_system.exam.application.ExamWorkflowService.GradeAttemptInput;
import com.exam_system.exam.application.ExamWorkflowService.ResultView;
import com.exam_system.exam.application.ExamWorkflowService.ValidationCommentView;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/exam-workflow")
public class ExamWorkflowController {

    private final ExamWorkflowService examWorkflowService;

    public ExamWorkflowController(ExamWorkflowService examWorkflowService) {
        this.examWorkflowService = examWorkflowService;
    }

    @PostMapping("/calls/{examCallId}/solve")
    @PreAuthorize("hasAuthority('exams.solve')")
    public AttemptResponse solve(@PathVariable Long examCallId,
                                 @RequestBody(required = false) SolveAttemptRequest request) {
        List<AnswerInput> answers = request == null ? List.of() : request.answers().stream()
                .map(a -> new AnswerInput(a.attemptQuestionId(), a.answerText()))
                .toList();

        var attempt = examWorkflowService.solve(examCallId, answers);
        return new AttemptResponse(attempt.getId(), attempt.getStatus(), attempt.getFinalScore());
    }

    @PatchMapping("/attempts/{attemptId}/grade")
    @PreAuthorize("hasAuthority('exams.grade')")
    public AttemptResponse grade(@PathVariable Long attemptId,
                                 @Valid @RequestBody GradeAttemptRequest request) {
        var attempt = examWorkflowService.grade(attemptId, new GradeAttemptInput(request.finalScore()));
        return new AttemptResponse(attempt.getId(), attempt.getStatus(), attempt.getFinalScore());
    }

    @GetMapping("/my-validations")
    @PreAuthorize("hasAuthority('exam.validations.read.self')")
    public List<ValidationCommentView> myValidations() {
        return examWorkflowService.myValidations();
    }

    @GetMapping("/my-results")
    @PreAuthorize("hasAuthority('exam.results.read.self')")
    public List<ResultView> myResults() {
        return examWorkflowService.myResults();
    }

    public record SolveAttemptRequest(List<AnswerRequest> answers) {
    }

    public record AnswerRequest(Long attemptQuestionId, String answerText) {
    }

    public record GradeAttemptRequest(
            @NotNull(message = "finalScore is required")
            BigDecimal finalScore
    ) {
    }

    public record AttemptResponse(Long attemptId, String status, BigDecimal finalScore) {
    }
}
