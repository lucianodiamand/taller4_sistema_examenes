package com.exam_system.exam.application;

import com.exam_system.auth.security.CurrentUser;
import com.exam_system.exam.domain.AttemptQuestion;
import com.exam_system.exam.domain.ExamAttempt;
import com.exam_system.exam.domain.ExamCall;
import com.exam_system.exam.repository.ExamAttemptRepository;
import com.exam_system.exam.repository.ExamCallRepository;
import com.exam_system.user.domain.User;
import com.exam_system.user.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ExamWorkflowService {

    private final ExamCallRepository examCallRepository;
    private final ExamAttemptRepository examAttemptRepository;
    private final UserRepository userRepository;
    private final CurrentUser currentUser;

    public ExamWorkflowService(ExamCallRepository examCallRepository,
                               ExamAttemptRepository examAttemptRepository,
                               UserRepository userRepository,
                               CurrentUser currentUser) {
        this.examCallRepository = examCallRepository;
        this.examAttemptRepository = examAttemptRepository;
        this.userRepository = userRepository;
        this.currentUser = currentUser;
    }

    @Transactional
    public ExamAttempt solve(Long examCallId, List<AnswerInput> answers) {
        Long studentId = currentUser.id();
        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new EntityNotFoundException("Student not found"));

        ExamCall examCall = examCallRepository.findById(examCallId)
                .orElseThrow(() -> new EntityNotFoundException("Exam call not found"));

        ExamAttempt attempt = examAttemptRepository.findByExamCallIdAndStudentId(examCallId, studentId)
                .orElseGet(() -> {
                    ExamAttempt created = new ExamAttempt();
                    created.setExamCall(examCall);
                    created.setStudent(student);
                    created.setStartedAt(LocalDateTime.now());
                    return created;
                });

        if (answers != null) {
            for (AnswerInput answer : answers) {
                for (AttemptQuestion question : attempt.getQuestions()) {
                    if (question.getId() != null && question.getId().equals(answer.attemptQuestionId())) {
                        question.setAnswerText(answer.answerText());
                    }
                }
            }
        }

        attempt.setStatus("SUBMITTED");
        attempt.setSubmittedAt(LocalDateTime.now());
        return examAttemptRepository.save(attempt);
    }

    @Transactional
    public ExamAttempt grade(Long attemptId, GradeAttemptInput input) {
        ExamAttempt attempt = examAttemptRepository.findById(attemptId)
                .orElseThrow(() -> new EntityNotFoundException("Attempt not found"));
        attempt.setFinalScore(input.finalScore());
        attempt.setStatus("GRADED");
        return examAttemptRepository.save(attempt);
    }

    @Transactional(readOnly = true)
    public List<ValidationCommentView> myValidations() {
        Long studentId = currentUser.id();
        return examAttemptRepository.findByStudentId(studentId).stream()
                .flatMap(a -> a.getQuestions().stream()
                        .filter(q -> q.getReviewComment() != null && !q.getReviewComment().isBlank())
                        .map(q -> new ValidationCommentView(
                                a.getId(),
                                q.getId(),
                                q.getReviewComment()
                        )))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ResultView> myResults() {
        Long studentId = currentUser.id();
        return examAttemptRepository.findByStudentId(studentId).stream()
                .map(a -> new ResultView(a.getId(), a.getStatus(), a.getFinalScore()))
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
