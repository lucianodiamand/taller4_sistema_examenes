package com.exam_system.exam.application;

import com.exam_system.auth.security.CurrentUser;
import com.exam_system.exam.domain.AttemptStatus;
import com.exam_system.exam.domain.ExamAttempt;
import com.exam_system.exam.repository.ExamAttemptRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class ExamWorkflowService {

    private final ExamAttemptRepository examAttemptRepository;
    private final CurrentUser currentUser;
    private final StudentExamService studentExamService;

    public ExamWorkflowService(ExamAttemptRepository examAttemptRepository,
                               CurrentUser currentUser,
                               StudentExamService studentExamService) {
        this.examAttemptRepository = examAttemptRepository;
        this.currentUser = currentUser;
        this.studentExamService = studentExamService;
    }

    /**
     * Mantiene compatible el endpoint anterior, pero usa el mismo flujo seguro
     * que los endpoints nuevos del estudiante.
     */
    @Transactional
    public ExamAttempt solve(Long examCallId, List<AnswerInput> answers) {
        var startedAttempt = studentExamService.startAttempt(examCallId);
        List<StudentExamService.AnswerCommand> commands = answers == null
                ? List.of()
                : answers.stream()
                .map(answer -> new StudentExamService.AnswerCommand(
                        answer.attemptQuestionId(),
                        answer.answerText()
                ))
                .toList();

        studentExamService.submitAttempt(startedAttempt.attemptId(), commands);
        return examAttemptRepository.findById(startedAttempt.attemptId())
                .orElseThrow(() -> new EntityNotFoundException("Resolución no encontrada"));
    }

    @Transactional
    public ExamAttempt grade(Long attemptId, GradeAttemptInput input) {
        ExamAttempt attempt = examAttemptRepository.findById(attemptId)
                .orElseThrow(() -> new EntityNotFoundException("Attempt not found"));
        attempt.setFinalScore(input.finalScore());
        attempt.setStatus(AttemptStatus.GRADED);
        return examAttemptRepository.save(attempt);
    }

    @Transactional(readOnly = true)
    public List<ValidationCommentView> myValidations() {
        Long studentId = currentUser.id();
        return examAttemptRepository.findByStudentId(studentId).stream()
                .flatMap(attempt -> attempt.getQuestions().stream()
                        .filter(question -> question.getReviewComment() != null
                                && !question.getReviewComment().isBlank())
                        .map(question -> new ValidationCommentView(
                                attempt.getId(),
                                question.getId(),
                                question.getReviewComment()
                        )))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ResultView> myResults() {
        Long studentId = currentUser.id();
        return examAttemptRepository.findByStudentId(studentId).stream()
                .map(attempt -> new ResultView(
                        attempt.getId(),
                        attempt.getStatus().name(),
                        attempt.getFinalScore()
                ))
                .toList();
    }

    public record AnswerInput(Long attemptQuestionId, String answerText) {
    }

    public record GradeAttemptInput(BigDecimal finalScore) {
    }

    public record ValidationCommentView(Long attemptId, Long attemptQuestionId, String comment) {
    }

    public record ResultView(Long attemptId, String status, BigDecimal finalScore) {
    }
}
