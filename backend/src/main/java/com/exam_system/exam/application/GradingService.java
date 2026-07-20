package com.exam_system.exam.application;

import com.exam_system.exam.domain.AttemptQuestion;
import com.exam_system.exam.domain.AttemptStatus;
import com.exam_system.exam.domain.ExamAttempt;
import com.exam_system.exam.repository.ExamAttemptRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Centraliza la lógica de corrección de exámenes.
 * El profesor solo puede ver y corregir resoluciones de sus propios exámenes.
 * El estudiante solo puede ver sus propias resoluciones.
 */
@Service
public class GradingService {

    private final ExamAttemptRepository examAttemptRepository;

    public GradingService(ExamAttemptRepository examAttemptRepository) {
        this.examAttemptRepository = examAttemptRepository;
    }

    @Transactional(readOnly = true)
    public List<ExamAttempt> getAttemptsForProfessor(Long professorId) {
        return examAttemptRepository.findAllByProfessorId(professorId);
    }

    @Transactional(readOnly = true)
    public List<ExamAttempt> getAttemptsForExam(Long examId, Long professorId) {
        return examAttemptRepository.findAllByExamIdAndProfessorId(examId, professorId);
    }

    @Transactional(readOnly = true)
    public ExamAttempt getAttemptForProfessor(Long attemptId, Long professorId) {
        return examAttemptRepository.findByIdAndProfessorId(attemptId, professorId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "La resolución no existe o no pertenece a tus exámenes"));
    }

    /**
     * Califica una pregunta individual dentro de una resolución.
     * Valida que la resolución sea del profesor y que esté en estado SUBMITTED.
     */
    @Transactional
    public AttemptQuestion gradeQuestion(Long attemptId,
                                         Long questionId,
                                         BigDecimal score,
                                         String comment,
                                         Long professorId) {
        ExamAttempt attempt = examAttemptRepository.findByIdAndProfessorId(attemptId, professorId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "La resolución no existe o no pertenece a tus exámenes"));

        if (attempt.getStatus() != AttemptStatus.SUBMITTED) {
            throw new IllegalStateException(
                    "Solo se pueden corregir resoluciones en estado SUBMITTED (estado actual: " + attempt.getStatus() + ")");
        }

        AttemptQuestion attemptQuestion = attempt.getQuestions().stream()
                .filter(q -> q.getId().equals(questionId))
                .findFirst()
                .orElseThrow(() -> new EntityNotFoundException("La pregunta no existe en esta resolución"));

        attemptQuestion.setAwardedScore(score);
        if (comment != null && !comment.isBlank()) {
            attemptQuestion.setReviewComment(comment);
        }

        examAttemptRepository.save(attempt);
        return attemptQuestion;
    }

    /**
     * Cierra la corrección de una resolución: calcula el puntaje final como la suma
     * de los puntajes asignados a cada pregunta y pasa el estado a GRADED.
     */
    @Transactional
    public ExamAttempt closeGrading(Long attemptId, Long professorId) {
        ExamAttempt attempt = examAttemptRepository.findByIdAndProfessorId(attemptId, professorId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "La resolución no existe o no pertenece a tus exámenes"));

        if (attempt.getStatus() != AttemptStatus.SUBMITTED) {
            throw new IllegalStateException(
                    "Solo se pueden cerrar resoluciones en estado SUBMITTED");
        }

        BigDecimal finalScore = attempt.getQuestions().stream()
                .map(q -> q.getAwardedScore() != null ? q.getAwardedScore() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, (sum, score) -> sum.add(score));

        attempt.setFinalScore(finalScore);
        attempt.setStatus(AttemptStatus.GRADED);
        return examAttemptRepository.save(attempt);
    }

    @Transactional(readOnly = true)
    public List<ExamAttempt> getResultsForStudent(Long studentId) {
        return examAttemptRepository.findByStudentIdOrderByStartedAtDesc(studentId);
    }

    @Transactional(readOnly = true)
    public ExamAttempt getResultForStudent(Long attemptId, Long studentId) {
        return examAttemptRepository.findByIdAndStudentId(attemptId, studentId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "La resolución no existe o no te pertenece"));
    }
}
