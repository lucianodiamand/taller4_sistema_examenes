package com.exam_system.exam.repository;

import com.exam_system.exam.domain.ExamAttempt;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ExamAttemptRepository extends JpaRepository<ExamAttempt, Long> {

    Optional<ExamAttempt> findByExamCallIdAndStudentId(Long examCallId, Long studentId);

    List<ExamAttempt> findByStudentId(Long studentId);

    List<ExamAttempt> findByStudentIdAndExamCallIdIn(Long studentId, Collection<Long> examCallIds);

    /**
     * El studentId forma parte de la consulta a propósito: así una resolución ajena
     * se comporta como inexistente y nunca llega a exponerse al estudiante.
     */
    @EntityGraph(attributePaths = {
            "examCall",
            "examCall.exam",
            "examCall.exam.professor",
            "questions",
            "questions.question"
    })
    Optional<ExamAttempt> findByIdAndStudentId(Long attemptId, Long studentId);

    @EntityGraph(attributePaths = {
            "examCall",
            "examCall.exam",
            "examCall.exam.professor",
            "questions",
            "questions.question"
    })
    List<ExamAttempt> findByStudentIdOrderByStartedAtDesc(Long studentId);
}
