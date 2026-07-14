package com.exam_system.exam.application;

import com.exam_system.exam.domain.AttemptQuestion;
import com.exam_system.exam.domain.ExamAttempt;
import com.exam_system.exam.repository.ExamAttemptRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Handles grading logic: professors can correct submitted attempts
 * that belong to their own exams, and students can view their own results.
 * All visibility rules are enforced here.
 */
@Service
public class GradingService {

    private final ExamAttemptRepository examAttemptRepository;

    public GradingService(ExamAttemptRepository examAttemptRepository) {
        this.examAttemptRepository = examAttemptRepository;
    }

    // -------------------------------------------------------------------------
    // Professor views
    // -------------------------------------------------------------------------

    /**
     * Returns all submitted/graded attempts for ALL exams belonging to the given professor.
     */
    @Transactional(readOnly = true)
    public List<ExamAttempt> getAttemptsForProfessor(Long professorId) {
        return examAttemptRepository.findByExamCall_Exam_ProfessorId(professorId);
    }

    /**
     * Returns submitted/graded attempts for a SPECIFIC exam, ensuring it belongs to the professor.
     */
    @Transactional(readOnly = true)
    public List<ExamAttempt> getAttemptsForExam(Long examId, Long professorId) {
        return examAttemptRepository.findByExamCall_Exam_IdAndExamCall_Exam_ProfessorId(examId, professorId);
    }

    /**
     * Returns a single attempt, enforcing that it belongs to one of the professor's exams.
     */
    @Transactional(readOnly = true)
    public ExamAttempt getAttemptForProfessor(Long attemptId, Long professorId) {
        return examAttemptRepository.findByIdAndExamCall_Exam_ProfessorId(attemptId, professorId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Attempt not found or does not belong to your exams"));
    }

    // -------------------------------------------------------------------------
    // Grading
    // -------------------------------------------------------------------------

    /**
     * Grades a single question within an attempt. Validates that the attempt belongs
     * to the professor and that the attempt is in SUBMITTED state.
     *
     * @param attemptId   the attempt to grade
     * @param questionId  the AttemptQuestion id
     * @param score       score awarded to this question (>= 0)
     * @param comment     optional review comment / feedback
     * @param professorId the authenticated professor's id (ownership check)
     * @return the updated AttemptQuestion
     */
    @Transactional
    public AttemptQuestion gradeQuestion(Long attemptId,
                                         Long questionId,
                                         BigDecimal score,
                                         String comment,
                                         Long professorId) {
        ExamAttempt attempt = examAttemptRepository.findByIdAndExamCall_Exam_ProfessorId(attemptId, professorId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Attempt not found or does not belong to your exams"));

        if (!"SUBMITTED".equals(attempt.getStatus())) {
            throw new IllegalStateException(
                    "Only SUBMITTED attempts can be graded (current status: " + attempt.getStatus() + ")");
        }

        AttemptQuestion attemptQuestion = attempt.getQuestions().stream()
                .filter(q -> q.getId().equals(questionId))
                .findFirst()
                .orElseThrow(() -> new EntityNotFoundException("Question not found in this attempt"));

        attemptQuestion.setAwardedScore(score);
        if (comment != null && !comment.isBlank()) {
            attemptQuestion.setReviewComment(comment);
        }

        examAttemptRepository.save(attempt);
        return attemptQuestion;
    }

    /**
     * Closes grading for an attempt: computes finalScore as the sum of all awardedScores,
     * sets status to GRADED. Validates ownership.
     *
     * @param attemptId   the attempt to close
     * @param professorId the authenticated professor's id (ownership check)
     * @return the updated ExamAttempt
     */
    @Transactional
    public ExamAttempt closeGrading(Long attemptId, Long professorId) {
        ExamAttempt attempt = examAttemptRepository.findByIdAndExamCall_Exam_ProfessorId(attemptId, professorId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Attempt not found or does not belong to your exams"));

        if (!"SUBMITTED".equals(attempt.getStatus())) {
            throw new IllegalStateException(
                    "Only SUBMITTED attempts can be closed for grading");
        }

        BigDecimal finalScore = attempt.getQuestions().stream()
                .map(q -> q.getAwardedScore() != null ? q.getAwardedScore() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        attempt.setFinalScore(finalScore);
        attempt.setStatus("GRADED");
        return examAttemptRepository.save(attempt);
    }

    // -------------------------------------------------------------------------
    // Student views
    // -------------------------------------------------------------------------

    /**
     * Returns all attempts (with results) for the given student. Only their own attempts.
     */
    @Transactional(readOnly = true)
    public List<ExamAttempt> getResultsForStudent(Long studentId) {
        return examAttemptRepository.findByStudentId(studentId);
    }

    /**
     * Returns a single attempt result, ensuring it belongs to the student.
     */
    @Transactional(readOnly = true)
    public ExamAttempt getResultForStudent(Long attemptId, Long studentId) {
        return examAttemptRepository.findByIdAndStudentId(attemptId, studentId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Attempt not found or does not belong to you"));
    }
}
